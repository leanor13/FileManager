CREATE SEQUENCE IF NOT EXISTS file_metadata_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS file_metadata (
                                             id BIGSERIAL PRIMARY KEY DEFAULT nextval('file_metadata_seq'),
                                             file_url VARCHAR(1024) NOT NULL,
                                             file_name VARCHAR(255) NOT NULL,
                                             file_type VARCHAR(50) NOT NULL,
                                             file_size BIGINT NOT NULL,
                                             upload_date TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_file_size ON file_metadata(file_size);
CREATE INDEX IF NOT EXISTS idx_file_type ON file_metadata(file_type);
CREATE INDEX IF NOT EXISTS idx_file_name ON file_metadata(file_name);