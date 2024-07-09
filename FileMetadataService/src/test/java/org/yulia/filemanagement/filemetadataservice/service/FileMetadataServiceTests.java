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
}

