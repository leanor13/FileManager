package org.yulia.filemanagement.filemetadataservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
     *
     * @param fileMetadataService the service used to manage file metadata operations
     */
    @Autowired
    public FileMetadataController(FileMetadataService fileMetadataService) {
        this.fileMetadataService = fileMetadataService;
        logger.debug("FileMetadataController initialized with FileMetadataService");
    }

    @Operation(summary = "Registers file metadata using a given URL",
            description = "This endpoint registers file metadata provided the URL of the file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File registered successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = "{ \"message\": \"File registered successfully\", " +
                                    "\"data\": null }"))}),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> registerFile(@RequestBody @NotNull FileUrlDto fileUrlDto) {
        logger.info("Received request to register file");
        fileMetadataService.registerFile(fileUrlDto);

        var response = new SuccessResponse("File registered successfully", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieves a list of files based on filtering criteria",
            description = "This endpoint returns a list of files that match the provided filtering criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File search completed successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = "{ \"message\": \"File search completed successfully\"," +
                                    " \"data\": [...] }"))}),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/files")
    public ResponseEntity<SuccessResponse> getFiles(
            @Parameter(description = "Optional file type to filter by, e.g application/pdf, text/plain, etc.")
            @RequestParam(required = false) String file_type,
            @Parameter(description = "Optional minimum file size to filter by")
            @RequestParam(required = false) Long min_size,
            @Parameter(description = "Optional maximum file size to filter by")
            @RequestParam(required = false) Long max_size,
            @Parameter(description = "Optional exact file size to filter by")
            @RequestParam(required = false) Long equal_size,
            @Parameter(description = "Optional unit of the size parameters, with a default if unspecified",
                    schema = @Schema(defaultValue = "bytes", allowableValues = {"bytes", "kb", "mb", "gb"}))
            @RequestParam(required = false) String size_unit,
            @Parameter(hidden = true)
            @Value("${default.size.unit}") String defaultUnit) {

        logger.info("Received request to filter files based on provided criteria");
        validateRequestParam(min_size, max_size, equal_size);

        var sizeUnit = SizeUnit.fromString(size_unit, defaultUnit);
        var queryDto = new FileQueryDto(file_type, min_size, max_size, equal_size, sizeUnit);
        List<FileMetadata> files = fileMetadataService.findFiles(queryDto);
        var response = new SuccessResponse("File search completed successfully", files);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deletes metadata for a specified file name",
            description = "This endpoint deletes metadata for the file with the specified name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata deleted successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = @ExampleObject(value = "{ \"message\": \"Metadata for file 'example.pdf' " +
                                    "deleted successfully.\", \"data\": null }"))}),
            @ApiResponse(responseCode = "404", description = "Metadata not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
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
     *
     * @param min_size   Minimum size to filter by, must be non-negative
     * @param max_size   Maximum size to filter by, must be non-negative and greater than or equal to min_size
     * @param equal_size Exact size to filter by, cannot be used with min_size or max_size
     *                   Throws IllegalArgumentException if size parameters are invalid
     */
    private void validateRequestParam(Long min_size, Long max_size, Long equal_size) {
        if ((min_size != null && min_size < 0) || (max_size != null && max_size < 0) || (equal_size != null && equal_size < 0)) {
            throw new IllegalArgumentException("Size parameters cannot be negative");
        }
        if (equal_size != null && (min_size != null || max_size != null)) {
            throw new IllegalArgumentException("Cannot specify equal_size with min_size or max_size");
        }
        if (min_size != null && max_size != null && min_size > max_size) {
            throw new IllegalArgumentException("min_size cannot be greater than max_size");
        }
    }
}



