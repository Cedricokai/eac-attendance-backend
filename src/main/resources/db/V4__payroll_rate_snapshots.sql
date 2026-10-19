-- Add nullable rate snapshots; historical rates remain unknown.
-- Safe to run when Hibernate has already added some or all columns.

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'rate_snapshot_saved'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN rate_snapshot_saved BIT(1) NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'applied_minimum_rate'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN applied_minimum_rate DOUBLE NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'applied_hourly_rate'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN applied_hourly_rate DOUBLE NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'applied_employee_rate'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN applied_employee_rate DOUBLE NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'applied_monthly_salary'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN applied_monthly_salary DOUBLE NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;

SET @payroll_rate_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'applied_overtime_rate'),
  'SELECT 1',
  'ALTER TABLE payroll_record ADD COLUMN applied_overtime_rate DOUBLE NULL'
);
PREPARE payroll_rate_statement FROM @payroll_rate_ddl;
EXECUTE payroll_rate_statement;
DEALLOCATE PREPARE payroll_rate_statement;
