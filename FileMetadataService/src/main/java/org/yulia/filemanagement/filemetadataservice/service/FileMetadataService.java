package org.yulia.filemanagement.filemetadataservice.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yulia.filemanagement.filemetadataservice.constants.SizeUnit;
import org.yulia.filemanagement.filemetadataservice.dto.FileQueryDto;
import org.yulia.filemanagement.filemetadataservice.dto.FileUrlDto;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;
import org.yulia.filemanagement.filemetadataservice.repository.FileMetadataRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing file metadata.
 */
@Service
public class FileMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataExtractor fileMetadataExtractor;
    private final String bucketName;
    private final boolean showFileUrl;

    @Autowired
    public FileMetadataService(FileMetadataRepository fileMetadataRepository,
                               FileMetadataExtractor fileMetadataExtractor,
                               @Value("${minio.bucket-name}") String bucketName,
                               @Value("${file.metadata.showUrl}") boolean showFileUrl) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileMetadataExtractor = fileMetadataExtractor;
        this.bucketName = bucketName;
        this.showFileUrl = showFileUrl;
    }

    /**
     * Registers a file by extracting its metadata and saving it to the repository.
     *
     * @param fileUrlDto the DTO containing the URL of the file to register
     * @throws IllegalArgumentException if the fileUrlDto, fileUrl, or bucketName is null
     */
    public void registerFile(FileUrlDto fileUrlDto) {
        if (fileUrlDto == null || fileUrlDto.fileUrl() == null || bucketName == null) {
            throw new IllegalArgumentException("File URL, FileUrlDto, and bucket name cannot be null");
        }

        var fileUrl = fileUrlDto.fileUrl();

        try {
            var metadata = fileMetadataExtractor.extractMetadata(bucketName, fileUrl);

            var fileName = fileMetadataExtractor.extractName(fileUrl);
            var existingMetadata =
                    fileMetadataRepository.findByFileName(fileName);

            // check if file already exists in the database and update if necessary
            if (existingMetadata.isPresent()) {
                var updatedMetadata = existingMetadata.get();
                updatedMetadata.setFileUrl(fileUrl);
                updatedMetadata.setFileSize(metadata.getFileSize());
                updatedMetadata.setFileType(metadata.getFileType());
                updatedMetadata.setUploadDate(metadata.getUploadDate());
                fileMetadataRepository.save(updatedMetadata);
                logger.info("File metadata updated successfully: {}", fileName);
            } else {
                // create new record if file does not exist
                fileMetadataRepository.save(metadata);
                logger.info("File registered successfully: {}", fileName);
            }
        } catch (Exception ex) {
            handleException(ex, "Error during file registration for URL: " + fileUrl);
        }
    }

    /**
     * Finds files based on the specified query criteria.
     *
     * @param queryDto the DTO containing the query criteria
     * @return a list of FileMetadata objects that match the query criteria
     */
    public List<FileMetadata> findFiles(FileQueryDto queryDto) {
        try {
            var spec = createSpecification(queryDto);
            var files = fileMetadataRepository.findAll(spec);
            if (!showFileUrl) {
                files.forEach(file -> file.setFileUrl(null));
            }
            return files;
        } catch (Exception ex) {
            handleException(ex, "Error during file retrieval with query: " + queryDto);
            return null;
        }
    }

    /**
     * Deletes metadata for a file by its name.
     *
     * @param fileName the name of the file whose metadata is to be deleted
     * @return true if the metadata was deleted successfully, false otherwise
     */
    @Transactional
    public boolean deleteFileMetadata(String fileName) {
        var countBefore = fileMetadataRepository.count();
        fileMetadataRepository.deleteByFileName(fileName);
        var countAfter = fileMetadataRepository.count();
        return countBefore > countAfter;
    }

    // Create a specification for querying file metadata based on the provided criteria
    private Specification<FileMetadata> createSpecification(FileQueryDto queryDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            handleSizeParameters(queryDto, predicates, criteriaBuilder, root);
            if (queryDto.fileType() != null) {
                logger.info("Adding fileType predicate: {}", queryDto.fileType());
                predicates.add(criteriaBuilder.equal(root.get("fileType"), queryDto.fileType()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Handle size parameters for the query. Parameter validation is done in the controller.
    private void handleSizeParameters(FileQueryDto queryDto, List<Predicate> predicates,
                                      CriteriaBuilder criteriaBuilder, Root<FileMetadata> root) {
        var minSize = queryDto.minSize() != null ? convertSizeToBytes(queryDto.minSize(),
                queryDto.sizeUnit()) : null;
        var maxSize = queryDto.maxSize() != null ? convertSizeToBytes(queryDto.maxSize(),
                queryDto.sizeUnit()) : null;
        var equalSize = queryDto.equalSize() != null ? convertSizeToBytes(queryDto.equalSize(),
                queryDto.sizeUnit()) : null;

        if (equalSize != null) {
            predicates.add(criteriaBuilder.equal(root.get("fileSize"), equalSize));
        } else {
            if (minSize != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fileSize"), minSize));
            }
            if (maxSize != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fileSize"), maxSize));
            }
        }
    }

    // Convert size to bytes based on the specified size unit
    private long convertSizeToBytes(Long size, SizeUnit sizeUnit) {
        if (size == null) {
            return 0;  // If no size is provided, return 0 which implies no size restriction
        }

        return switch (sizeUnit) {
            case kb -> size * 1024;
            case mb -> size * 1024 * 1024;
            case gb -> size * 1024 * 1024 * 1024;
            case bytes -> size;
        };
    }

    // Handle exceptions and log appropriate messages
    private void handleException(Exception ex, String message) {
        if (ex instanceof IllegalArgumentException) {
            logger.error("{} - Illegal argument: {}", message, ex.getMessage(), ex);
            throw (IllegalArgumentException) ex;
        } else if (ex instanceof DataAccessException) {
            logger.error("{} - Database error: {}", message, ex.getMessage(), ex);
            throw new RuntimeException("Database error: " + ex.getMessage(), ex);
        } else {
            logger.error("{} - Unexpected error: {}", message, ex.getMessage(), ex);
            throw new RuntimeException("Unexpected error: " + ex.getMessage(), ex);
        }
    }
}
