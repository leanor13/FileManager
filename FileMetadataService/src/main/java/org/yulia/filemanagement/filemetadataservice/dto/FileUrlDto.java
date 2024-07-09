package org.yulia.filemanagement.filemetadataservice.dto;

/**
 * Data Transfer Object for holding the URL of a file stored in MinIO.
 * Constructs a new FileUrlDto.
 * @param fileUrl the URL of the file in MinIO. This should be a valid URL string.
 */
public record FileUrlDto(String fileUrl) {
}
