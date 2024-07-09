package org.yulia.filemanagement.filemetadataservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;

import java.util.Optional;

/**
 * Repository interface for accessing and manipulating FileMetadata entities.
 * Extends JpaRepository for basic CRUD operations and JpaSpecificationExecutor for specification-based queries.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>,
        JpaSpecificationExecutor<FileMetadata> {

    /**
     * Finds a FileMetadata entity by its file name.
     *
     * @param fileName the name of the file
     * @return an Optional containing the FileMetadata if found, or empty if not found
     */
    Optional<FileMetadata> findByFileName(String fileName);

    /**
     * Deletes a FileMetadata entity by its file name.
     *
     * @param fileName the name of the file to delete
     */
    void deleteByFileName(String fileName);
}
