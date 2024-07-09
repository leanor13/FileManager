package org.yulia.filemanagement.fileuploadservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.yulia.filemanagement.fileuploadservice.communication.CommunicationService;
import org.yulia.filemanagement.fileuploadservice.config.SecurityConfig;
import org.yulia.filemanagement.fileuploadservice.service.FileUploadService;
import org.yulia.filemanagement.fileuploadservice.service.MinioService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = FileUploadController.class)
@TestPropertySource(locations = "classpath:application.properties")
@Import(SecurityConfig.class)
public class FileUploadIntegrationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private FileUploadService fileUploadService;

    @MockBean
    private MinioService minioService;

    @MockBean
    private CommunicationService communicationService;

    @Test
    public void testSuccessfulUploadOfThreeFiles() throws Exception {
        // Mock successful interactions with MinioService
        given(minioService.uploadObject(anyString(), any(), anyLong(), anyString()))
                .willReturn("https://minio.example.com/file1")
                .willReturn("https://minio.example.com/file2")
                .willReturn("https://minio.example.com/file3");

        // Mock successful metadata service response
        given(communicationService.sendFileUrl(anyString()))
                .willReturn(ResponseEntity.status(HttpStatus.CREATED).body("{\"message\":\"File registered successfully\"}"));

        // Creating mock files
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "file1.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "file2.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content2".getBytes());
        MockMultipartFile file3 = new MockMultipartFile(
                "file",
                "file3.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content3".getBytes());

        // Perform the upload test
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file1).file(file2).file(file3)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("File uploaded successfully"))
                .andExpect(jsonPath("$[1].message").value("File uploaded successfully"))
                .andExpect(jsonPath("$[2].message").value("File uploaded successfully"));

        // Verify interactions with MinioService and CommunicationService
        verify(minioService, times(3)).uploadObject(anyString(), any(), anyLong(), anyString());
        verify(communicationService, times(3)).sendFileUrl(anyString());

        // Ensure delete operations are not called
        verify(minioService, never()).deleteObject(anyString());
        verify(communicationService, never()).sendDeleteMessage(anyString());
    }

    @Test
    public void testUploadThreeFilesAllFailMinio() throws Exception {
        // Создаем три файла
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "file1.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "file2.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content2".getBytes());
        MockMultipartFile file3 = new MockMultipartFile(
                "file",
                "file3.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "content3".getBytes());

        given(minioService.uploadObject(anyString(), any(), anyLong(), anyString()))
                .willThrow(new RuntimeException("Upload failed"));

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file1).file(file2).file(file3)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().isInternalServerError())  // Проверяем, что статус 500
                .andExpect(jsonPath("$[0].fileName").value("file1.txt"))
                .andExpect(jsonPath("$[0].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[0].status").value(500))
                .andExpect(jsonPath("$[1].fileName").value("file2.txt"))
                .andExpect(jsonPath("$[1].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[1].status").value(500))
                .andExpect(jsonPath("$[2].fileName").value("file3.txt"))
                .andExpect(jsonPath("$[2].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[2].status").value(500));

        verify(communicationService, never()).sendFileUrl(anyString());
    }

    public static Stream<Arguments> provideStatusCodes() {
        return Stream.of(
                Arguments.of(400, 400), // If CommunicationService returns 400, the overall response should also be 400
                Arguments.of(503, 500)  // If CommunicationService returns 503, map this to a 500 Internal Server Error
        );
    }

    @ParameterizedTest
    @MethodSource("provideStatusCodes")
    public void testUploadThreeFilesAllCommunicationErrors(int receivedStatus, int expectedStatus) throws Exception {
        // Mock successful interactions with MinioService
        given(minioService.uploadObject(anyString(), any(), anyLong(), anyString()))
                .willReturn("https://minio.example.com/file1")
                .willReturn("https://minio.example.com/file2")
                .willReturn("https://minio.example.com/file3");

        // Mock failed metadata service response with received status
        given(communicationService.sendFileUrl(anyString()))
                .willReturn(ResponseEntity.status(receivedStatus).body("{\"error\":\"Invalid request\"}"));

        // Creating mock files
        MockMultipartFile file1 = new MockMultipartFile("file", "file1.txt", MediaType.TEXT_PLAIN_VALUE, "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "file2.txt", MediaType.TEXT_PLAIN_VALUE, "content2".getBytes());
        MockMultipartFile file3 = new MockMultipartFile("file", "file3.txt", MediaType.TEXT_PLAIN_VALUE, "content3".getBytes());

        // Perform the upload test
        mockMvc.perform(multipart("/api/files/upload")
                        .file(file1).file(file2).file(file3)
                        .with(httpBasic("test_user", "test_password")))
                .andExpect(status().is(expectedStatus)) // Checking for the expected general response status
                .andExpect(jsonPath("$[0].fileName").exists())
                .andExpect(jsonPath("$[0].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[0].status").value(receivedStatus))
                .andExpect(jsonPath("$[1].fileName").exists())
                .andExpect(jsonPath("$[1].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[1].status").value(receivedStatus))
                .andExpect(jsonPath("$[2].fileName").exists())
                .andExpect(jsonPath("$[2].message").value("Failed to upload file. Please try again later."))
                .andExpect(jsonPath("$[2].status").value(receivedStatus));

        // Verify interactions
        verify(minioService, times(3)).uploadObject(anyString(), any(), anyLong(), anyString());
        verify(minioService).deleteObject("file1.txt");
        verify(minioService).deleteObject("file2.txt");
        verify(minioService).deleteObject("file3.txt");
        verify(communicationService).sendDeleteMessage("file1.txt");
        verify(communicationService).sendDeleteMessage("file2.txt");
        verify(communicationService).sendDeleteMessage("file3.txt");
    }

}
