-- Preserve the legacy Transport contact directory before its JPA mapping is isolated.
-- The old table is also the real application login table: never rename or delete it.
-- Run before deploying the new mapping. Re-running this migration retains new edits.
CREATE TABLE IF NOT EXISTS transport_directory (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 full_name VARCHAR(255),
 email VARCHAR(255),
 role VARCHAR(255),
 phone VARCHAR(255),
 archived_at TIMESTAMP(6) NULL,
 INDEX idx_transport_directory_archived (archived_at)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS eac_preserve_transport_directory;
DELIMITER $$
CREATE PROCEDURE eac_preserve_transport_directory()
BEGIN
 IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name IN ('id','full_name','email','role','phone'))=5 THEN
  INSERT INTO transport_directory (id,full_name,email,role,phone)
  SELECT u.id,u.full_name,u.email,u.role,u.phone FROM users u
  WHERE u.full_name IS NOT NULL AND TRIM(u.full_name)<>''
  AND NOT EXISTS (SELECT 1 FROM transport_directory d WHERE d.id=u.id);
 END IF;
END$$
DELIMITER ;
CALL eac_preserve_transport_directory();
DROP PROCEDURE eac_preserve_transport_directory;

-- No original users row, authentication field, role membership or foreign key is altered.
-- Rollback: keep both tables. Do not drop the copied directory or rewrite login records.
