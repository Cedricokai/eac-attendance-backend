CREATE TABLE inventory_procurement_proposals (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, inventory_request_id BIGINT NOT NULL, reference VARCHAR(255) NOT NULL UNIQUE,
 supplier VARCHAR(255) NOT NULL, quotation_reference VARCHAR(500) NOT NULL, notes VARCHAR(800), total_amount DECIMAL(19,2) NOT NULL,
 status VARCHAR(255) NOT NULL, submitted_by_user_id BIGINT, submitted_by VARCHAR(255), submitted_at DATETIME(6),
 reviewed_by_user_id BIGINT, reviewed_by VARCHAR(255), reviewed_at DATETIME(6), review_notes VARCHAR(800), funding_route VARCHAR(255), finance_request_id BIGINT, finance_reference VARCHAR(255)
);
CREATE TABLE inventory_proposal_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, proposal_id BIGINT NOT NULL, request_item_id BIGINT NOT NULL, product_name VARCHAR(255), quantity INT NOT NULL, unit_cost DECIMAL(19,2) NOT NULL,
 CONSTRAINT fk_inventory_proposal FOREIGN KEY (proposal_id) REFERENCES inventory_procurement_proposals(id)
);
