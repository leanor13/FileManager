package org.yulia.filemanagement.filemetadataservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;
import org.yulia.filemanagement.filemetadataservice.service.FileMetadataService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileMetadataController.class)
public class FileMetadataControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileMetadataService fileMetadataService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private static Stream<Arguments> provideValidFileQueryParams() {
        return Stream.of(
                Arguments.of("text/plain", 512L, 2048L, null, "bytes"),
                Arguments.of("text/plain", null, null, 1024L, "bytes"),
                Arguments.of("text/plain", null, 2048L, null, "GB"),
                Arguments.of("text/plain", 512L, null, null, "mb"),
                Arguments.of("text/plain", null, null, null, "bytes")
        );
    }

    private static Stream<Arguments> provideInvalidFileQueryDtos() {
        return Stream.of(
                Arguments.of("file_type", 1024L, null, 1024L, "bytes",
                        "Cannot specify equal_size with min_size or max_size"), // Min and equalSize
                Arguments.of("file_type", null, 1024L, 1024L, "bytes",
                        "Cannot specify equal_size with min_size or max_size"), // Max and equalSize
                Arguments.of("file_type", -102L, null, null, "bytes",
                        "Size parameters cannot be negative"), // Negative minSize
                Arguments.of("file_type", null, -102L, null, "bytes",
                        "Size parameters cannot be negative"), // Negative maxSize
                Arguments.of("file_type", null, null, -102L, "bytes",
                        "Size parameters cannot be negative"), // Negative equalSize
                Arguments.of("file_type", 1024L, 2048L, 1024L, "bytes",
                        "Cannot specify equal_size with min_size or max_size"),  // All parameters
                Arguments.of("file_type", 10L, null, null, "dijfsdf",
                        "Invalid size unit: dijfsdf. Valid units are: bytes, kb, mb, gb."), // wrong size unit
                Arguments.of("file_type", 100L, 10L, null, "mb",
                        "min_size cannot be greater than max_size") // minSize > maxSize
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideValidFileQueryParams")
    void whenGetFiles_withValidParams_thenReturnsFiles(String fileType, Long minSize, Long maxSize, Long equalSize,
                                                       String sizeUnit) throws Exception {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName("example.txt");
        fileMetadata.setFileType("text/plain");
        fileMetadata.setFileSize(1024L);
        fileMetadata.setFileUrl("http://example.com/example.txt");
        List<FileMetadata> files = Collections.singletonList(fileMetadata);

        when(fileMetadataService.findFiles(any(FileQueryDto.class))).thenReturn(files);

        mockMvc.perform(get("/api/metadata/files")
                        .param("file_type", fileType)
                        .param("min_size", minSize != null ? String.valueOf(minSize) : "")
                        .param("max_size", maxSize != null ? String.valueOf(maxSize) : "")
                        .param("equal_size", equalSize != null ? String.valueOf(equalSize) : "")
                        .param("size_unit", sizeUnit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File search completed successfully"))
                .andExpect(jsonPath("$.data[0].fileName").value("example.txt"))
                .andExpect(jsonPath("$.data[0].fileType").value("text/plain"))
                .andExpect(jsonPath("$.data[0].fileSize").value(1024))
                .andExpect(jsonPath("$.data[0].fileUrl").value("http://example.com/example.txt"));
    }

    @Test
    void whenGetFiles_withoutParams_thenReturnsFiles() throws Exception {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName("example.txt");
        fileMetadata.setFileType("text/plain");
        fileMetadata.setFileSize(1024L);
        fileMetadata.setFileUrl("http://example.com/example.txt");
        List<FileMetadata> files = Collections.singletonList(fileMetadata);

        when(fileMetadataService.findFiles(any(FileQueryDto.class))).thenReturn(files);

        mockMvc.perform(get("/api/metadata/files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File search completed successfully"))
                .andExpect(jsonPath("$.data[0].fileName").value("example.txt"))
                .andExpect(jsonPath("$.data[0].fileType").value("text/plain"))
                .andExpect(jsonPath("$.data[0].fileSize").value(1024))
                .andExpect(jsonPath("$.data[0].fileUrl").value("http://example.com/example.txt"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFileQueryDtos")
    void whenGetFiles_withInvalidParameters_thenReturnsBadRequest(String fileType, Long minSize, Long maxSize,
                                                                  Long equalSize, String sizeUnit,
                                                                  String expectedErrorMessage) throws Exception {
        mockMvc.perform(get("/api/metadata/files")
                        .param("file_type", fileType)
                        .param("min_size", minSize != null ? String.valueOf(minSize) : "")
                        .param("max_size", maxSize != null ? String.valueOf(maxSize) : "")
                        .param("equal_size", equalSize != null ? String.valueOf(equalSize) : "")
                        .param("size_unit", sizeUnit)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid argument"))
                .andExpect(jsonPath("$.errors[0]").value(expectedErrorMessage));

        verify(fileMetadataService, times(0)).findFiles(any(FileQueryDto.class)); // Ensure findFiles is not called
    }

    @Test
    public void testRegisterFile_Success() throws Exception {
        FileUrlDto fileUrlDto = new FileUrlDto("http://example.com/file");
        doNothing().when(fileMetadataService).registerFile(fileUrlDto);

        mockMvc.perform(post("/api/metadata/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(fileUrlDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("File registered successfully"));

        verify(fileMetadataService).registerFile(fileUrlDto);
    }

    @Test
    public void testDeleteFileMetadata_Success() throws Exception {
        String fileName = "example.txt";
        when(fileMetadataService.deleteFileMetadata(fileName)).thenReturn(true);

        mockMvc.perform(delete("/api/metadata/delete")
                        .param("fileName", fileName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Metadata for file 'example.txt' deleted successfully."));

        verify(fileMetadataService).deleteFileMetadata(fileName);
    }

    @Test
    public void testDeleteFileMetadata_NotFound() throws Exception {
        String fileName = "nonexistent.txt";
        when(fileMetadataService.deleteFileMetadata(fileName)).thenReturn(false);

        mockMvc.perform(delete("/api/metadata/delete")
                        .param("fileName", fileName))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Metadata for file 'nonexistent.txt' not found."));

        verify(fileMetadataService).deleteFileMetadata(fileName);
    }

    @Test
    public void testRegisterFile_ServiceThrowsException() throws Exception {
        FileUrlDto fileUrlDto = new FileUrlDto("http://example.com/file");
        doThrow(new RuntimeException("Internal server error")).when(fileMetadataService).registerFile(fileUrlDto);

        mockMvc.perform(post("/api/metadata/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(fileUrlDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error: Internal server error"));

        verify(fileMetadataService).registerFile(fileUrlDto);
    }

    @Test
    public void testRegisterFile_InvalidInput() throws Exception {
        mockMvc.perform(post("/api/metadata/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid argument"));
    }

    @Test
    public void testDeleteFileMetadata_MissingParameters() throws Exception {
        mockMvc.perform(delete("/api/metadata/delete"))
                .andExpect(status().isBadRequest());

        verify(fileMetadataService, never()).deleteFileMetadata(anyString());
    }
}
