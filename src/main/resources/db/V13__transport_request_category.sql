-- Optional request categorization. Existing requests keep their unrecorded category.
DROP PROCEDURE IF EXISTS eac_transport_request_category;
DELIMITER $$
CREATE PROCEDURE eac_transport_request_category()
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='transport_procurement' AND column_name='category') THEN
  ALTER TABLE transport_procurement ADD COLUMN category VARCHAR(100) NULL;
 END IF;
END$$
DELIMITER ;
CALL eac_transport_request_category();
DROP PROCEDURE eac_transport_request_category;
