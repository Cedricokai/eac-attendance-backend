CREATE DATABASE IF NOT EXISTS eac_attendance;

use eac_attendance;

CREATE TABLE overview (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attendance_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL, -- Assuming employee_id is a foreign key
    shift VARCHAR(255),
    date DATE, -- Use DATE instead of VARCHAR for dates
    hours_worked DOUBLE, -- Use DOUBLE for numeric values
    minimum_hour DOUBLE, -- Use DOUBLE for numeric values
    overtime DOUBLE, -- Use DOUBLE for numeric values
    version INT NOT NULL DEFAULT 0-- Add this line for the version column

);

CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    shift VARCHAR(255),
    work_type VARCHAR(255),
    date DATE NOT NULL,
    status VARCHAR(255),
    leave_type VARCHAR(255),
    minimum_hour VARCHAR(255),
    overtime VARCHAR(255),
    version VARCHAR(255),
    total_overtime_hour VARCHAR(255),

    CONSTRAINT fk_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
);

CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255),
    last_name VARCHAR(255),
    first_name VARCHAR(255),
    phone VARCHAR(255),
    job_position VARCHAR(255),
	work_type VARCHAR(255)
);

SELECT * FROM employees WHERE id = 9007199254740991;
