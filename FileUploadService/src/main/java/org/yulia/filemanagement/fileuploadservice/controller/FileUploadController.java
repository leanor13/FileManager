package org.yulia.filemanagement.fileuploadservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.service.FileUploadService;

import java.util.List;
import java.util.Map;

import static org.yulia.filemanagement.fileuploadservice.constants.UserErrorMessages.FILE_UPLOAD_FAILED;

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
     * Handles the file upload request.
     * Note: Currently, only one file at a time is supported.
     *
     * @param files the list of files to be uploaded; only one file allowed
     * @return ResponseEntity with the upload result or error message
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestPart("file") List<MultipartFile> files) {

        logger.info("Received file upload request");

        // Validate file upload
        ResponseEntity<?> validationResult = validateFileUpload(files);
        if (validationResult != null) {
            return validationResult;
        }

        MultipartFile file = files.getFirst();

        if (file.isEmpty()) {
            logger.warn("Uploaded file is empty."); // Log warning
            return ResponseEntity.badRequest().body("The file is empty. Please select a non-empty file to upload.");
        }

        var result = fileUploadService.uploadFile(file);

        if (result == null || result.userMessage() == null || result.status() == null) {
            logger.error("File upload failed due to internal server error."); // Log error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed due to server error.");
        }

        if (result.success()) {
            logger.info("File uploaded successfully: {}", file.getOriginalFilename());
            return ResponseEntity.ok(result.userMessage());
        } else {
            logger.warn("File upload failed: {} - {}", result.status(), result.userMessage());
            return ResponseEntity.status(result.status()).body(result.userMessage());
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

        if (files.size() != 1) {
            logger.warn("Multiple files detected. Only one file upload is allowed.");
            return ResponseEntity.badRequest().body("Please upload only one file at a time.");
        }

        return null; // Return null to indicate that validation passed
    }


}
