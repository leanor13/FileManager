package org.yulia.filemanagement.filemetadataservice.exception;

import jakarta.validation.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.yulia.filemanagement.filemetadataservice.dto.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Global exception handler for handling various exceptions in the application.
 * Provides centralized exception handling across all @RequestMapping methods.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors for method arguments annotated with @Valid.
     *
     * @param ex the MethodArgumentNotValidException
     * @param headers the HTTP headers
     * @param statusCode the HTTP status code
     * @param request the web request
     * @return a ResponseEntity containing the validation error details
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  @NotNull HttpHeaders headers,
                                                                  @NotNull HttpStatusCode statusCode,
                                                                  @NotNull WebRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        logger.error("Validation error", ex);

        return new ResponseEntity<>(new ErrorResponse("Validation error", errors), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles database access exceptions.
     *
     * @param ex the DataAccessException
     * @return a ResponseEntity containing the database error details
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseExceptions(DataAccessException ex) {

        logger.error("Database error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Database error: " + ex.getMessage()));
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex the IllegalArgumentException
     * @param request the web request
     * @return a ResponseEntity containing the invalid argument error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
                                                                        WebRequest request) {
        logger.error("Handled IllegalArgumentException: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withSingleError("Invalid argument", ex.getMessage()));
    }

    /**
     * Handles runtime exceptions.
     *
     * @param ex the RuntimeException
     * @param request the web request
     * @return a ResponseEntity containing the runtime error details
     */
    @ExceptionHandler(RuntimeException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {

        logger.error("Handled RuntimeException: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error: " + ex.getMessage()));
    }

    /**
     * Handles constraint violation exceptions.
     *
     * @param ex the ConstraintViolationException
     * @param request the web request
     * @return a ResponseEntity containing the constraint violation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex,
                                                                            WebRequest request) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        logger.error("ConstraintViolationException: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation error", errors));
    }

    /**
     * Handles all other exceptions not specifically handled by other methods.
     *
     * @param ex the Exception
     * @param request the web request
     * @return a ResponseEntity containing the error details
     */
    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {

        logger.error("Unexpected error", ex);

        return new ResponseEntity<>(new ErrorResponse("Internal server error: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
