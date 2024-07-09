package org.yulia.filemanagement.filemetadataservice.service;

import io.minio.MinioClient;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service class for extracting metadata from files stored in a MinIO bucket.
 */
@Service
public class FileMetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataExtractor.class);

    private final MinioClient minioClient;

    @Autowired
    public FileMetadataExtractor(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Extracts the file name from a URL by stripping any query parameters.
     *
     * @param fileUrl the URL from which to extract the file name
     * @return the extracted file name
     */
    public String extractName(String fileUrl) {
        // Remove query parameters from the URL
        var cleanUrl = fileUrl.split("\\?")[0];
        // Extract the file name from the URL
        return cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
    }

    /**
     * Extracts metadata for a file stored in a MinIO bucket.
     *
     * @param bucketName the name of the MinIO bucket
     * @param fileUrl    the URL of the file to extract metadata from
     * @return FileMetadata containing the extracted data
     * Note: The returned URL is cleaned of all query parameters. If you need the full URL with query parameters,
     * consider using the MinIO URL generator method.
     */
    public FileMetadata extractMetadata(String bucketName, String fileUrl) {
        validateInputs(fileUrl, bucketName);

        try {
            // clean the URL to remove any query parameters
            String cleanUrl = fileUrl.split("\\?")[0];
            String fileName = extractName(fileUrl);

            var stat = minioClient.statObject(
                    io.minio.StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            long fileSize = stat.size();
            String fileType = determineFileType(stat.contentType());

            LocalDateTime uploadDate = determineUploadDate(stat.lastModified());

            FileMetadata metadata = createFileMetadata(cleanUrl, fileName, fileType, fileSize, uploadDate);
            logger.info("Extracted metadata - File URL: {}, File Name: {}, File Type: {}, File Size: {}, Upload Date:" +
                            " {}",
                    fileUrl, fileName, fileType, fileSize, uploadDate);

            return metadata;
        } catch (Exception e) {
            throw handleMinioException(e);
        }
    }

    private void validateInputs(String fileUrl, String bucketName) {
        if (fileUrl == null || bucketName == null) {
            throw new IllegalArgumentException("File URL and bucket name cannot be null");
        }
    }

    // if the file type is not available, set it to "unknown"
    private String determineFileType(String fileType) {
        return fileType != null ? fileType : "unknown";
    }

    // if there is no Upload Date in MinIO, set it to the current date and time
    private LocalDateTime determineUploadDate(ZonedDateTime lastModifiedDate) {
        if (lastModifiedDate != null) {
            return lastModifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else {
            return LocalDateTime.now();
        }
    }

    // create a new FileMetadata object with the extracted metadata
    private FileMetadata createFileMetadata(String fileUrl, String fileName, String fileType, long fileSize,
                                            LocalDateTime uploadDate) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileUrl(fileUrl);
        metadata.setFileName(fileName);
        metadata.setFileType(fileType);
        metadata.setFileSize(fileSize);
        metadata.setUploadDate(uploadDate);
        return metadata;
    }

    // handle MinIO exceptions and convert them to runtime exceptions
    private RuntimeException handleMinioException(Exception e) {
        if (e instanceof InvalidResponseException) {
            logger.error("Security or response format error: {}", e.getMessage());
            return new RuntimeException("Security or response format error: " + e.getMessage(), e);
        } else if (e instanceof InternalException || e instanceof XmlParserException) {
            logger.error("Data access error: {}", e.getMessage());
            return new RuntimeException("Data access error: " + e.getMessage(), e);
        } else if (e instanceof ErrorResponseException || e instanceof ServerException) {
            logger.error("Server-side error: {}", e.getMessage());
            return new RuntimeException("Server-side error: " + e.getMessage(), e);
        } else {
            logger.error("Unhandled Minio exception: {}", e.getMessage());
            return new RuntimeException("Unhandled Minio exception: " + e.getMessage(), e);
        }
    }
}