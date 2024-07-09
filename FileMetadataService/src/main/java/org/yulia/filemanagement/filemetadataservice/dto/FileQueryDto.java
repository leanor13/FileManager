package org.yulia.filemanagement.filemetadataservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;

/**
 * Data Transfer Object used for querying file metadata based on various criteria.
 * It supports filtering based on file type, size constraints, and size units.
 */
public record FileQueryDto(
    // The type of the file to filter by, such as 'text/plain', etc.
    String fileType,

    // The minimum size of the files to be included in the result, measured in SizeUnit.
    @Min(value = 0, message = "The minimum size must not be negative") Long minSize,

    // The maximum size of the files to be included in the result, measured in SizeUnit.
    @Min(value = 0, message = "The maximum size must not be negative") Long maxSize,

    // The exact size that the files must match to be included in the result, measured in the SizeUnit.
    @Min(value = 0, message = "The exact size must not be negative") Long equalSize,

    //The unit of measurement used for the size filters: bytes, kilobytes, megabytes, or gigabytes.
    @NotNull(message = "Size unit must be specified") SizeUnit sizeUnit
) {
}
