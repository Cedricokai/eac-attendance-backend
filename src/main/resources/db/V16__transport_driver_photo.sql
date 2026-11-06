-- Additive driver photo reference. Apply through the normal backed-up migration workflow.
-- Hibernate schema update can create the same nullable column for the existing local profile.
SET @photo_column = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='transport_driver_profiles' AND column_name='photo_file_id');
SET @photo_sql = IF(@photo_column=0,'ALTER TABLE transport_driver_profiles ADD COLUMN photo_file_id BIGINT NULL','SELECT 1');
PREPARE photo_stmt FROM @photo_sql;
EXECUTE photo_stmt;
DEALLOCATE PREPARE photo_stmt;
SET @photo_fk = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name='transport_driver_profiles' AND constraint_name='fk_transport_driver_photo');
SET @photo_sql = IF(@photo_fk=0,'ALTER TABLE transport_driver_profiles ADD CONSTRAINT fk_transport_driver_photo FOREIGN KEY(photo_file_id) REFERENCES file_items(id) ON DELETE SET NULL','SELECT 1');
PREPARE photo_stmt FROM @photo_sql;
EXECUTE photo_stmt;
DEALLOCATE PREPARE photo_stmt;
