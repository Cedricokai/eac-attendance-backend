-- For deployments using schema migrations. Local development uses Hibernate ddl-auto=update.
ALTER TABLE inventory_request MODIFY COLUMN status VARCHAR(50);
ALTER TABLE inventory_request ADD COLUMN requester_user_id BIGINT NULL;
ALTER TABLE inventory_request ADD COLUMN requester_email VARCHAR(255) NULL;
ALTER TABLE inventory_request ADD COLUMN requester_name VARCHAR(255) NULL;
ALTER TABLE inventory_request ADD COLUMN finance_fulfilment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE petty_cash_requests ADD COLUMN inventory_request_id BIGINT NULL;
ALTER TABLE petty_cash_requests ADD COLUMN inventory_request_number VARCHAR(255) NULL;
ALTER TABLE petty_cash_requests ADD COLUMN created_by_user_id BIGINT NULL;
ALTER TABLE petty_cash_requests ADD COLUMN created_by_name VARCHAR(255) NULL;
ALTER TABLE direct_purchases ADD COLUMN inventory_request_id BIGINT NULL;
ALTER TABLE direct_purchases ADD COLUMN inventory_request_number VARCHAR(255) NULL;
ALTER TABLE direct_purchases ADD COLUMN created_by_user_id BIGINT NULL;
ALTER TABLE direct_purchases ADD COLUMN created_by_name VARCHAR(255) NULL;
ALTER TABLE system_settings ADD COLUMN petty_cash_purchase_limit DECIMAL(38,2) NULL;
CREATE TABLE inventory_purchase_lines (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 inventory_request_id BIGINT, request_item_id BIGINT, product_id BIGINT,
 route VARCHAR(255), finance_request_id BIGINT, finance_item_id BIGINT, finance_reference VARCHAR(255),
 quantity INT NOT NULL, unit_cost DECIMAL(19,2), received_quantity INT NOT NULL DEFAULT 0, issued_quantity INT NOT NULL DEFAULT 0,
 created_by VARCHAR(255), created_at DATETIME(6),
 INDEX idx_ipl_request (inventory_request_id), INDEX idx_ipl_product (product_id)
);
CREATE TABLE inventory_finance_receipts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, purchase_line_id BIGINT, quantity INT NOT NULL,
 evidence_reference VARCHAR(500), received_by VARCHAR(255), received_at DATETIME(6)
);
-- Existing requesters are linked only by a unique login/email, never an ambiguous display name.
UPDATE inventory_request r JOIN users u ON r.requested_by = u.user_name
SET r.requester_user_id=u.id, r.requester_email=u.email, r.requester_name=u.name
WHERE r.requester_user_id IS NULL;

UPDATE inventory_request r JOIN users u ON r.requested_by = u.email
SET r.requester_user_id=u.id, r.requester_email=u.email, r.requester_name=u.name
WHERE r.requester_user_id IS NULL;
