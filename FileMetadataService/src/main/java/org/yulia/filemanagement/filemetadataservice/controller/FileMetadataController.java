package org.yulia.filemanagement.filemetadataservice.controller;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;
import org.yulia.filemanagement.filemetadataservice.dto.ErrorResponse;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.SuccessResponse;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;
import org.yulia.filemanagement.filemetadataservice.service.FileMetadataService;

import java.util.List;

@RestController
@RequestMapping("/api/metadata")
@Validated
public class FileMetadataController {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataController.class);

    private final FileMetadataService fileMetadataService;

    @Autowired
    public FileMetadataController(FileMetadataService fileMetadataService) {
        this.fileMetadataService = fileMetadataService;
    }

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> registerFile(@RequestBody @NotNull FileUrlDto fileUrlDto) {
        logger.info("Received request to register file");
        fileMetadataService.registerFile(fileUrlDto);

        var response = new SuccessResponse("File registered successfully", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/files")
    public ResponseEntity<SuccessResponse> getFiles(
            @RequestParam(required = false) String file_type,
            @RequestParam(required = false) Long min_size,
            @RequestParam(required = false) Long max_size,
            @RequestParam(required = false) Long equal_size,
            @RequestParam(required = false) String size_unit,
            @Value("${default.size.unit}") String defaultUnit) {

        logger.info("received request to filter files");

        validateRequestParam(min_size, max_size, equal_size);

        var sizeUnit = SizeUnit.fromString(size_unit, defaultUnit);

        var queryDto = new FileQueryDto(file_type, min_size, max_size, equal_size, sizeUnit);
        List<FileMetadata> files = fileMetadataService.findFiles(queryDto);
        var response = new SuccessResponse("Files retrieved successfully", files);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFileMetadata(@RequestParam @NotNull String fileName) {
        try {
            if (fileMetadataService.deleteFileMetadata(fileName)) {
                var response = new SuccessResponse("Metadata for file '" + fileName + "' deleted successfully.", null);
                return ResponseEntity.ok(response);
            } else {
                var errorResponse = new ErrorResponse("Metadata for file '" + fileName + "' not found.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            var errorResponse = new ErrorResponse("Error deleting metadata for file '" + fileName + "'.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private void validateRequestParam(Long min_size, Long max_size, Long equal_size) {
        if ((min_size != null && min_size < 0) ||
                (max_size != null && max_size < 0) ||
                (equal_size != null && equal_size < 0)) {
            throw new IllegalArgumentException("Size parameters cannot be negative");
        }
        if (equal_size != null && (min_size != null || max_size != null)) {
            throw new IllegalArgumentException("Cannot specify equalSize with minSize or maxSize");
        }
        if (min_size != null && max_size != null && min_size > max_size) {
            throw new IllegalArgumentException("minSize cannot be greater than maxSize");
        }
    }
}


