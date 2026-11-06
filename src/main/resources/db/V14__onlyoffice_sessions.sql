-- Additive connector state only. Canonical content remains in file_items/file_versions.
CREATE TABLE IF NOT EXISTS file_office_sessions (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 file_id BIGINT NOT NULL,
 document_key VARCHAR(100) NOT NULL UNIQUE,
 saved_version BIGINT NOT NULL,
 writable BOOLEAN NOT NULL DEFAULT FALSE,
 state VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 expires_at TIMESTAMP(6) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 participants TEXT NULL,
 last_checksum VARCHAR(64) NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 INDEX idx_office_file_state(file_id,state),
 CONSTRAINT fk_office_session_file FOREIGN KEY(file_id) REFERENCES file_items(id) ON DELETE CASCADE
) ENGINE=InnoDB;
-- Rollback: disable app.onlyoffice.enabled, retain this table and all file versions.
