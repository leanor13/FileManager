package org.yulia.filemanagement.filemetadataservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;
import org.yulia.filemanagement.filemetadataservice.repository.FileMetadataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest
@ActiveProfiles("test")
class FileMetadataServiceIntegrationTests {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileMetadataService fileMetadataService;

    @MockBean
    private FileMetadataExtractor fileMetadataExtractor;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    void testRegisterFile_WhenRepositoryIsEmpty() {
        // Given
        String fileUrl = "http://example.com/file.txt";
        FileUrlDto fileUrlDto = new FileUrlDto(fileUrl);
        FileMetadata extractedMetadata = new FileMetadata();
        extractedMetadata.setFileName("file.txt");
        extractedMetadata.setFileUrl(fileUrl);
        extractedMetadata.setFileSize(1234);
        extractedMetadata.setFileType("text/plain");
        extractedMetadata.setUploadDate(LocalDateTime.now());

        when(fileMetadataExtractor.extractName(fileUrl)).thenReturn("file.txt");
        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenReturn(extractedMetadata);

        // When
        fileMetadataService.registerFile(fileUrlDto);

        // Then
        assertEquals(1, fileMetadataRepository.count(), "There should be one record in the repository");
        assertTrue(fileMetadataRepository.findByFileName("file.txt").isPresent(), "File should be registered " +
                "successfully");
    }

    @Test
    void whenAddNewFile_andTableHasDifferentFile_thenTwoRecordsInTable() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(2048);
        existingMetadata.setFileType("application/pdf");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String fileUrl = "http://example.com/newFile.txt";
        FileUrlDto fileUrlDto = new FileUrlDto(fileUrl);
        FileMetadata extractedMetadata = new FileMetadata();
        extractedMetadata.setFileName("newFile.txt");
        extractedMetadata.setFileUrl(fileUrl);
        extractedMetadata.setFileSize(1234);
        extractedMetadata.setFileType("text/plain");
        extractedMetadata.setUploadDate(LocalDateTime.now());

        when(fileMetadataExtractor.extractName(fileUrl)).thenReturn("newFile.txt");
        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenReturn(extractedMetadata);

        // When
        fileMetadataService.registerFile(fileUrlDto);

