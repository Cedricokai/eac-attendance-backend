-- Saved payroll project breakdowns and timesheet provenance. Historical values stay NULL.
-- Hibernate ddl-auto=update also adds these nullable columns.

SET @project_payroll_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'payroll_record' AND column_name = 'project_earnings_snapshot'), 'SELECT 1', 'ALTER TABLE payroll_record ADD COLUMN project_earnings_snapshot LONGTEXT NULL');
PREPARE project_payroll_statement FROM @project_payroll_ddl;
EXECUTE project_payroll_statement;
DEALLOCATE PREPARE project_payroll_statement;

SET @project_payroll_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'attendance' AND column_name = 'timesheet_id'), 'SELECT 1', 'ALTER TABLE attendance ADD COLUMN timesheet_id BIGINT NULL');
PREPARE project_payroll_statement FROM @project_payroll_ddl;
EXECUTE project_payroll_statement;
DEALLOCATE PREPARE project_payroll_statement;

SET @project_payroll_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'attendance' AND column_name = 'project_id'), 'SELECT 1', 'ALTER TABLE attendance ADD COLUMN project_id BIGINT NULL');
PREPARE project_payroll_statement FROM @project_payroll_ddl;
EXECUTE project_payroll_statement;
DEALLOCATE PREPARE project_payroll_statement;

SET @project_payroll_ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'attendance' AND column_name = 'project_name'), 'SELECT 1', 'ALTER TABLE attendance ADD COLUMN project_name VARCHAR(255) NULL');
PREPARE project_payroll_statement FROM @project_payroll_ddl;
EXECUTE project_payroll_statement;
DEALLOCATE PREPARE project_payroll_statement;
