package org.yulia.filemanagement.fileuploadservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.yulia.filemanagement.fileuploadservice.dto.UploadResult;
import org.yulia.filemanagement.fileuploadservice.service.FileUploadService;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
public class FileUploadControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploadService fileUploadService;

    @Test
    public void testSuccessfulFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World".getBytes());

        UploadResult mockResult = new UploadResult(
                true,
                "File uploaded successfully",
                "Ok",
                HttpStatus.OK,
                Optional.of("File uploaded successfully")
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("File uploaded successfully"));  // Adjust JSON path as needed based on actual response structure
    }


    @Test
    public void testUploadResultIsNull() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World".getBytes());

        given(fileUploadService.uploadFile(file)).willReturn(null);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Check file name in the response
                .andExpect(jsonPath("$[0].message").value("File upload failed due to server error.")) // Check error message in the response
                .andExpect(jsonPath("$[0].status").value(500)); // Check the status code in the response
    }


    @Test
    public void testUploadResultUserMessageIsNull() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World".getBytes());

        UploadResult mockResult = new UploadResult(
                true,
                null,  // User message is null
                "Ok",
                HttpStatus.OK,
                Optional.empty()
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Check file name in the response
                .andExpect(jsonPath("$[0].message").value("File upload failed due to server error.")) // Check error message in the response
                .andExpect(jsonPath("$[0].status").value(500));
    }


    @Test
    public void testUploadResultStatusIsNull() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Hello World".getBytes());

        UploadResult mockResult = new UploadResult(
                true,
                "File upload successfully",
                null,
                null, // Status is null
                Optional.of("File upload successfully")
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Check file name in the response
                .andExpect(jsonPath("$[0].message").value("File upload failed due to server error.")) // Check error message in the response
                .andExpect(jsonPath("$[0].status").value(500));
    }

    @Test
    public void testUploadResultHttp400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Invalid content".getBytes());

        UploadResult mockResult = new UploadResult(
                false,
                "Invalid file format",
                "Bad Request",
                HttpStatus.BAD_REQUEST,
                Optional.empty()
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Verifying file name in response
                .andExpect(jsonPath("$[0].message").value("Invalid file format")) // Verifying error message in response
                .andExpect(jsonPath("$[0].status").value(400)); // Verifying status code in response
    }

    @Test
    public void testUploadResultHttp500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Server error content".getBytes());

        UploadResult mockResult = new UploadResult(
                false,
                "Error response due to server error",
                "Error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                Optional.empty()
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Check file name in the response
                .andExpect(jsonPath("$[0].message").value("Error response due to server error")) // Check error message in the response
                .andExpect(jsonPath("$[0].status").value(500));
    }

    @Test
    public void testUploadServiceThrowsRuntimeException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Some content".getBytes());

        UploadResult mockResult = new UploadResult(
                false,
                "Internal error occurred",
                "Error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                Optional.empty()
        );

        given(fileUploadService.uploadFile(file)).willReturn(mockResult);

        mockMvc.perform(multipart("/api/files/upload").file(file)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$[0].fileName").value("test.txt")) // Check file name in the response
                .andExpect(jsonPath("$[0].message").value("Internal error occurred")) // Check error message in the response
                .andExpect(jsonPath("$[0].status").value(500));
    }

    @Test
    public void testConcurrentFileUploads() throws Exception {
        // Mock files
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "first.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "First file content".getBytes());

        MockMultipartFile secondFile = new MockMultipartFile(
                "file",
                "second.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Second file content".getBytes());

        // Prepare results
        UploadResult firstResult = new UploadResult(true, "First file uploaded successfully", "Ok",
                HttpStatus.OK, java.util.Optional.of("first.txt"));

        UploadResult secondResult = new UploadResult(true, "Second file uploaded successfully", "Ok",
                HttpStatus.OK, java.util.Optional.of("second.txt"));

        // Setup mocks
        given(fileUploadService.uploadFile(firstFile)).willReturn(firstResult);
        given(fileUploadService.uploadFile(secondFile)).willReturn(secondResult);

        // Executor for parallel execution
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {

            // Callable tasks for each file upload
            Callable<MvcResult> uploadFirstFile = () -> mockMvc.perform(multipart("/api/files/upload")
                            .file(firstFile)
                            .with(httpBasic("test_user", "test_password")))
                    .andExpect(status().isOk())
                    .andReturn();

            Callable<MvcResult> uploadSecondFile = () -> mockMvc.perform(multipart("/api/files/upload")
                            .file(secondFile)
                            .with(httpBasic("test_user", "test_password")))
                    .andExpect(status().isOk())
                    .andReturn();

            // Execute tasks
            Future<MvcResult> firstFuture = executor.submit(uploadFirstFile);
            Future<MvcResult> secondFuture = executor.submit(uploadSecondFile);

            // Get and assert results
            MvcResult firstMvcResult = firstFuture.get();
            MvcResult secondMvcResult = secondFuture.get();

            // Ensure that responses are correct
            assert (firstMvcResult.getResponse().getContentAsString().contains("First file uploaded successfully"));
            assert (secondMvcResult.getResponse().getContentAsString().contains("Second file uploaded successfully"));

            executor.shutdown();
        }
    }




}

