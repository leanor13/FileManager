package org.yulia.filemanagement.fileuploadservice.dto;

import org.springframework.http.HttpStatus;

import java.util.Optional;

/**
 * Data Transfer Object (DTO) for representing the result of a file upload operation.
 */
public record UploadResult(
        // Indicates whether the upload was successful
        boolean success,

        // Message to be displayed to the user */
        String userMessage,

        // Internal message for logging or debugging */
        String internalMessage,

        // HTTP status code of the result */
        HttpStatus status,

        // Optional URL of the uploaded file, if available */
        Optional<String> fileUrl) {
}

