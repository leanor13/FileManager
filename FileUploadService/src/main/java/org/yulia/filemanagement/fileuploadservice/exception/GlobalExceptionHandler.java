package org.yulia.filemanagement.fileuploadservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for the application.
 * We don't expose any internal details in the error messages at this point.
 * We can add more specific logging as needed.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestPartException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<String> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Required part 'file' is not present. Check the " +
                "request and try again.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No static resource found.");
    }

    @ExceptionHandler(RestClientException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<String> handleRestClientException(RestClientException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Failed to communicate with the metadata service\"}");
    }

    @ExceptionHandler(MultipartException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<String> handleMultipartException(MultipartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid request. Ensure you have specified file to upload'.");
    }
}
