package org.yulia.filemanagement.fileuploadservice.controller;

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

    /**
     * Processes the upload of multiple files sent in a multipart request. Each file is processed
     * individually and the method returns a detailed result for each file upload, including status
     * and messages. This allows clients to receive comprehensive feedback on the outcome of each file's processing.
     *
     * @param files the list of files to be uploaded, provided as parts of a multipart request ('multipart/form-data').
     * @return ResponseEntity containing a list of results for each file. Depending on the outcomes of the uploads,
     *         the method may return one of the following HTTP statuses:
     *         - 200 OK if all files were successfully processed.
     *         - 207 MULTI_STATUS if some files were successfully uploaded and others were not.
     *         - 400 BAD_REQUEST if all files failed to upload due to client-side errors (e.g., files are empty).
     *         - 500 INTERNAL_SERVER_ERROR if there was an internal server error while processing the files.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("file") List<MultipartFile> files) {
        logger.info("Received file upload request with {} file(s).", files.size());

        // Validate file upload
        ResponseEntity<?> validationResult = validateFileUpload(files);
        if (validationResult != null) {
            return validationResult;
        }

        List<Map<String, Object>> fileResults = new ArrayList<>();
        boolean anySuccess = false;
        boolean anyFailures = false;

        for (MultipartFile file : files) {
            Map<String, Object> fileResult = processFileUpload(file);
            fileResults.add(fileResult);
            if ((Integer) fileResult.get("status") == HttpStatus.OK.value()) {
                anySuccess = true;
            } else {
                anyFailures = true;
            }
        }

        if (anySuccess && anyFailures) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(fileResults);
        } else if (anySuccess) {
            return ResponseEntity.ok(fileResults);
        } else {
            return ResponseEntity.badRequest().body(fileResults);
        }
    }

    /**
     * Handles HTTP GET requests to retrieve a list of files with specified filters.
     * This method attempts to fetch files from the metadata service with retries in case of failures.
     * Filters can include parameters like file type, size, and other criteria defined in the metadata service.
     *
     * @param filters a map of string parameters used for filtering the list of files. Each entry in the map
     *                represents a filter field and value, such as "type" = "pdf" or "size" = "1000".
     * @return a ResponseEntity containing the JSON-formatted list of files if successful, or an error message
     *         if the attempt to retrieve files fails. The status code in the ResponseEntity reflects the outcome
     *         of the request: HTTP 200 OK on success, or an error code (such as HTTP 500 Internal Server Error) if there are issues.
     *         If the thread is interrupted during retries, the method returns HTTP 500 Internal Server Error.
     */
    @GetMapping("")
    public ResponseEntity<String> getFiles(@RequestParam Map<String, String> filters) {
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
