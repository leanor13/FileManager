package org.yulia.filemanagement.fileuploadservice.constants;

public class UserErrorMessages {
    public static final String FILE_NEGATIVE_SIZE = "File size cannot be negative.";
    //TODO: we can add specific max limit value to this message
    public static final String FILE_SIZE_EXCEEDED = "File size exceeds the maximum limit.";
    public static final String FILE_UPLOAD_FAILED = "Failed to upload file. Please try again later.";
    public static final String NETWORK_ISSUE = "Temporary network issue";
    public static final String STORAGE_ERROR = "Failed to upload file: Storage error.";
    public static final String FILE_NOT_FOUND = "File not found.";
}
