package org.yulia.filemanagement.fileuploadservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.service.FileUploadService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.base.path}")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    // TODO: take out logic for statuses and messages to a separate method. Implement it through list of objects
    @PostMapping("/upload")
    @Operation(summary = "Upload multiple files",
            description = "FOR TESTING THIS API USE POSTMAN OR CURL" +
                    "Processes the upload of multiple files sent in a multipart request. " +
                    "Each file is processed individually and the method returns a detailed result for each file upload, " +
                    "including status and messages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All files were successfully processed"),
            @ApiResponse(responseCode = "207", description = "Some files were successfully uploaded and others were not"),
            @ApiResponse(responseCode = "400", description = "All files failed to upload due to client-side errors such as empty files"),
            @ApiResponse(responseCode = "500", description = "Internal server error while processing the files")
    })
    public ResponseEntity<?> uploadFiles(
            @RequestPart("file")
            @Parameter(description = "Files to be uploaded", required = true,
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "array", implementation = MultipartFile.class)))
            List<MultipartFile> files) {
        logger.info("Received file upload request with {} file(s).", files.size());

        // Validate file upload
        ResponseEntity<?> validationResult = validateFileUpload(files);
        if (validationResult != null) {
            return validationResult;
        }

        List<Map<String, Object>> fileResults = new ArrayList<>();
        boolean anySuccess = false;
        boolean anyFailures = false;
        boolean allServerErrors = true;

        for (MultipartFile file : files) {
            Map<String, Object> fileResult = processFileUpload(file);
            fileResults.add(fileResult);
            int status = (Integer) fileResult.get("status");

            if (HttpStatus.Series.SUCCESSFUL.equals(HttpStatus.valueOf(status).series())) {
                anySuccess = true;
                allServerErrors = false;
            } else if (HttpStatus.Series.CLIENT_ERROR.equals(HttpStatus.valueOf(status).series())) {
                anyFailures = true;
                allServerErrors = false;
            } else if (HttpStatus.Series.SERVER_ERROR.equals(HttpStatus.valueOf(status).series())) {
                anyFailures = true;
            } else {
                anyFailures = true;
                allServerErrors = false;
            }
        }

        if (allServerErrors) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fileResults);  // Все файлы вызвали ошибку 5xx
        } else if (anySuccess && anyFailures) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(fileResults);  // Некоторые файлы загружены успешно, другие нет
        } else if (anySuccess) {
            return ResponseEntity.ok(fileResults);
        } else {
            return ResponseEntity.badRequest().body(fileResults);
        }
    }

    @Operation(summary = "Retrieve files with filters",
            description = "Handles HTTP GET requests to retrieve a list of files with specified filters. This method attempts to fetch files from the metadata service with retries in case of failures.",
            parameters = {
                    @Parameter(name = "file_type", description = "Optional file type to filter by, e.g., application/pdf, text/plain, etc.",
                            in = ParameterIn.QUERY),
                    @Parameter(name = "min_size", description = "Optional minimum file size to filter by",
                            in = ParameterIn.QUERY),
                    @Parameter(name = "max_size", description = "Optional maximum file size to filter by",
                            in = ParameterIn.QUERY),
                    @Parameter(name = "equal_size", description = "Optional exact file size to filter by",
                            in = ParameterIn.QUERY),
                    @Parameter(name = "size_unit", description = "Optional unit of the size parameters, with a default if unspecified",
                            schema = @Schema(allowableValues = {"bytes", "kb", "mb", "gb"}, defaultValue = "bytes"),
                            in = ParameterIn.QUERY),
                    @Parameter(name = "custom_filter", description = "Additional custom filter - not yet supported by" +
                            " Metadata Service", in = ParameterIn.QUERY)
            })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of files based on the filters provided"),
            @ApiResponse(responseCode = "4xx", description = "Client error. The request contains bad syntax or cannot be fulfilled."),
            @ApiResponse(responseCode = "5xx", description = "Server error. The server failed to fulfill an apparently valid request.")
    })
    @GetMapping("")
    public ResponseEntity<String> getFiles(
            @Parameter(hidden = true)
            @RequestParam Map<String, String> filters)
    {
        logger.info("Received request to get files with filters: {}", filters);
        try {
            return fileUploadService.getFiles(filters);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread was interrupted during file retrieval retries.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve files due to interruption.");
        }
    }

    private ResponseEntity<?> validateFileUpload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            logger.warn("File upload request does not contain 'file' parameter or file was not attached.");
            return ResponseEntity.badRequest().body("File parameter 'file' is missing or no file was attached. Please attach a file with the parameter 'file'.");
        }

        // TODO: move constant to parameters
        if (files.size() > 10) {
            logger.warn("Too many files detected for upload. Only up to 10 file upload is allowed.");
            return ResponseEntity.badRequest().body("Please limit file upload quantity.");
        }

        return null; // Return null to indicate that validation passed
    }

    // processFileUpload method is used to process a single file upload and add the result to the list of file results
    private Map<String, Object> processFileUpload(MultipartFile file) {
        Map<String, Object> fileResult = new HashMap<>();
        fileResult.put("fileName", file.getOriginalFilename());

        if (file.isEmpty()) {
            fileResult.put("status", HttpStatus.BAD_REQUEST.value());
            fileResult.put("message", "The file is empty. Please select a non-empty file to upload.");
        } else {
            var result = fileUploadService.uploadFile(file);
            if (result == null || result.userMessage() == null || result.status() == null) {
                fileResult.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                fileResult.put("message", "File upload failed due to server error.");
            } else if (result.success()) {
                fileResult.put("status", HttpStatus.OK.value());
                fileResult.put("message", result.userMessage());
            } else {
                fileResult.put("status", result.status().value());
                fileResult.put("message", result.userMessage());
            }
        }
        return fileResult;
    }



}
