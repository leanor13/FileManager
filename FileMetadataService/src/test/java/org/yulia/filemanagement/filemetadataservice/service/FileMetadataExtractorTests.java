package org.yulia.filemanagement.filemetadataservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.minio.errors.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import io.minio.messages.ErrorResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import java.time.LocalDateTime;

public class FileMetadataExtractorTests {

    @Mock
    private MinioClient minioClient;
    @Mock
    private StatObjectResponse statObjectResponse;

    private FileMetadataExtractor extractor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        extractor = new FileMetadataExtractor(minioClient);
    }

    @Test
    public void testExtractMetadata_ValidLink_ReturnsCorrectMetadata() throws Exception {
        String bucketName = "test-bucket";
        String fileUrl = "http://minio.example.com/test-bucket/testfile.txt?query=string";
        String fileName = "testfile.txt";
        long fileSize = 1024;
        String fileType = "text/plain";

        StatObjectResponse statObjectResponse = mock(StatObjectResponse.class);
        when(statObjectResponse.size()).thenReturn(fileSize);
        when(statObjectResponse.contentType()).thenReturn(fileType);

        when(minioClient.statObject(any())).thenReturn(statObjectResponse);

        FileMetadata metadata = extractor.extractMetadata(bucketName, fileUrl);

        assertNotNull(metadata);
        assertEquals("http://minio.example.com/test-bucket/testfile.txt", metadata.getFileUrl()); // Ensure query parameters are not included
        assertEquals(fileName, metadata.getFileName());
        assertEquals(fileType, metadata.getFileType());
        assertEquals(fileSize, metadata.getFileSize());
        assertEquals(LocalDateTime.now().withNano(0), metadata.getUploadDate().withNano(0)); // Remove nanoseconds for comparison

        verify(minioClient).statObject(any());
    }

    @Test
    public void testExtractMetadata_NullLink_ThrowsException() {
        String bucketName = "test-bucket";
        String fileUrl = null;

        Executable action = () -> extractor.extractMetadata(bucketName, fileUrl);

        assertThrows(IllegalArgumentException.class, action, "Should throw IllegalArgumentException when file URL is null");
    }

    @Test
    public void testExtractMetadata_NullBucket_ThrowsException() {
        String bucketName = null;
        String fileUrl = "link";

        Executable action = () -> extractor.extractMetadata(bucketName, fileUrl);

        assertThrows(IllegalArgumentException.class, action, "Should throw IllegalArgumentException when file URL is null");
    }

    @Test
    public void testExtractMetadata_ServerError_ThrowsRuntimeException() throws Exception {
        String bucketName = "test-bucket";
        String fileUrl = "http://minio.example.com/test-bucket/testfile.txt";

        ErrorResponse errorResponse = new ErrorResponse();
        Response okHttpResponse = new Response.Builder()
                .request(new Request.Builder().url(fileUrl).build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Server error")
                .body(ResponseBody.create("", MediaType.get("application/xml")))
                .build();

        ErrorResponseException exception = new ErrorResponseException(errorResponse, okHttpResponse, "Server error");

        when(minioClient.statObject(any())).thenThrow(exception);

        Executable action = () -> extractor.extractMetadata(bucketName, fileUrl);

        RuntimeException thrown = assertThrows(RuntimeException.class, action, "Should throw RuntimeException when there is a server error");

        assertTrue(thrown.getMessage().contains("Server-side error"), "Exception message should indicate a server-side error");
    }

    private static Stream<Exception> provideExceptions() {
        return Stream.of(
                new ErrorResponseException(new ErrorResponse(), new Response.Builder()
                        .request(new Request.Builder().url("http://localhost").build())
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Server error")
                        .build(), "Server error"), // 2024-07-06: Adjusted Response initialization
                new InsufficientDataException("Insufficient data"),
                new InternalException("Internal error", "Some additional message"), // 2024-07-06: Adjusted constructor
                new InvalidKeyException("Invalid key"),
                new InvalidResponseException(500, "Invalid response", "Some additional message", "More info"), // 2024-07-06: Adjusted constructor
                new IOException("I/O error"),
                new NoSuchAlgorithmException("Algorithm not found"),
                new ServerException("Server error", 500, "Some additional message"), // 2024-07-06: Adjusted constructor
                new XmlParserException(new Exception("XML parsing error")) // 2024-07-06: Adjusted constructor
        );
    }

    @ParameterizedTest
    @MethodSource("provideExceptions")
    void testExtractMetadata_Exceptions_ThrowsRuntimeException(Exception exception) throws Exception {
        when(minioClient.statObject(any())).thenThrow(exception);

        Executable action = () -> extractor.extractMetadata("test-bucket", "http://minio.example.com/test-bucket/testfile.txt");

        assertThrows(RuntimeException.class, action, "Should throw RuntimeException for any MinIO exception");
    }

    @Test
    void testExtractMetadata_MissingMetadata_ReturnsMetadataWithDefaults() throws Exception {
        String bucketName = "test-bucket";
        String fileUrl = "http://minio.example.com/test-bucket/testfile.txt";
        String fileName = "testfile.txt";

        when(statObjectResponse.object()).thenReturn(fileName);
        when(statObjectResponse.size()).thenReturn(0L);
        when(statObjectResponse.contentType()).thenReturn(null);
        when(statObjectResponse.lastModified()).thenReturn(null);

        when(minioClient.statObject(any())).thenReturn(statObjectResponse);

        FileMetadata metadata = extractor.extractMetadata(bucketName, fileUrl);

        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(fileName, metadata.getFileName(), "File name should be correct");
        assertEquals(0L, metadata.getFileSize(), "File size should be zero if unknown");
        assertEquals(metadata.getFileType(), "unknown", "File type should be unknown if null");

        assertNotNull(metadata.getUploadDate(), "Upload date should not be null even if MinIO provides no date");
        long timeDifference = Duration.between(metadata.getUploadDate(), LocalDateTime.now()).getSeconds();
        assertTrue(timeDifference < 5, "Upload date should be close to the current time");
    }

    @Test
    void testExtractMetadata_NoSlashInUrl_HandlesCorrectly() throws Exception {
        String bucketName = "test-bucket";
        String fileUrl = "minio.example.comtestfile.txt?query=string";
        when(minioClient.statObject(any())).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn(1234L);
        when(statObjectResponse.contentType()).thenReturn("text/plain");
        when(statObjectResponse.lastModified()).thenReturn(null);

        FileMetadata metadata = extractor.extractMetadata(bucketName, fileUrl);

        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("minio.example.comtestfile.txt", metadata.getFileUrl(), "File URL should be correctly extracted without parameters");
        assertEquals("minio.example.comtestfile.txt", metadata.getFileName(), "File name should match the URL path when no '/' present");
        assertEquals(1234L, metadata.getFileSize(), "File size should be correctly set");
        assertEquals("text/plain", metadata.getFileType(), "File type should be correctly set");
    }

    private static Stream<Arguments> provideLongValues() {
        return Stream.of(
                Arguments.of("a".repeat(256), "http://minio.example.com/" + "a".repeat(256) + "?query=string", 1234L),
                // Длинное имя файла
                Arguments.of("name", "http://minio.example.com/" + "b".repeat(1000) + "/name?query=string",
                        1234L),  // Длинная ссылка
                Arguments.of("testfile.txt", "http://minio.example.com/testfile.txt", Long.MAX_VALUE)                    // Очень большой размер файла
        );
    }

    @ParameterizedTest
    @MethodSource("provideLongValues")
    void testExtractMetadata_LongValues(String fileName, String fileUrl, long fileSize) throws Exception {
        when(minioClient.statObject(any())).thenReturn(statObjectResponse);
        when(statObjectResponse.size()).thenReturn(fileSize);
        when(statObjectResponse.contentType()).thenReturn("text/plain");

        FileMetadata metadata = extractor.extractMetadata("test-bucket", fileUrl);

        assertNotNull(metadata, "Metadata should not be null");
        assertEquals(fileUrl.split("\\?")[0], metadata.getFileUrl(), "File URL should be correctly extracted without parameters");
        assertEquals(fileName, metadata.getFileName(), "File name should match the provided one");
        assertEquals(fileSize, metadata.getFileSize(), "File size should match the provided one");
    }

    private static Stream<ZonedDateTime> provideLastModifiedDates() {
        return Stream.of(
                ZonedDateTime.now(ZoneId.of("UTC")),
                null
        );
    }

    @ParameterizedTest
    @MethodSource("provideLastModifiedDates")
    void testExtractMetadata_LastModifiedDate(ZonedDateTime lastModifiedDate) throws Exception {
        String bucketName = "test-bucket";
        String fileUrl = "http://minio.example.com/test-bucket/testfile.txt";
        when(minioClient.statObject(any())).thenReturn(statObjectResponse);
        when(statObjectResponse.lastModified()).thenReturn(lastModifiedDate);

        LocalDateTime expectedDate;
        if (lastModifiedDate != null) {
            expectedDate = lastModifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else {
            expectedDate = LocalDateTime.now();
        }

        FileMetadata metadata = extractor.extractMetadata(bucketName, fileUrl);

        long timeDifference = Duration.between(metadata.getUploadDate(), expectedDate).getSeconds();
        assertTrue(timeDifference < 5, "Upload date should be close to the expected date");
    }


}

