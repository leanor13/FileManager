package org.yulia.filemanagement.fileuploadservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.yulia.filemanagement.fileuploadservice.communication.CommunicationService;
import org.yulia.filemanagement.fileuploadservice.communication.HTTPCommunicationService;
import org.yulia.filemanagement.fileuploadservice.dto.UploadResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
class FileUploadServiceTests {

    @Mock
    private MinioService minioService;
    @Mock
    private CommunicationService communicationService;

    private FileUploadService fileUploadService;

    private AutoCloseable closeable;

    @BeforeEach
    void setup() throws IOException {
        fileUploadService = new FileUploadService(minioService, 1024L, communicationService, 3, 100L);
        lenient().when(minioService.uploadObject(anyString(), any(), anyLong(), anyString()))
                .thenReturn("http://mockurl.com/filename.txt");
        lenient().when(communicationService.sendFileUrl(anyString()))
                .thenReturn(ResponseEntity.ok("URL Sent Successfully"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 500, 1024})
        // checking valid sizes
    void testUploadFile_Success(int fileSize) throws Exception {
        InputStream mockInputStream = new ByteArrayInputStream(new byte[fileSize]);
        MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain",
                mockInputStream.readAllBytes());
        UploadResult result = fileUploadService.uploadFile(file);

        // Assert success and correct URL
        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(HttpStatus.OK, result.status());
        assertTrue(result.fileUrl().isPresent());
        assertEquals("http://mockurl.com/filename.txt", result.fileUrl().get());
        verify(communicationService, times(1)).sendFileUrl("http://mockurl.com/filename.txt");
    }

    @Test
    void testUploadFile_NullFile() throws Exception {
        Optional<UploadResult> resultOptional = Optional.ofNullable(fileUploadService.uploadFile(null));

        // Assert failure and correct error messages
        assertTrue(resultOptional.isPresent(), "Result should be present");
        UploadResult result = resultOptional.get();
        assertNotNull(result, "Upload result should not be null");
        assertFalse(result.success(), "Upload should fail");
        assertEquals(HttpStatus.BAD_REQUEST, result.status(), "Status should be BAD_REQUEST");
        assertEquals("Failed to upload file. Please try again later.", result.userMessage(), "Failed to upload file. "
                + "Please try again later.");
        assertEquals("File is null.", result.internalMessage(), "File is null.");
        verify(communicationService, never()).sendFileUrl(anyString());
    }

    @Test
    public void testFileUploadWithNegativeSize() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(-1L); // set up negative file size

        UploadResult result = fileUploadService.uploadFile(file);