        // Then
        assertEquals(2, fileMetadataRepository.count(), "There should be two records in the repository");
        assertTrue(fileMetadataRepository.findByFileName("existingFile.txt").isPresent(), "Existing file should be " +
                "present");
        assertTrue(fileMetadataRepository.findByFileName("newFile.txt").isPresent(), "New file should be registered " +
                "successfully");
    }

    @Test
    void whenAddNewFile_andTableHasSameContentDifferentName_thenTwoRecordsInTable() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(1234);
        existingMetadata.setFileType("text/plain");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String fileUrl = "http://example.com/newFile.txt";
        FileUrlDto fileUrlDto = new FileUrlDto(fileUrl);
        FileMetadata extractedMetadata = new FileMetadata();
        extractedMetadata.setFileName("newFile.txt");
        extractedMetadata.setFileUrl(fileUrl);
        extractedMetadata.setFileSize(1234);
        extractedMetadata.setFileType("text/plain");
        extractedMetadata.setUploadDate(LocalDateTime.now());

        when(fileMetadataExtractor.extractName(fileUrl)).thenReturn("newFile.txt");
        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenReturn(extractedMetadata);

        // When
        fileMetadataService.registerFile(fileUrlDto);

        // Then
        assertEquals(2, fileMetadataRepository.count(), "There should be two records in the repository");
        assertTrue(fileMetadataRepository.findByFileName("existingFile.txt").isPresent(), "Existing file should be " +
                "present");
        assertTrue(fileMetadataRepository.findByFileName("newFile.txt").isPresent(), "New file should be registered " +
                "successfully");
    }

    @Test
    void whenAddNewFile_andTableHasSameContentAndName_thenNoNewRecordAdded() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(1234);
        existingMetadata.setFileType("text/plain");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String fileUrl = "http://example.com/existingFile.txt";
        FileUrlDto fileUrlDto = new FileUrlDto(fileUrl);
        FileMetadata extractedMetadata = new FileMetadata();
        //extractedMetadata.setFileName("existingFile.txt");
        extractedMetadata.setFileUrl(fileUrl);
        extractedMetadata.setFileSize(1234);
        extractedMetadata.setFileType("text/plain");
        extractedMetadata.setUploadDate(LocalDateTime.now());

        when(fileMetadataExtractor.extractName(fileUrl)).thenReturn("existingFile.txt");
        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenReturn(extractedMetadata);

        // When
        fileMetadataService.registerFile(fileUrlDto);

        // Then
        long count = fileMetadataRepository.count();
        assertEquals(1, count, "There should be one record in the repository");

        Optional<FileMetadata> retrievedMetadata = fileMetadataRepository.findByFileName("existingFile.txt");
        assertTrue(retrievedMetadata.isPresent(), "File should be present in the repository");

        assertEquals(existingMetadata.getFileSize(), retrievedMetadata.get().getFileSize(), "File size should be the " +
                "same");
        assertEquals(existingMetadata.getFileType(), retrievedMetadata.get().getFileType(), "File type should be the " +
                "same");
        assertEquals(existingMetadata.getFileUrl(), retrievedMetadata.get().getFileUrl(), "File URL should be the " +
                "same");
    }

    @Test
    void whenAddNewFile_andTableHasDifferentName_thenRecordIsAdded() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(1234);
        existingMetadata.setFileType("text/plain");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String newFileUrl = "http://example.com/newFile.txt";
        FileUrlDto newFileUrlDto = new FileUrlDto(newFileUrl);
        FileMetadata extractedMetadata = new FileMetadata();
        extractedMetadata.setFileName("newFile.txt");
        extractedMetadata.setFileUrl(newFileUrl);
        extractedMetadata.setFileSize(1234);
        extractedMetadata.setFileType("text/plain");
        extractedMetadata.setUploadDate(LocalDateTime.now());

        when(fileMetadataExtractor.extractName(newFileUrl)).thenReturn("newFile.txt");
        when(fileMetadataExtractor.extractMetadata(anyString(), anyString())).thenReturn(extractedMetadata);

        // When
        fileMetadataService.registerFile(newFileUrlDto);

        // Then
        long count = fileMetadataRepository.count();
        assertEquals(2, count, "There should be two records in the repository");

        Optional<FileMetadata> retrievedMetadata = fileMetadataRepository.findByFileName("newFile.txt");
        assertTrue(retrievedMetadata.isPresent(), "New file should be present in the repository");

        assertEquals(extractedMetadata.getFileSize(), retrievedMetadata.get().getFileSize(), "File size should be the" +
                " same");
        assertEquals(extractedMetadata.getFileType(), retrievedMetadata.get().getFileType(), "File type should be the" +
                " same");
        assertEquals(extractedMetadata.getFileUrl(), retrievedMetadata.get().getFileUrl(), "File URL should be the " +
                "same");
    }

    @Test
    void whenFindFiles_withoutParameters_thenAllFilesReturned() {
        // Given
        FileMetadata file1 = new FileMetadata();
        file1.setFileName("file1.txt");
        file1.setFileUrl("http://example.com/file1.txt");
        file1.setFileSize(1234);
        file1.setFileType("text/plain");
        file1.setUploadDate(LocalDateTime.now());

        FileMetadata file2 = new FileMetadata();
        file2.setFileName("file2.txt");
        file2.setFileUrl("http://example.com/file2.txt");
        file2.setFileSize(2048);
        file2.setFileType("application/pdf");
        file2.setUploadDate(LocalDateTime.now());

        fileMetadataRepository.save(file1);
        fileMetadataRepository.save(file2);

        FileQueryDto queryDto = new FileQueryDto(null, null, null, null, null);

        // When
        List<FileMetadata> result = fileMetadataService.findFiles(queryDto);

        // Then
        assertEquals(2, result.size(), "All files should be returned");
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("file1.txt")), "file1.txt should be " +
                "present");
        assertTrue(result.stream().anyMatch(file -> file.getFileName().equals("file2.txt")), "file2.txt should be " +
                "present");
    }

    @Test
    void whenFindFiles_withVariousParameters_thenOnlyMatchingFilesReturned() {
        // Given
        FileMetadata file1 = new FileMetadata();
        file1.setFileName("file1.txt");
        file1.setFileUrl("http://example.com/file1.txt");
        file1.setFileSize(500 * 1024 * 1024); // 500 MB
        file1.setFileType("text/plain");
        file1.setUploadDate(LocalDateTime.now());

        FileMetadata file2 = new FileMetadata();
        file2.setFileName("file2.txt");
        file2.setFileUrl("http://example.com/file2.txt");
        file2.setFileSize(2 * 1024 * 1024 * 1024L); // 2 GB
        file2.setFileType("application/pdf");
        file2.setUploadDate(LocalDateTime.now());

        FileMetadata file3 = new FileMetadata();
        file3.setFileName("file3.txt");
        file3.setFileUrl("http://example.com/file3.txt");
        file3.setFileSize(5 * 1024 * 1024 * 1024L); // 5 GB
        file3.setFileType("application/pdf");
        file3.setUploadDate(LocalDateTime.now());

        FileMetadata file4 = new FileMetadata();
        file4.setFileName("file4.txt");
        file4.setFileUrl("http://example.com/file4.txt");
        file4.setFileSize(1024L); // 1 KB
        file4.setFileType("unknown");
        file4.setUploadDate(LocalDateTime.now());

        fileMetadataRepository.save(file1);
        fileMetadataRepository.save(file2);
        fileMetadataRepository.save(file3);
        fileMetadataRepository.save(file4);

        // Test case 1: SizeUnit = gb, excluding equalSize
        FileQueryDto queryDto1 = new FileQueryDto(null, 1L, 4L, null, SizeUnit.gb);
        List<FileMetadata> result1 = fileMetadataService.findFiles(queryDto1);
        assertEquals(1, result1.size(), "Only one file should match the criteria");
        assertTrue(result1.stream().anyMatch(file -> file.getFileName().equals("file2.txt")), "file2.txt should be " +
                "present");

        // Test case 2: excluding SizeUnit, max and min size, checking equalSize in bytes
        FileQueryDto queryDto2 = new FileQueryDto(null, null, null, 1024L, SizeUnit.bytes);
        List<FileMetadata> result2 = fileMetadataService.findFiles(queryDto2);
        assertEquals(1, result2.size(), "Only one file should match the criteria");
        assertTrue(result2.stream().anyMatch(file -> file.getFileName().equals("file4.txt")), "file4.txt should be " +
                "present");

        // Test case 3: Filter only by file type
        FileQueryDto queryDto3 = new FileQueryDto("application/pdf", null, null, null, SizeUnit.bytes);
        List<FileMetadata> result3 = fileMetadataService.findFiles(queryDto3);
        assertEquals(2, result3.size(), "Two files should match the criteria");
        assertTrue(result3.stream().anyMatch(file -> file.getFileName().equals("file2.txt")), "file2.txt should be " +
                "present");
        assertTrue(result3.stream().anyMatch(file -> file.getFileName().equals("file3.txt")), "file3.txt should be " +
                "present");

        // Test case 4: Filter by file type unknown
        FileQueryDto queryDto4 = new FileQueryDto("unknown", null, null, null, SizeUnit.bytes);
        List<FileMetadata> result4 = fileMetadataService.findFiles(queryDto4);
        assertEquals(1, result4.size(), "Only one file should match the criteria");
        assertTrue(result4.stream().anyMatch(file -> file.getFileName().equals("file4.txt")), "file4.txt should be " +
                "present");

        // Test case 5: no parameters at all with SizeUnit = bytes
        FileQueryDto queryDto5 = new FileQueryDto(null, null, null, null, SizeUnit.bytes);
        List<FileMetadata> result5 = fileMetadataService.findFiles(queryDto5);
        assertEquals(4, result5.size(), "All files match criteria");
        assertTrue(result5.stream().anyMatch(file -> file.getFileName().equals("file1.txt")), "file1.txt should be " +
                "present");

        // Test case 6: no parameters at all with SizeUnit = null
        FileQueryDto queryDto6 = new FileQueryDto(null, null, null, null, null);
        List<FileMetadata> result6 = fileMetadataService.findFiles(queryDto6);
        assertEquals(4, result6.size(), "All files match criteria");
        assertTrue(result6.stream().anyMatch(file -> file.getFileName().equals("file1.txt")), "file1.txt should be " +
                "present");

        // Test case 7: no files match criteria
        FileQueryDto queryDto7 = new FileQueryDto(null, 100L, null, null, SizeUnit.gb);
        List<FileMetadata> result7 = fileMetadataService.findFiles(queryDto7);
        assertEquals(0, result7.size(), "No files match criteria");
    }

    @Test
    void testDeleteFileMetadata_WhenTableIsEmpty() {
        // Given
        String fileName = "nonExistingFile.txt";
        long initialCount = fileMetadataRepository.count();

        // When
        boolean result = fileMetadataService.deleteFileMetadata(fileName);

        // Then
        long finalCount = fileMetadataRepository.count();
        assertFalse(result, "No record should be deleted");
        assertEquals(initialCount, finalCount, "Table should still be empty");
    }

    @Test
    void testDeleteFileMetadata_WhenFileExists() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(1234);
        existingMetadata.setFileType("text/plain");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String fileName = "existingFile.txt";
        long initialCount = fileMetadataRepository.count();

        // When
        boolean result = fileMetadataService.deleteFileMetadata(fileName);

        // Then
        long finalCount = fileMetadataRepository.count();
        assertTrue(result, "Record should be deleted");
        assertEquals(initialCount - 1, finalCount, "Table should have one less record");
        assertFalse(fileMetadataRepository.findByFileName(fileName).isPresent(), "Deleted file should not be present in the repository");
    }

    @Test
    void testDeleteFileMetadata_WhenFileDoesNotExist() {
        // Given
        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileName("existingFile.txt");
        existingMetadata.setFileUrl("http://example.com/existingFile.txt");
        existingMetadata.setFileSize(1234);
        existingMetadata.setFileType("text/plain");
        existingMetadata.setUploadDate(LocalDateTime.now());
        fileMetadataRepository.save(existingMetadata);

        String fileName = "nonExistingFile.txt";
        long initialCount = fileMetadataRepository.count();

        // When
        boolean result = fileMetadataService.deleteFileMetadata(fileName);

        // Then
        long finalCount = fileMetadataRepository.count();
        assertFalse(result, "No record should be deleted");
        assertEquals(initialCount, finalCount, "Table should have the same number of records");
        assertTrue(fileMetadataRepository.findByFileName("existingFile.txt").isPresent(), "Existing file should still be present in the repository");
    }


}
