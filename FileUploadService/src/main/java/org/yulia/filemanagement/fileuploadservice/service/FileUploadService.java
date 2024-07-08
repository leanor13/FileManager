package org.yulia.filemanagement.fileuploadservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.communication.CommunicationService;
import org.yulia.filemanagement.fileuploadservice.dto.UploadResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import static org.yulia.filemanagement.fileuploadservice.constants.InternalErrorMessages.*;
import static org.yulia.filemanagement.fileuploadservice.constants.SuccessMessages.FILE_UPLOAD_SUCCESS;
import static org.yulia.filemanagement.fileuploadservice.constants.UserErrorMessages.*;


// TODO: take out retry method
@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final MinioService minioService;
    private final long maxFileSize;
    private final CommunicationService communicationService;
    private final int maxRetries;
    private final long sleepBetweenRetries;

    public FileUploadService(MinioService minioService,
                             @Value("${file.max.size.bytes}") long maxFileSize,
                             CommunicationService communicationService,
                             @Value("${send.message.retry}") int maxRetries,
                             @Value("${send.sleep.between.retry.ms}") long sleepBetweenRetries) {
        this.minioService = minioService;
        this.maxFileSize = maxFileSize;
        this.communicationService = communicationService;
        this.maxRetries = maxRetries;
        this.sleepBetweenRetries = sleepBetweenRetries;
    }

    /**
     * Uploads a file and returns the result of the upload operation.
     *
     * @param file the file to be uploaded
     * @return the result of the upload operation
     */
    public UploadResult uploadFile(MultipartFile file) {
        logger.info("Starting file upload.");

        // Validate the file
        Optional<UploadResult> validationResult = validateFile(file);
        if (validationResult.isPresent()) {
            logger.warn("File validation failed: {}", validationResult.get().internalMessage());
            return validationResult.get();
        }

        try (InputStream inputStream = file.getInputStream()) {
            var fileUrl = minioService.uploadObject(file.getOriginalFilename(), inputStream,
                    file.getSize(), file.getContentType());
            logger.info("File uploaded to Minio: {}", fileUrl);

            // Checking if attempt to send file URL to MetadataService was successful
            if (!sendUrlWithRetry(fileUrl).getStatusCode().is2xxSuccessful()) {
                var fileName = file.getOriginalFilename();

                logger.error("Failed to send file {} URL after {} retries, deleting file from Minio: {}",
                        fileName, maxRetries, fileUrl);
                minioService.deleteObject(fileName);
                communicationService.sendDeleteMessage(fileName);
                logger.info("Sent request to delete from MetadataService file name: {}", fileName);
                return new UploadResult(false, FILE_UPLOAD_FAILED, COMMUNICATION_MESSAGE_FAILURE,
                        HttpStatus.EXPECTATION_FAILED, Optional.empty());
            }

            logger.info("File URL successfully sent: {}", fileUrl);
            return new UploadResult(true, FILE_UPLOAD_SUCCESS, FILE_UPLOAD_SUCCESS,
                    HttpStatus.OK, Optional.ofNullable(fileUrl));
        } catch (Exception e) {
            logger.error("File upload failed: {}", e.getMessage());
            return handleException(e);
        }
    }

    /**
     * Attempts to retrieve files from the metadata service with retries. This method retries fetching files for a specified
     * number of attempts with pauses between them if necessary. It differentiates between client-side and server-side errors
     * to determine whether retries are appropriate.
     *
     * @param filters the parameters to filter the files by, such as file type, size, or other criteria.
     * @return ResponseEntity containing the JSON response with file data if successful, or an error message if not. For server errors,
     *         retries are performed until successful or all retries are exhausted. For client errors, it returns immediately with the error.
     * @throws InterruptedException if the thread is interrupted during sleep between retries, maintaining proper handling of thread interruption.
     */
    public ResponseEntity<String> getFilesWithRetry(Map<String, String> filters) throws InterruptedException {
        int retries = maxRetries;

        while (retries > 0) {
            ResponseEntity<String> response = communicationService.getFiles(filters);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully retrieved files on attempt: {}", maxRetries - retries + 1);
                return ResponseEntity.ok(response.getBody());
            } else {
                if (response.getStatusCode().is4xxClientError()) {
                    logger.error("Client error received: {}, stopping retries.", response.getStatusCode());
                    return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
                }

                if (retries > 1) {
                    logger.warn("Server error received: {}. Retrying... Attempts left: {}", response.getStatusCode(), retries - 1);
                    Thread.sleep(sleepBetweenRetries);
                } else {
                    logger.error("Failed to retrieve files after all retries. Last received status: {}", response.getStatusCode());
                }
            }
            retries--;
        }
        // If all retries are exhausted without success, return a service unavailable error
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":\"Service unavailable after multiple retries.\"}");
    }

    /**
     * Validates the file to be uploaded.
     *
     * @param file the file to be validated
     * @return an optional UploadResult if validation fails, otherwise an empty optional
     */
    private Optional<UploadResult> validateFile(MultipartFile file) {
        if (file == null) {
            logger.warn("File is null");
            return Optional.of(new UploadResult(false, FILE_UPLOAD_FAILED, FILE_IS_NULL,
                    HttpStatus.BAD_REQUEST, Optional.empty()));
        }

        var fileSize = file.getSize();

        if (fileSize < 0) {
            logger.warn("File size is negative: {}", file.getSize());
            return Optional.of(new UploadResult(false, FILE_NEGATIVE_SIZE, FILE_NEGATIVE_SIZE,
                    HttpStatus.BAD_REQUEST, Optional.empty()));
        }
        if (fileSize > maxFileSize) {
            logger.warn("File size exceeds maximum allowed size: {} > {}", file.getSize(), maxFileSize);
            return Optional.of(new UploadResult(false, FILE_SIZE_EXCEEDED, FILE_SIZE_EXCEEDED,
                    HttpStatus.PAYLOAD_TOO_LARGE, Optional.empty()));
        }
        return Optional.empty();
    }

    /**
     * Handles exceptions that occur during file upload.
     *
     * @param e the exception that occurred
     * @return an UploadResult representing the error
     */
    private UploadResult handleException(Exception e) {
        if (e instanceof IOException || e instanceof IllegalArgumentException) {
            logger.error("IOException occurred: {}", e.getMessage());
            return new UploadResult(false, FILE_UPLOAD_FAILED, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
                    Optional.empty());
        }
        if (e instanceof RuntimeException && e.getMessage().contains("Temporary network issue")) {
            logger.warn("Temporary network issue: {}", e.getMessage());
            return new UploadResult(false, NETWORK_ISSUE, e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE,
                    Optional.empty());
        }
        var errorMessage = "Upload failed: " + e.getMessage();
        logger.error("Unhandled exception: {}", errorMessage);
        var status = (e instanceof RuntimeException) ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        return new UploadResult(false, FILE_UPLOAD_FAILED, errorMessage, status, Optional.empty());
    }


    private ResponseEntity<String> sendUrlWithRetry(String fileUrl) throws InterruptedException {
        int retries = maxRetries;

        while (retries > 0) {
            ResponseEntity<String> response = communicationService.sendFileUrl(fileUrl);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent file URL on attempt: {}", maxRetries - retries + 1);
                return ResponseEntity.ok(response.getBody());
            } else {
                if (response.getStatusCode().is4xxClientError()) {
                    logger.error("Client error received: {}, stopping retries.", response.getStatusCode());
                    return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
                }

                if (retries > 1) {
                    logger.warn("Server error received: {}. Retrying... Attempts left: {}", response.getStatusCode(), retries - 1);
                    Thread.sleep(sleepBetweenRetries);
                } else {
                    logger.error("Failed to send file URL after all retries. Last received status: {}", response.getStatusCode());
                }
            }
            retries--;
        }
        // If all retries are exhausted without success, return a service unavailable error
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":\"Service unavailable after multiple retries.\"}");
    }

}


