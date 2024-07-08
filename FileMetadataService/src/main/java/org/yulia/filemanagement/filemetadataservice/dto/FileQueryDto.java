package org.yulia.filemanagement.filemetadataservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;

public record FileQueryDto(
        String fileType,
        @Min(value = 0, message = "The minimum size must not be negative") Long minSize,
        @Min(value = 0, message = "The maximum size must not be negative") Long maxSize,
        @Min(value = 0, message = "The exact size must not be negative") Long equalSize,
        @NotNull(message = "Size unit must be specified") SizeUnit sizeUnit
) {
}

