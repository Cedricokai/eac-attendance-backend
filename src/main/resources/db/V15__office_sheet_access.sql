-- Additive; apply after backing up MySQL. Existing workbook bytes are untouched.
CREATE TABLE IF NOT EXISTS file_office_sheet_policies (
 file_id BIGINT NOT NULL PRIMARY KEY,
 rules LONGTEXT NOT NULL,
 pending BIT NOT NULL,
 policy_version BIGINT NOT NULL,
 changed_at DATETIME(6) NOT NULL,
 changed_by VARCHAR(255),
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_office_sheet_policy_file FOREIGN KEY(file_id) REFERENCES file_items(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS file_office_sheet_policy_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 file_id BIGINT NOT NULL,
 policy_version BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 actor_name VARCHAR(255) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 rules LONGTEXT NOT NULL,
 INDEX idx_sheet_policy_file(file_id)
);
