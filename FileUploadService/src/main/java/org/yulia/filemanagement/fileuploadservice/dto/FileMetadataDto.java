package org.yulia.filemanagement.fileuploadservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record FileMetadataDto(
        @NotBlank(message = "Filename cannot be empty") String fileName,
        @Min(value = 0, message = "Size cannot be negative") long size,
        String url,
        String type
) implements Serializable {

}

