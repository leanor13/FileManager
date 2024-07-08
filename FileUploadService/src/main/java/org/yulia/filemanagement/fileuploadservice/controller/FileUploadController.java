package org.yulia.filemanagement.fileuploadservice.controller;

import okio.FileMetadata;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.dto.ErrorResponse;
import org.yulia.filemanagement.fileuploadservice.dto.UploadResult;
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
     *
     * @param file the file to be uploaded
     * @return ResponseEntity with the upload result
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        logger.info("Received file upload request: {}", file.getOriginalFilename());

        var result = fileUploadService.uploadFile(file);

        if (result == null || result.userMessage() == null || result.status() == null) {
            logger.error("File upload failed due to internal server error."); // Log error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(FILE_UPLOAD_FAILED);
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
            return fileUploadService.getFilesWithRetry(filters);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread was interrupted during file retrieval retries.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve files due to interruption.");
        }
    }

}
