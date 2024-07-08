package org.yulia.filemanagement.fileuploadservice.constants;

public class InternalErrorMessages {
    public static final String BUCKET_NAME_EMPTY = "Bucket name must not be empty.";
    public static final String STORAGE_ERROR = "Storage error occurred during file upload.";
    public static final String FILE_IS_NULL = "File is null.";
    public static final String DATABASE_CONNECTION_FAILURE = "Failed to connect to the database.";
    public static final String COMMUNICATION_MESSAGE_FAILURE = "Communication message could not be sent. Deleting " +
            "file.";
}
