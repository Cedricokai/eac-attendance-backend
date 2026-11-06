-- Existing grants retain downloads. New restrictions apply per employee and inherit through folders.
SET @download_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='file_permissions' AND column_name='download_blocked');
SET @download_sql = IF(@download_column=0,'ALTER TABLE file_permissions ADD COLUMN download_blocked BOOLEAN NOT NULL DEFAULT FALSE','SELECT 1');
PREPARE download_stmt FROM @download_sql;
EXECUTE download_stmt;
DEALLOCATE PREPARE download_stmt;
