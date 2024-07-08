package org.yulia.filemanagement.filemetadataservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.yulia.filemanagement.filemetadataservice.entity.FileMetadata;

import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long>,
        JpaSpecificationExecutor<FileMetadata> {
    Optional<FileMetadata> findByFileName(String fileName);
    void deleteByFileName(String fileName);
}
