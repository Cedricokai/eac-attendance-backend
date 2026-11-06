CREATE TABLE IF NOT EXISTS file_work_returns (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 file_id BIGINT NOT NULL,
 employee_id BIGINT NOT NULL,
 version_number BIGINT NOT NULL,
 checksum VARCHAR(64) NOT NULL,
 note VARCHAR(2000) NOT NULL,
 returned_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6) NULL,
 CONSTRAINT uq_file_work_return UNIQUE(file_id,employee_id,version_number),
 CONSTRAINT fk_work_return_file FOREIGN KEY(file_id) REFERENCES file_items(id) ON DELETE CASCADE,
 INDEX idx_work_return_file_time(file_id,returned_at)
);
