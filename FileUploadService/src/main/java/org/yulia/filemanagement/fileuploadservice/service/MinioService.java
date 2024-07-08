package org.yulia.filemanagement.fileuploadservice.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

import static org.yulia.filemanagement.fileuploadservice.constants.InternalErrorMessages.BUCKET_NAME_EMPTY;

@Service
public class MinioService {

    private static final Logger logger = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(MinioClient minioClient, @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    /**
     * Uploads an object to Minio and returns the presigned URL for accessing the object.
     *
     * @param filename    the name of the file to be uploaded
     * @param data        the input stream of the file data
     * @param size        the size of the file
     * @param contentType the content type of the file
     * @return the presigned URL for accessing the uploaded object
     * @throws IOException if an error occurs during upload or URL generation
     */
    public String uploadObject(String filename, InputStream data, long size, String contentType) throws IOException {
        validateBucketName();

        logger.info("Uploading object '{}' to bucket '{}'", filename, bucketName);

        var args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .stream(data, size, -1)
                .contentType(contentType)
                .build();
        try {
            minioClient.putObject(args);
            logger.info("Successfully uploaded object '{}'", filename);

            var urlArgs = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(filename)
                    .build();
            var url = minioClient.getPresignedObjectUrl(urlArgs);
            logger.info("Generated presigned URL for object '{}': {}", filename, url);
            return url;
        } catch (Exception e) {
            logger.error("Error uploading object '{}': {}", filename, e.getMessage());
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Minio error: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes an object from Minio.
     *
     * @param filename the name of the file to be deleted
     * @throws IOException if an error occurs during deletion
     */
    public void deleteObject(String filename) throws IOException {
        validateBucketName();

        try {
            logger.info("Deleting object '{}' from bucket '{}'", filename, bucketName);
            var deleteArgs = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .build();
            minioClient.removeObject(deleteArgs);
            logger.info("Successfully deleted object '{}'", filename);
        } catch (Exception e) {
            logger.error("Error deleting object '{}': {}", filename, e.getMessage());
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Failed to delete object from Minio", e);
        }
    }

    /**
     * Validates the bucket name.
     *
     * @throws IllegalArgumentException if the bucket name is null or empty
     */
    private void validateBucketName() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException(BUCKET_NAME_EMPTY);
        }
    }
}

