package org.yulia.filemanagement.filemetadataservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.Specification;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;
import org.yulia.filemanagement.filemetadataservice.repository.FileMetadataRepository;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class FileMetadataServiceTests {

    @MockBean
    private FileMetadataRepository fileMetadataRepository;

    @MockBean
    private FileMetadataExtractor fileMetadataExtractor;

    @Autowired
    private FileMetadataService fileMetadataService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void whenRegisterFile_withNullUrl_thenThrowsException() {
        // Given
        FileUrlDto fileUrlDto = new FileUrlDto(null);

        // When
        Executable action = () -> fileMetadataService.registerFile(fileUrlDto);

        // Then
        assertThrows(IllegalArgumentException.class, action, "Should throw IllegalArgumentException for null URL");
        verify(fileMetadataRepository, times(0)).save(any(FileMetadata.class)); // Ensure no save operation was called
    }

    private static Stream<Arguments> provideInvalidUrlsAndExceptions() {
        return Stream.of(
                Arguments.of("http://example.com/invalid", new RuntimeException("Invalid URL")),
                Arguments.of("http://example.com/illegal", new IllegalArgumentException("Illegal URL"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUrlsAndExceptions")
    void whenRegisterFile_withInvalidUrl_thenThrowsException(String url, Exception exception) {
        // Given
        FileUrlDto fileUrlDto = new FileUrlDto(url);

        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenThrow(exception);

        // When
        Executable action = () -> fileMetadataService.registerFile(fileUrlDto);

        // Then
        assertThrows(exception.getClass(), action, "Should throw the expected exception");
        verify(fileMetadataRepository, times(0)).save(any(FileMetadata.class)); // Ensure no save operation was called
    }

    private static Stream<Arguments> provideInvalidFileQueryDtos() {
        return Stream.of(
                Arguments.of(new FileQueryDto(null, 1024L, null, 1024L, SizeUnit.bytes)), // Min and equalSize
                Arguments.of(new FileQueryDto(null, null, 1024L, 1024L, SizeUnit.bytes)), // Max and equalSize
                Arguments.of(new FileQueryDto(null, -102L, null, null, SizeUnit.bytes)), // Negative minSize
                Arguments.of(new FileQueryDto(null, null, -1024L, null, SizeUnit.bytes)), // Negative maxSize
                Arguments.of(new FileQueryDto(null, null, null, -1024L, SizeUnit.bytes)), // Negative equalSize
                Arguments.of(new FileQueryDto(null, 1024L, 2048L, 1024L, SizeUnit.bytes))  // All parameters
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFileQueryDtos")
    void whenFindFiles_withInvalidParameters_thenThrowsException(FileQueryDto queryDto) {
        // When
        Executable action = () -> fileMetadataService.findFiles(queryDto);

        // Then
        assertThrows(IllegalArgumentException.class, action, "Should throw IllegalArgumentException for invalid query parameters");
        verify(fileMetadataRepository, times(0)).findAll(any(Specification.class)); // Ensure no find operation was called
    }

}

