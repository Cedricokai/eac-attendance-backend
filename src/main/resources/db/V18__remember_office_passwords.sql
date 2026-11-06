-- Passwords are encrypted with APP_FILES_PASSWORD_KEY, which must remain outside this database.
CREATE TABLE IF NOT EXISTS file_office_passwords (
 file_id BIGINT NOT NULL PRIMARY KEY,
 secret_box TEXT NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 updated_by VARCHAR(180) NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_office_password_file FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE CASCADE
);
