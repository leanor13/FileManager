package org.yulia.filemanagement.fileuploadservice.exception;

import java.io.IOException;

public class MinioServiceUnavailableException extends IOException {
    public MinioServiceUnavailableException(String message) {
        super(message);
    }
}
