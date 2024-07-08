package org.yulia.filemanagement.filemetadataservice.dto;

import java.util.List;

public record ErrorResponse(String message, List<String> errors) {
    public ErrorResponse(String message) {
        this(message, null);
    }
}
