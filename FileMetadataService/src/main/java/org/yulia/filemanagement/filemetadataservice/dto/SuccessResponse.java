package org.yulia.filemanagement.filemetadataservice.dto;

/**
 * Data Transfer Object for sending successful responses.
 * Contains a message indicating the success and optional data related to the success.
 */
public record SuccessResponse(String message, Object data) {
}
