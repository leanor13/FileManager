package org.yulia.filemanagement.fileuploadservice.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.communication.CommunicationService;
import org.yulia.filemanagement.fileuploadservice.exception.MinioServiceUnavailableException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
public class MinioServiceTests {

    @MockBean
    private MinioClient mockMinioClient;

    @MockBean
    private CommunicationService mockCommunicationService;

    @Autowired
    private MinioService minioService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Test
    void testSuccessfulUpload() throws Exception {
        String filename = "testfile.txt";
        InputStream data = new ByteArrayInputStream("file content".getBytes());
        long size = 12;
        String contentType = "text/plain";
        String expectedUrl = "http://example.com/testfile.txt";

        PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .stream(data, size, -1)
                .contentType(contentType)
                .build();

        GetPresignedObjectUrlArgs urlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(filename)
                .build();

        when(mockMinioClient.getPresignedObjectUrl(urlArgs)).thenReturn(expectedUrl);

        String resultUrl = minioService.uploadObject(filename, data, size, contentType);
        assertEquals(expectedUrl, resultUrl);
    }


    @Test
    public void testUploadWithSpecialCharactersInFilename() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "na@me#test$.txt", "text/plain", "content".getBytes());
        InputStream is = new ByteArrayInputStream(file.getBytes());

        minioService.uploadObject(file.getOriginalFilename(), is, file.getSize(), file.getContentType());
        verify(mockMinioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testUploadZeroLengthFile() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        InputStream is = new ByteArrayInputStream(file.getBytes());

        minioService.uploadObject(file.getOriginalFilename(), is, file.getSize(), file.getContentType());
        verify(mockMinioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testUploadNonStandardContentType() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "vector.svg", "image/svg+xml", "<svg></svg>".getBytes());
        InputStream is = new ByteArrayInputStream(file.getBytes());

        minioService.uploadObject(file.getOriginalFilename(), is, file.getSize(), file.getContentType());
        verify(mockMinioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testHandlingPermissionsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "readonly.txt", "text/plain", "cannot write".getBytes());
        doThrow(new RuntimeException("Permission denied")).when(mockMinioClient).putObject(any(PutObjectArgs.class));

        Exception exception = assertThrows(IOException.class,
                () -> minioService.uploadObject(file.getOriginalFilename(), file.getInputStream(), file.getSize(),
                        file.getContentType()));
        assertThat(exception.getMessage()).contains("Permission denied");
    }

    @Test
    public void testHandlingInvalidStream() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "corrupt.zip", "application/zip", new byte[0]);
        InputStream corruptedStream = new InputStream() {
            public int read() throws IOException {
                throw new IOException("Stream is corrupted");
            }
        };

        MinioClient mockMinioClient = mock(MinioClient.class);
        MinioService minioServiceWithMock = new MinioService(mockMinioClient, "test-bucket");

        doThrow(new IOException("Stream is corrupted")).when(mockMinioClient).putObject(any(PutObjectArgs.class));

        try (InputStream stream = corruptedStream) {
            Exception exception = assertThrows(IOException.class,
                    () -> minioServiceWithMock.uploadObject(file.getOriginalFilename(), stream, file.getSize(),
                            file.getContentType()));
            assertThat(exception.getMessage()).contains("Stream is corrupted");
        }
    }

    @Test
    public void testHandlingSimultaneousUploadsToSameFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "race-condition.txt", "text/plain",
                "content".getBytes());
        InputStream is1 = new ByteArrayInputStream(file.getBytes());
        InputStream is2 = new ByteArrayInputStream(file.getBytes());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> future1 = executor.submit(() -> {
            try {
                minioService.uploadObject(file.getOriginalFilename(), is1, file.getSize(), file.getContentType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Future<?> future2 = executor.submit(() -> {
            try {
                minioService.uploadObject(file.getOriginalFilename(), is2, file.getSize(), file.getContentType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        future1.get();
        future2.get();

        // Verify that the Minio client was called twice, potentially with some form of locking or versioning if
        // implemented
        verify(mockMinioClient, times(2)).putObject(any(PutObjectArgs.class));
    }

    @Test
    public void testHandlingNetworkFailureDuringFileTransfer() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "network-issue.txt", "text/plain",
                "will fail".getBytes());
        doThrow(new IOException("Network interruption")).when(mockMinioClient).putObject(any(PutObjectArgs.class));

        Exception exception = assertThrows(IOException.class,
                () -> minioService.uploadObject(file.getOriginalFilename(), file.getInputStream(), file.getSize(),
                        file.getContentType()));
        assertThat(exception.getMessage()).contains("Network interruption");
    }

    @Test
    public void testHandlingServiceUnavailable() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());
        doThrow(new MinioServiceUnavailableException("Service unavailable")).when(mockMinioClient).putObject(any(PutObjectArgs.class));

        Exception exception = assertThrows(IOException.class,
                () -> minioService.uploadObject(file.getOriginalFilename(), file.getInputStream(), file.getSize(),
                        file.getContentType()));
        assertThat(exception.getMessage()).contains("Service unavailable");
    }

    @Test
    void testSuccessfulDelete() throws Exception {
        String filename = "testfile.txt";

        // Prepare the arguments for the mock Minio client
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .build();

        // Mock the removeObject method to do nothing
        doNothing().when(mockMinioClient).removeObject(removeObjectArgs);

        // Call the method to be tested
        minioService.deleteObject(filename);

        // Verify that the removeObject method was called with the correct arguments
        verify(mockMinioClient, times(1)).removeObject(removeObjectArgs);
    }

    @Test
    void testDeleteWhenServiceUnavailable() throws Exception {
        String filename = "testfile.txt";

        // Prepare the arguments for the mock Minio client
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .build();

        // Mock the removeObject method to throw an IOException
        doThrow(new IOException("Service unavailable")).when(mockMinioClient).removeObject(removeObjectArgs);

        // Call the method to be tested and assert that it throws an IOException
        Exception exception = assertThrows(IOException.class, () -> minioService.deleteObject(filename));
        assertThat(exception.getMessage()).contains("Service unavailable");

        // Verify that the removeObject method was called with the correct arguments
        verify(mockMinioClient, times(1)).removeObject(removeObjectArgs);
    }

    @Test
    void testUploadWithInvalidBucketName() throws Exception {
        String invalidBucketName = " ";
        MinioService minioServiceWithInvalidBucket = new MinioService(mockMinioClient, invalidBucketName);
        String filename = "testfile.txt";
        InputStream data = new ByteArrayInputStream("file content".getBytes());
        long size = 12;
        String contentType = "text/plain";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                minioServiceWithInvalidBucket.uploadObject(filename, data, size, contentType));
        assertThat(exception.getMessage()).contains("Bucket name must not be empty.");
    }

    @Test
    void testDeleteWithInvalidBucketName() throws Exception {
        String invalidBucketName = " ";
        MinioService minioServiceWithInvalidBucket = new MinioService(mockMinioClient, invalidBucketName);
        String filename = "testfile.txt";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                minioServiceWithInvalidBucket.deleteObject(filename));
        assertThat(exception.getMessage()).contains("Bucket name must not be empty.");
    }


}

