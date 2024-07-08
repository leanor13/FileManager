package org.yulia.filemanagement.filemetadataservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "file_metadata", indexes = {
        @Index(name = "idx_file_size", columnList = "file_size"),
        @Index(name = "idx_file_type", columnList = "file_type"),
        @Index(name = "idx_file_name", columnList = "file_name")
})
public class FileMetadata {
    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_metadata_seq")
    @SequenceGenerator(name = "file_metadata_seq", sequenceName = "file_metadata_seq", allocationSize = 1)
    private Long id;

    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;
}