        // Assert failure and correct error messages
        assertFalse(result.success(), "Upload should fail");
        assertEquals(HttpStatus.BAD_REQUEST, result.status());
        assertEquals("File size cannot be negative.", result.userMessage(), "File size cannot be negative.");
        assertEquals("File size cannot be negative.", result.internalMessage(), "File size cannot be negative.");
        verify(communicationService, never()).sendFileUrl(anyString());
    }

    @Test
    void testUploadFile_TooLargeSize() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "too-large.txt", "text/plain", new byte[2048]); //
        // File size exceeds max limit

        UploadResult result = fileUploadService.uploadFile(file);

        // Assert failure and correct error messages
        assertNotNull(result);
        assertFalse(result.success());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, result.status());
        assertEquals("File size exceeds the maximum limit.", result.internalMessage());
        verify(communicationService, never()).sendFileUrl(anyString());
    }

    @Test
    void testUploadFile_ServiceNotFound2() throws IOException {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String responseBody = "Service not found";
        runTestWithMockServer(status, responseBody);
    }

    @Test
    void testUploadFile_ResponseNotReceived() throws IOException {
        HttpStatus status = HttpStatus.GATEWAY_TIMEOUT;
        String responseBody = "Response not received";
        runTestWithMockServer(status, responseBody);
    }

    @Test
    void testUploadFile_ErrorResponseReceived() throws IOException {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String responseBody = "Error processing request";
        runTestWithMockServer(status, responseBody);
    }

    void runTestWithMockServer(HttpStatus status, String responseBody) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        CommunicationService customCommunicationService = new HTTPCommunicationService(restTemplate, "http://metadata.url");
        fileUploadService = new FileUploadService(minioService, 1024L, customCommunicationService, 3, 100L);

        MultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "content".getBytes());
        when(minioService.uploadObject(anyString(), any(), anyLong(), anyString())).thenReturn("http://mockurl.com/file");

        // Expect POST request for file upload attempt
        mockServer.expect(ExpectedCount.times(3), MockRestRequestMatchers.requestTo("http://metadata.url/register"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(status).body(responseBody));

        // Expect DELETE request for file deletion
        mockServer.expect(ExpectedCount.once(), MockRestRequestMatchers.requestTo("http://metadata.url/delete?fileName=filename.txt"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));

        doNothing().when(minioService).deleteObject(anyString());

        long startTime = System.nanoTime();
        UploadResult result = fileUploadService.uploadFile(file);
        long endTime = System.nanoTime();

        long durationMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        long expectedTime = 200;  // 3 retries with 100ms sleep each between

        // Assert that the actual duration meets the expected minimum time
        assertTrue(durationMillis >= expectedTime, "Expected at least " + expectedTime + "ms for three retries with 100ms sleep, got " + durationMillis + "ms");

        mockServer.verify();
        verify(minioService, times(1)).deleteObject(anyString());
    }


    @Test
    void testUploadFile_ThirdAttemptSuccess() throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        CommunicationService customCommunicationService = new HTTPCommunicationService(restTemplate, "http://metadata"
                + ".url");
        fileUploadService = new FileUploadService(minioService, 1024L, customCommunicationService, 3, 100L);

        MultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain", "content".getBytes());
        when(minioService.uploadObject(anyString(), any(), anyLong(), anyString())).thenReturn("http://mockurl" +
                ".com/file");

        // First two attempts return error
        mockServer.expect(ExpectedCount.times(1), MockRestRequestMatchers.requestTo("http://metadata.url/register")).andExpect(MockRestRequestMatchers.method(HttpMethod.POST)).andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Error " + "processing request"));

        mockServer.expect(ExpectedCount.times(1), MockRestRequestMatchers.requestTo("http://metadata.url/register")).andExpect(MockRestRequestMatchers.method(HttpMethod.POST)).andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("Error " + "processing request"));

        // Third attempt succeeds
        mockServer.expect(ExpectedCount.times(1), MockRestRequestMatchers.requestTo("http://metadata.url/register")).andExpect(MockRestRequestMatchers.method(HttpMethod.POST)).andRespond(MockRestResponseCreators.withSuccess());

        long startTime = System.currentTimeMillis();
        UploadResult result = fileUploadService.uploadFile(file);
        long endTime = System.currentTimeMillis();

        // Assert success and correct messages
        assertTrue(result.success());
        assertEquals(HttpStatus.OK, result.status());
        assertEquals("File uploaded successfully", result.internalMessage());
        assertEquals("File uploaded successfully", result.userMessage());

        mockServer.verify();

        long expectedTime = 2 * 100L;  // 2 retries with 100ms sleep
        assertTrue((endTime - startTime) >= expectedTime,
                "Expected at least " + expectedTime + "ms for two retries " + "with 100ms sleep");

        verify(minioService, never()).deleteObject(anyString());
    }

    @Test
    void testUploadFile_TwoFilesSimultaneously() throws InterruptedException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        CommunicationService customCommunicationService = new HTTPCommunicationService(restTemplate, "http://metadata"
                + ".url");
        fileUploadService = new FileUploadService(minioService, 1024L, customCommunicationService, 3, 100L);

        // Setup MockRestServiceServer for successful responses
        mockServer.expect(ExpectedCount.manyTimes(),
                MockRestRequestMatchers.requestTo("http://metadata.url/register")).andExpect(MockRestRequestMatchers.method(HttpMethod.POST)).andRespond(MockRestResponseCreators.withSuccess());

        MultipartFile file1 = new MockMultipartFile("file", "filename1.txt", "text/plain", "content1".getBytes());
        MultipartFile file2 = new MockMultipartFile("file", "filename2.txt", "text/plain", "content2".getBytes());

        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Upload files simultaneously
        executor.execute(() -> {
            fileUploadService.uploadFile(file1);
            latch.countDown();
        });

        executor.execute(() -> {
            fileUploadService.uploadFile(file2);
            latch.countDown();
        });

        latch.await();
        executor.shutdown();

        // Verify correct file uploads
        verify(minioService, times(1)).uploadObject(eq("filename1.txt"), any(), anyLong(), anyString());
        verify(minioService, times(1)).uploadObject(eq("filename2.txt"), any(), anyLong(), anyString());

        mockServer.verify();
    }

    @Test
    void testUploadFile_MinioFailure() throws Exception {
        // Mock MinioService to simulate upload failure
        doThrow(new IOException("Failed to upload to Minio.")).when(minioService).uploadObject(anyString(), any(),
                anyLong(), anyString());

        InputStream mockInputStream = new ByteArrayInputStream(new byte[1024]); // Example file size
        MockMultipartFile file = new MockMultipartFile("file", "filename.txt", "text/plain",
                mockInputStream.readAllBytes());

        UploadResult result = fileUploadService.uploadFile(file);

        // Assert failure and correct error messages
        assertNotNull(result);
        assertFalse(result.success());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.status());
        assertFalse(result.fileUrl().isPresent()); // No file URL should be present
        assertEquals("Failed to upload file. Please try again later.", result.userMessage());
        assertEquals("Failed to upload to Minio.", result.internalMessage());

        // Verify that communication service was not invoked
        verifyNoInteractions(communicationService);

        // Verify uploadObject was called exactly once with the correct parameters
        verify(minioService, times(1)).uploadObject(eq("filename.txt"), any(), anyLong(), anyString());
    }

    @Test
    void testUploadFile_DuplicateUpload() throws Exception {
        // Mock MinioService to simulate successful upload of initial file
        when(minioService.uploadObject(anyString(), any(), anyLong(), anyString())).thenReturn("http://mockurl" +
                ".com/filename.txt");

        // First upload
        InputStream mockInputStream1 = new ByteArrayInputStream(new byte[1024]); // Example file size
        MockMultipartFile file1 = new MockMultipartFile("file", "filename.txt", "text/plain",
                mockInputStream1.readAllBytes());

        UploadResult result1 = fileUploadService.uploadFile(file1);

        // Assert success and correct URL for first upload
        assertNotNull(result1);
        assertTrue(result1.success());
        assertEquals(HttpStatus.OK, result1.status());
        assertTrue(result1.fileUrl().isPresent());
        assertEquals("http://mockurl.com/filename.txt", result1.fileUrl().get());
        assertEquals("File uploaded successfully", result1.userMessage());
        assertEquals("File uploaded successfully", result1.internalMessage());

        // Second upload (simulating overwrite of existing file)
        InputStream mockInputStream2 = new ByteArrayInputStream(new byte[1024]); // Example file size
        MockMultipartFile file2 = new MockMultipartFile("file", "filename.txt", "text/plain",
                mockInputStream2.readAllBytes());

        UploadResult result2 = fileUploadService.uploadFile(file2);

        // Assert success and correct URL for second upload
        assertNotNull(result2);
        assertTrue(result2.success());
        assertEquals(HttpStatus.OK, result2.status());
        assertTrue(result2.fileUrl().isPresent());
        assertEquals("http://mockurl.com/filename.txt", result2.fileUrl().get());
        assertEquals("File uploaded successfully", result2.userMessage());
        assertEquals("File uploaded successfully", result2.internalMessage());

        // Verify that minioService.uploadObject was called twice with the correct parameters
        verify(minioService, times(2)).uploadObject(eq("filename.txt"), any(), anyLong(), anyString());

        // Verify that communication service was invoked twice with the correct file URL
        verify(communicationService, times(2)).sendFileUrl("http://mockurl.com/filename.txt");
    }
}