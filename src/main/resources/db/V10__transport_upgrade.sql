-- V10: additive Transport upgrade. No existing rows are deleted or replaced.
-- MySQL 8+. Apply after taking a database and managed-file backup.

CREATE TABLE IF NOT EXISTS transport_vehicle_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  driver_id BIGINT NULL,
  make VARCHAR(100) NULL,
  model VARCHAR(100) NULL,
  category VARCHAR(100) NULL,
  engine_number VARCHAR(120) NULL,
  transmission VARCHAR(60) NULL,
  ownership_type VARCHAR(60) NULL,
  department VARCHAR(120) NULL,
  project VARCHAR(180) NULL,
  acquisition_date DATE NULL,
  initial_odometer DECIMAL(19,3) NULL,
  primary_image_id BIGINT NULL,
  operational_status VARCHAR(40) NULL DEFAULT 'AVAILABLE',
  downtime_started_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_vehicle_profiles_active (archived_at,updated_at),
  CONSTRAINT fk_transport_vehicle_profiles_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_vehicle_profiles_vehicle (vehicle_id),
  CONSTRAINT fk_transport_vehicle_profiles_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_vehicle_profiles_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
  CONSTRAINT fk_transport_vehicle_profiles_primaryImage FOREIGN KEY (primary_image_id) REFERENCES file_items(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_driver_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  driver_id BIGINT NOT NULL,
  account_id BIGINT NULL,
  employee_id BIGINT NULL,
  email VARCHAR(254) NULL,
  license_class VARCHAR(60) NULL,
  department VARCHAR(120) NULL,
  project VARCHAR(180) NULL,
  last_invitation_at TIMESTAMP(6) NULL,
  managed_account BOOLEAN NOT NULL DEFAULT FALSE,
  notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  INDEX idx_transport_driver_profiles_active (archived_at,updated_at),
  CONSTRAINT fk_transport_driver_profiles_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_driver_profiles_driver (driver_id),
  CONSTRAINT fk_transport_driver_profiles_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
  UNIQUE KEY uk_transport_driver_profiles_account (account_id),
  CONSTRAINT fk_transport_driver_profiles_account FOREIGN KEY (account_id) REFERENCES users(id),
  CONSTRAINT fk_transport_driver_profiles_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_odometer (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  reading DECIMAL(19,3) NULL,
  reading_date DATE NULL,
  source VARCHAR(60) NULL,
  reference VARCHAR(120) NULL,
  superseded_at TIMESTAMP(6) NULL,
  superseded_reason TEXT NULL,
  correction BOOLEAN NOT NULL,
  reason VARCHAR(1000) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_odometer_active (archived_at,updated_at),
  CONSTRAINT fk_transport_odometer_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_odometer_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_documents (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  document_type VARCHAR(80) NULL,
  reference_number VARCHAR(180) NULL,
  issue_date DATE NULL,
  expiry_date DATE NULL,
  file_id BIGINT NULL,
  original_file_id BIGINT NULL,
  original_file_name VARCHAR(180) NULL,
  previous_document_id BIGINT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  notes TEXT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_documents_active (archived_at,updated_at),
  CONSTRAINT fk_transport_documents_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_documents_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_documents_file FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE SET NULL,
  CONSTRAINT fk_transport_documents_previousDocument FOREIGN KEY (previous_document_id) REFERENCES transport_documents(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_components (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  name VARCHAR(120) NULL,
  category VARCHAR(100) NULL,
  last_service_date DATE NULL,
  last_service_mileage DECIMAL(19,3) NULL,
  interval_km DECIMAL(19,3) NULL,
  interval_days INT NULL,
  interval_months INT NULL,
  next_service_mileage DECIMAL(19,3) NULL,
  next_service_date DATE NULL,
  replacement_date DATE NULL,
  replacement_cost DECIMAL(19,2) NULL,
  supplier VARCHAR(180) NULL,
  technician VARCHAR(180) NULL,
  notes TEXT NULL,
  checklist TEXT NULL,
  warning_km DECIMAL(19,3) NULL,
  warning_days INT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_components_active (archived_at,updated_at),
  CONSTRAINT fk_transport_components_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_components_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_faults (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  reference VARCHAR(90) NULL,
  category VARCHAR(120) NULL,
  description TEXT NULL,
  severity VARCHAR(20) NULL DEFAULT 'MEDIUM',
  status VARCHAR(40) NULL DEFAULT 'SUBMITTED',
  location VARCHAR(255) NULL,
  odometer DECIMAL(19,3) NULL,
  operational BOOLEAN NOT NULL DEFAULT TRUE,
  review_notes TEXT NULL,
  reviewed_by_id BIGINT NULL,
  reviewed_at TIMESTAMP(6) NULL,
  request_key VARCHAR(100) NULL,
  request_hash VARCHAR(64) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_faults_active (archived_at,updated_at),
  CONSTRAINT fk_transport_faults_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_faults_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_faults_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
  CONSTRAINT fk_transport_faults_reviewedBy FOREIGN KEY (reviewed_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_faults_requestkey (request_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_work_orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  fault_id BIGINT NULL,
  component_id BIGINT NULL,
  reference VARCHAR(90) NULL,
  maintenance_type VARCHAR(30) NULL DEFAULT 'PREVENTIVE',
  title VARCHAR(180) NULL,
  component_name_at_completion VARCHAR(180) NULL,
  replacement BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(40) NULL DEFAULT 'SCHEDULED',
  scheduled_date DATE NULL,
  completed_date DATE NULL,
  completion_mileage DECIMAL(19,3) NULL,
  parts_cost DECIMAL(19,2) NULL,
  labour_cost DECIMAL(19,2) NULL,
  supplier VARCHAR(180) NULL,
  technician VARCHAR(180) NULL,
  checklist TEXT NULL,
  completion_notes TEXT NULL,
  verified_by_id BIGINT NULL,
  verified_at TIMESTAMP(6) NULL,
  started_at TIMESTAMP(6) NULL,
  completed_at TIMESTAMP(6) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_work_orders_active (archived_at,updated_at),
  CONSTRAINT fk_transport_work_orders_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_work_orders_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_work_orders_fault FOREIGN KEY (fault_id) REFERENCES transport_faults(id),
  CONSTRAINT fk_transport_work_orders_component FOREIGN KEY (component_id) REFERENCES transport_components(id),
  CONSTRAINT fk_transport_work_orders_verifiedBy FOREIGN KEY (verified_by_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_inspection_templates (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  name VARCHAR(180) NULL,
  frequency VARCHAR(30) NULL DEFAULT 'DAILY',
  interval_days INT NULL,
  description TEXT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  INDEX idx_transport_inspection_templates_active (archived_at,updated_at),
  CONSTRAINT fk_transport_inspection_templates_creator FOREIGN KEY (created_by_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_inspection_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  template_id BIGINT NOT NULL,
  label VARCHAR(255) NULL,
  position INT NOT NULL,
  safety_critical BOOLEAN NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_inspection_items_active (archived_at,updated_at),
  CONSTRAINT fk_transport_inspection_items_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_inspection_items_template FOREIGN KEY (template_id) REFERENCES transport_inspection_templates(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_inspection_assignments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  next_due DATE NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  INDEX idx_transport_inspection_assignments_active (archived_at,updated_at),
  CONSTRAINT fk_transport_inspection_assignments_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_inspection_assignments_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_inspection_assignments_template FOREIGN KEY (template_id) REFERENCES transport_inspection_templates(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_inspections (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  assignment_id BIGINT NULL,
  template_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  inspection_date DATE NULL,
  outcome VARCHAR(30) NULL,
  findings TEXT NULL,
  fault_id BIGINT NULL,
  request_key VARCHAR(100) NULL,
  request_hash VARCHAR(64) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_inspections_active (archived_at,updated_at),
  CONSTRAINT fk_transport_inspections_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_inspections_assignment FOREIGN KEY (assignment_id) REFERENCES transport_inspection_assignments(id),
  CONSTRAINT fk_transport_inspections_template FOREIGN KEY (template_id) REFERENCES transport_inspection_templates(id),
  CONSTRAINT fk_transport_inspections_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_inspections_inspector FOREIGN KEY (inspector_id) REFERENCES users(id),
  CONSTRAINT fk_transport_inspections_fault FOREIGN KEY (fault_id) REFERENCES transport_faults(id),
  UNIQUE KEY uk_transport_inspections_requestkey (request_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_inspection_results (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  inspection_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  label VARCHAR(255) NULL,
  outcome VARCHAR(30) NULL,
  notes TEXT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_inspection_results_active (archived_at,updated_at),
  CONSTRAINT fk_transport_inspection_results_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_inspection_results_inspection FOREIGN KEY (inspection_id) REFERENCES transport_inspections(id),
  CONSTRAINT fk_transport_inspection_results_item FOREIGN KEY (item_id) REFERENCES transport_inspection_items(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_procurement (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  fault_id BIGINT NULL,
  work_order_id BIGINT NULL,
  requested_by_id BIGINT NOT NULL,
  assigned_to_id BIGINT NULL,
  approved_by_id BIGINT NULL,
  reference VARCHAR(90) NULL,
  status VARCHAR(40) NULL DEFAULT 'DRAFT',
  priority VARCHAR(20) NULL DEFAULT 'MEDIUM',
  request_date DATE NULL,
  required_date DATE NULL,
  justification TEXT NULL,
  review_notes TEXT NULL,
  approved_at TIMESTAMP(6) NULL,
  received_at TIMESTAMP(6) NULL,
  purchase_reference VARCHAR(180) NULL,
  supplier VARCHAR(180) NULL,
  request_key VARCHAR(100) NULL,
  request_hash VARCHAR(64) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_procurement_active (archived_at,updated_at),
  CONSTRAINT fk_transport_procurement_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_procurement_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_procurement_fault FOREIGN KEY (fault_id) REFERENCES transport_faults(id),
  CONSTRAINT fk_transport_procurement_workOrder FOREIGN KEY (work_order_id) REFERENCES transport_work_orders(id),
  CONSTRAINT fk_transport_procurement_requestedBy FOREIGN KEY (requested_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_procurement_assignedTo FOREIGN KEY (assigned_to_id) REFERENCES users(id),
  CONSTRAINT fk_transport_procurement_approvedBy FOREIGN KEY (approved_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_procurement_requestkey (request_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_procurement_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  request_id BIGINT NOT NULL,
  name VARCHAR(180) NULL,
  unit VARCHAR(40) NULL,
  quantity DECIMAL(19,3) NULL,
  estimated_unit_cost DECIMAL(19,2) NULL,
  received_quantity DECIMAL(19,3) NULL,
  actual_unit_cost DECIMAL(19,2) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_procurement_items_active (archived_at,updated_at),
  CONSTRAINT fk_transport_procurement_items_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_procurement_items_request FOREIGN KEY (request_id) REFERENCES transport_procurement(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_assets (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NOT NULL,
  file_id BIGINT NULL,
  original_file_id BIGINT NULL,
  fuel_id BIGINT NULL,
  fault_id BIGINT NULL,
  work_order_id BIGINT NULL,
  inspection_id BIGINT NULL,
  procurement_id BIGINT NULL,
  request_key VARCHAR(120) UNIQUE,
  request_hash VARCHAR(64) NULL,
  purpose VARCHAR(30) NULL,
  name VARCHAR(180) NULL,
  media_type VARCHAR(120) NULL,
  size_bytes BIGINT NOT NULL,
  removed BOOLEAN NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_assets_active (archived_at,updated_at),
  CONSTRAINT fk_transport_assets_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_assets_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_assets_file FOREIGN KEY (file_id) REFERENCES file_items(id) ON DELETE SET NULL,
  CONSTRAINT fk_transport_assets_fuel FOREIGN KEY (fuel_id) REFERENCES fuel(id),
  CONSTRAINT fk_transport_assets_fault FOREIGN KEY (fault_id) REFERENCES transport_faults(id),
  CONSTRAINT fk_transport_assets_workOrder FOREIGN KEY (work_order_id) REFERENCES transport_work_orders(id),
  CONSTRAINT fk_transport_assets_inspection FOREIGN KEY (inspection_id) REFERENCES transport_inspections(id),
  CONSTRAINT fk_transport_assets_procurement FOREIGN KEY (procurement_id) REFERENCES transport_procurement(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_fuel_details (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  fuel_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  driver_id BIGINT NULL,
  price_per_litre DECIMAL(19,2) NULL,
  fuel_type VARCHAR(40) NULL,
  station VARCHAR(180) NULL,
  payment_method VARCHAR(60) NULL,
  transaction_reference VARCHAR(180) NULL,
  remarks TEXT NULL,
  receipt_id BIGINT NULL,
  odometer DECIMAL(19,3) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_fuel_details_active (archived_at,updated_at),
  CONSTRAINT fk_transport_fuel_details_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_fuel_details_fuel (fuel_id),
  CONSTRAINT fk_transport_fuel_details_fuel FOREIGN KEY (fuel_id) REFERENCES fuel(id),
  CONSTRAINT fk_transport_fuel_details_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_fuel_details_driver FOREIGN KEY (driver_id) REFERENCES drivers(id),
  CONSTRAINT fk_transport_fuel_details_receipt FOREIGN KEY (receipt_id) REFERENCES transport_assets(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_maintenance_details (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  maintenance_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  work_order_id BIGINT NULL,
  component_id BIGINT NULL,
  service_mileage DECIMAL(19,3) NULL,
  notes TEXT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_maintenance_details_active (archived_at,updated_at),
  CONSTRAINT fk_transport_maintenance_details_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_maintenance_details_maintenance (maintenance_id),
  CONSTRAINT fk_transport_maintenance_details_maintenance FOREIGN KEY (maintenance_id) REFERENCES maintenance(id),
  CONSTRAINT fk_transport_maintenance_details_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_maintenance_details_workOrder FOREIGN KEY (work_order_id) REFERENCES transport_work_orders(id),
  CONSTRAINT fk_transport_maintenance_details_component FOREIGN KEY (component_id) REFERENCES transport_components(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_timeline (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  vehicle_id BIGINT NULL,
  actor_id BIGINT NULL,
  event_type VARCHAR(70) NULL,
  description TEXT NULL,
  reference VARCHAR(120) NULL,
  details TEXT NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_timeline_active (archived_at,updated_at),
  CONSTRAINT fk_transport_timeline_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_timeline_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  CONSTRAINT fk_transport_timeline_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_notifications (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  recipient_id BIGINT NULL,
  email VARCHAR(254) NULL,
  event_type VARCHAR(70) NULL,
  priority VARCHAR(20) NULL DEFAULT 'NORMAL',
  title VARCHAR(255) NULL,
  body TEXT NULL,
  target_path VARCHAR(500) NULL,
  vehicle_id BIGINT NULL,
  reference VARCHAR(120) NULL,
  read_at TIMESTAMP(6) NULL,
  in_app BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL,
  delivery_status VARCHAR(30) NULL DEFAULT 'PENDING',
  attempts INT NOT NULL,
  next_attempt_at TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
  lease_until TIMESTAMP(6) NULL,
  delivered_at TIMESTAMP(6) NULL,
  last_error VARCHAR(500) NULL,
  dedupe_key VARCHAR(180) NULL,
  PRIMARY KEY (id),
  INDEX idx_transport_notifications_active (archived_at,updated_at),
  CONSTRAINT fk_transport_notifications_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  CONSTRAINT fk_transport_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(id),
  CONSTRAINT fk_transport_notifications_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
  UNIQUE KEY uk_transport_notifications_dedupekey (dedupe_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transport_policy (
  id BIGINT NOT NULL AUTO_INCREMENT,
  revision BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  created_by_id BIGINT NULL,
  archived_at TIMESTAMP(6) NULL,
  scope VARCHAR(30) NULL DEFAULT 'GLOBAL',
  company_name VARCHAR(180) NULL DEFAULT 'EAC Electrical Solutions Limited',
  currency VARCHAR(10) NULL DEFAULT 'GHS',
  timezone VARCHAR(60) NULL DEFAULT 'Africa/Accra',
  distance_unit VARCHAR(10) NULL DEFAULT 'km',
  admin_emails TEXT NULL,
  fault_emails TEXT NULL,
  procurement_emails TEXT NULL,
  supervisor_emails TEXT NULL,
  escalation_emails TEXT NULL,
  dvla_thresholds VARCHAR(100) NULL DEFAULT '30,14,7,0',
  insurance_thresholds VARCHAR(100) NULL DEFAULT '30,14,7,0',
  roadworthy_thresholds VARCHAR(100) NULL DEFAULT '30,14,7,0',
  license_thresholds VARCHAR(100) NULL DEFAULT '30,14,7,0',
  maintenance_days VARCHAR(100) NULL DEFAULT '30,7,0',
  maintenance_km VARCHAR(100) NULL DEFAULT '1000,500,0',
  vehicle_categories TEXT NULL DEFAULT ('Car,Van,Pickup,Truck,Bus,Heavy equipment'),
  document_types TEXT NULL DEFAULT ('DVLA,INSURANCE,ROADWORTHY,INSPECTION,OTHER'),
  component_categories TEXT NULL DEFAULT ('Engine,Filters,Brakes,Tyres,Battery,Transmission,Cooling,Electrical,Other'),
  procurement_categories TEXT NULL DEFAULT ('Parts,Consumables,Labour,Other'),
  reminder_days INT NOT NULL DEFAULT 1,
  forecast_min_readings INT NOT NULL DEFAULT 2,
  forecast_max_age_days INT NOT NULL DEFAULT 60,
  email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  notify_driver BOOLEAN NOT NULL DEFAULT TRUE,
  notify_admin BOOLEAN NOT NULL DEFAULT TRUE,
  critical_escalation BOOLEAN NOT NULL DEFAULT TRUE,
  allow_multiple_vehicles_per_driver BOOLEAN NOT NULL DEFAULT TRUE,
  driver_fuel_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  driver_mileage_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  INDEX idx_transport_policy_active (archived_at,updated_at),
  CONSTRAINT fk_transport_policy_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
  UNIQUE KEY uk_transport_policy_scope (scope)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS eac_transport_add_archive;
DELIMITER $$
CREATE PROCEDURE eac_transport_add_archive(IN target_table VARCHAR(64))
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=target_table AND column_name='archived_at') THEN
  SET @transport_ddl=CONCAT('ALTER TABLE ',target_table,' ADD COLUMN archived_at TIMESTAMP(6) NULL');
  PREPARE transport_statement FROM @transport_ddl; EXECUTE transport_statement; DEALLOCATE PREPARE transport_statement;
 END IF;
END$$
DELIMITER ;
CALL eac_transport_add_archive('vehicles');
CALL eac_transport_add_archive('drivers');
CALL eac_transport_add_archive('fuel');
CALL eac_transport_add_archive('maintenance');
DROP PROCEDURE eac_transport_add_archive;

CREATE TABLE IF NOT EXISTS transport_part_receipts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 revision BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 created_by_id BIGINT NULL,
 archived_at TIMESTAMP(6) NULL,
 request_id BIGINT NOT NULL,
 item_id BIGINT NOT NULL,
 received_by_id BIGINT NOT NULL,
 quantity DECIMAL(19,3) NOT NULL,
 unit_cost DECIMAL(19,2) NOT NULL,
 received_date DATE NOT NULL,
 reference VARCHAR(180) NULL,
 request_key VARCHAR(100) UNIQUE,
 request_hash VARCHAR(64) NULL,
 batch_key VARCHAR(100) NULL,
 INDEX idx_transport_receipt_batch (batch_key),
 INDEX idx_transport_receipt_date (received_date),
 CONSTRAINT fk_transport_receipt_creator FOREIGN KEY (created_by_id) REFERENCES users(id),
 CONSTRAINT fk_transport_receipt_request FOREIGN KEY (request_id) REFERENCES transport_procurement(id),
 CONSTRAINT fk_transport_receipt_item FOREIGN KEY (item_id) REFERENCES transport_procurement_items(id),
 CONSTRAINT fk_transport_receipt_user FOREIGN KEY (received_by_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Legacy attachment metadata is retained; new content is stored in File Management.
DROP PROCEDURE IF EXISTS eac_transport_add_column;
DELIMITER $$
CREATE PROCEDURE eac_transport_add_column(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN column_definition VARCHAR(120))
BEGIN
 IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=target_table AND column_name=target_column) THEN
  SET @transport_ddl=CONCAT('ALTER TABLE `',target_table,'` ADD COLUMN `',target_column,'` ',column_definition);
  PREPARE transport_statement FROM @transport_ddl; EXECUTE transport_statement; DEALLOCATE PREPARE transport_statement;
 END IF;
END$$
DELIMITER ;
CALL eac_transport_add_column('transport_odometer','superseded_at','TIMESTAMP(6) NULL');
CALL eac_transport_add_column('transport_odometer','superseded_reason','TEXT NULL');
CALL eac_transport_add_column('attachments','archived_at','TIMESTAMP(6) NULL');
CALL eac_transport_add_column('attachments','managed_file_id','BIGINT NULL');
CALL eac_transport_add_column('transport_assets','original_file_id','BIGINT NULL');
CALL eac_transport_add_column('transport_assets','request_key','VARCHAR(120) NULL');
CALL eac_transport_add_column('transport_assets','request_hash','VARCHAR(64) NULL');
CALL eac_transport_add_column('transport_documents','original_file_id','BIGINT NULL');
CALL eac_transport_add_column('transport_documents','original_file_name','VARCHAR(180) NULL');
CALL eac_transport_add_column('transport_driver_profiles','managed_account','BOOLEAN NOT NULL DEFAULT FALSE');
CALL eac_transport_add_column('transport_driver_profiles','last_invitation_at','TIMESTAMP(6) NULL');
CALL eac_transport_add_column('transport_policy','allow_multiple_vehicles_per_driver','BOOLEAN NOT NULL DEFAULT TRUE');
CALL eac_transport_add_column('transport_work_orders','component_name_at_completion','VARCHAR(180) NULL');
CALL eac_transport_add_column('transport_work_orders','replacement','BOOLEAN NOT NULL DEFAULT FALSE');
DROP PROCEDURE eac_transport_add_column;

-- Keep domain history when a file owner intentionally purges a managed file.
-- Module links become null; no vehicle, document, fuel or audit record is deleted.
DROP PROCEDURE IF EXISTS eac_transport_file_reference;
DELIMITER $$
CREATE PROCEDURE eac_transport_file_reference(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN foreign_key_name VARCHAR(64))
BEGIN
 DECLARE existing_key VARCHAR(64);
 SELECT MIN(constraint_name) INTO existing_key FROM information_schema.key_column_usage
 WHERE table_schema=DATABASE() AND table_name=target_table AND column_name=target_column AND referenced_table_name='file_items';
 WHILE existing_key IS NOT NULL DO
  SET @transport_ddl=CONCAT('ALTER TABLE `',target_table,'` DROP FOREIGN KEY `',existing_key,'`');
  PREPARE transport_statement FROM @transport_ddl; EXECUTE transport_statement; DEALLOCATE PREPARE transport_statement;
  SELECT MIN(constraint_name) INTO existing_key FROM information_schema.key_column_usage
  WHERE table_schema=DATABASE() AND table_name=target_table AND column_name=target_column AND referenced_table_name='file_items';
 END WHILE;
 SET @transport_ddl=CONCAT('ALTER TABLE `',target_table,'` MODIFY COLUMN `',target_column,'` BIGINT NULL');
 PREPARE transport_statement FROM @transport_ddl; EXECUTE transport_statement; DEALLOCATE PREPARE transport_statement;
 SET @transport_ddl=CONCAT('ALTER TABLE `',target_table,'` ADD CONSTRAINT `',foreign_key_name,'` FOREIGN KEY (`',target_column,'`) REFERENCES file_items(id) ON DELETE SET NULL');
 PREPARE transport_statement FROM @transport_ddl; EXECUTE transport_statement; DEALLOCATE PREPARE transport_statement;
END$$
DELIMITER ;
CALL eac_transport_file_reference('transport_assets','file_id','fk_transport_asset_file');
CALL eac_transport_file_reference('transport_documents','file_id','fk_transport_document_file');
CALL eac_transport_file_reference('transport_vehicle_profiles','primary_image_id','fk_transport_primary_image');
CALL eac_transport_file_reference('attachments','managed_file_id','fk_transport_legacy_managed_file');
DROP PROCEDURE eac_transport_file_reference;

UPDATE transport_assets SET original_file_id=file_id WHERE original_file_id IS NULL AND file_id IS NOT NULL;
UPDATE transport_documents d JOIN file_items f ON f.id=d.file_id SET d.original_file_id=f.id,d.original_file_name=f.name WHERE d.original_file_id IS NULL;
