-- Add retry-safe fuel submissions without modifying existing purchases.
DROP PROCEDURE IF EXISTS eac_transport_fuel_submission_keys;
DELIMITER $$
CREATE PROCEDURE eac_transport_fuel_submission_keys()
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='transport_fuel_details' AND column_name='request_key') THEN
  ALTER TABLE transport_fuel_details ADD COLUMN request_key VARCHAR(120) NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='transport_fuel_details' AND column_name='request_hash') THEN
  ALTER TABLE transport_fuel_details ADD COLUMN request_hash VARCHAR(64) NULL;
 END IF;
 IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='transport_fuel_details' AND column_name='request_key' AND non_unique=0) THEN
  ALTER TABLE transport_fuel_details ADD UNIQUE KEY uk_transport_fuel_request_key(request_key);
 END IF;
END$$
DELIMITER ;
CALL eac_transport_fuel_submission_keys();
DROP PROCEDURE eac_transport_fuel_submission_keys;
