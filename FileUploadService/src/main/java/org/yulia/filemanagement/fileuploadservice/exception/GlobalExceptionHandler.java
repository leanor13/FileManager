package org.yulia.filemanagement.fileuploadservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

//TODO: check if needed
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Required part 'file' is not present. Check the " +
                "request and try again.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No static resource found.");
    }

//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e) {
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. You do not have permission to access this resource.");
//    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleRestClientException(RestClientException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Failed to communicate with the metadata service\"}");
    }
}
