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
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
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

    /**
     * Constructs a FileMetadataController with dependency injection for the FileMetadataService.
     * @param fileMetadataService the service used to manage file metadata operations
     */
    @Autowired
    public FileMetadataController(FileMetadataService fileMetadataService) {
        this.fileMetadataService = fileMetadataService;
        logger.debug("FileMetadataController initialized with FileMetadataService");
    }

    /**
     * Registers file metadata using a given URL.
     * @param fileUrlDto DTO containing the URL of the file to register
     * @return ResponseEntity containing a success message if registration is successful
     */
    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> registerFile(@RequestBody @NotNull FileUrlDto fileUrlDto) {
        logger.info("Received request to register file");
        fileMetadataService.registerFile(fileUrlDto);

        var response = new SuccessResponse("File registered successfully", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves a list of files based on filtering criteria.
     * @param file_type Optional file type to filter by
     * @param min_size Optional minimum file size to filter by
     * @param max_size Optional maximum file size to filter by
     * @param equal_size Optional exact file size to filter by
     * @param size_unit Optional unit of the size parameters, with a default if unspecified
     * @param defaultUnit The default unit to use for size filtering, injected from application properties
     * @return ResponseEntity containing the files matching the criteria or an empty list
     */
    @GetMapping("/files")
    public ResponseEntity<SuccessResponse> getFiles(
            @RequestParam(required = false) String file_type,
            @RequestParam(required = false) Long min_size,
            @RequestParam(required = false) Long max_size,
            @RequestParam(required = false) Long equal_size,
            @RequestParam(required = false) String size_unit,
            @Value("${default.size.unit}") String defaultUnit) {

        logger.info("Received request to filter files based on provided criteria");
        validateRequestParam(min_size, max_size, equal_size);

        var sizeUnit = SizeUnit.fromString(size_unit, defaultUnit);
        var queryDto = new FileQueryDto(file_type, min_size, max_size, equal_size, sizeUnit);
        List<FileMetadata> files = fileMetadataService.findFiles(queryDto);
        var response = new SuccessResponse("File search completed successfully", files);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes metadata for a specified file name.
     * @param fileName The name of the file whose metadata is to be deleted
     * @return ResponseEntity indicating success or failure of deletion
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFileMetadata(@RequestParam @NotNull String fileName) {
        try {
            if (fileMetadataService.deleteFileMetadata(fileName)) {
                var response = new SuccessResponse("Metadata for file '" + fileName + "' deleted successfully.", null);
                return ResponseEntity.ok(response);
            } else {
                var errorResponse = new ErrorResponse("Metadata for file '" + fileName + "' not found.",
                        List.of("No metadata available for the specified file name."));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error occurred while deleting metadata for file '{}': {}", fileName, e.getMessage());
            var errorResponse = new ErrorResponse("Error deleting metadata for file '" + fileName + "'.",
                    List.of("Internal server error occurred."));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validates the size parameters for file querying.
     * @param min_size Minimum size to filter by, must be non-negative
     * @param max_size Maximum size to filter by, must be non-negative and greater than or equal to min_size
     * @param equal_size Exact size to filter by, cannot be used with min_size or max_size
     * Throws IllegalArgumentException if size parameters are invalid
     */
    private void validateRequestParam(Long min_size, Long max_size, Long equal_size) {
        if ((min_size != null && min_size < 0) || (max_size != null && max_size < 0) || (equal_size != null && equal_size < 0)) {
            throw new IllegalArgumentException("Size parameters cannot be negative");
        }
        if (equal_size != null && (min_size != null || max_size != null)) {
            throw new IllegalArgumentException("Cannot specify equalSize with minSize or maxSize");
        }
        if (min_size != null && max_size != null && min_size > max_size) {
            throw new IllegalArgumentException("MinSize cannot be greater than maxSize");
        }
    }
}



