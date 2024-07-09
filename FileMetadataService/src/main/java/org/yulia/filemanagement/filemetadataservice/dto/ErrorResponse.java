package org.yulia.filemanagement.filemetadataservice.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an error response for API interactions.
 * This record encapsulates a descriptive message along with a list of detailed errors.
 */
public record ErrorResponse(String message, List<String> errors) {

    /**
     * Constructs an ErrorResponse without detailed errors.
     * This constructor can be used when only a general error message is necessary without specifics.
     *
     * @param message The general description of the error.
     */
    public ErrorResponse(String message) {
        this(message, null);
    }

    /**
     * Constructs an ErrorResponse with a single detailed error.
     * This utility method is useful for simple errors with one specific cause.
     *
     * @param message The general description of the error.
     * @param detail A specific detail about the error, providing additional context.
     * @return An instance of ErrorResponse with one detailed error.
     */
    public static ErrorResponse withSingleError(String message, String detail) {
        return new ErrorResponse(message, new ArrayList<>(List.of(detail)));
    }
}
