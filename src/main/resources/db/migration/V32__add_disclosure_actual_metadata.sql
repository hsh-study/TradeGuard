ALTER TABLE catalyst_evidences ADD COLUMN receipt_no VARCHAR(40);
ALTER TABLE catalyst_evidences ADD COLUMN disclosure_type VARCHAR(100);
ALTER TABLE catalyst_evidences ADD COLUMN related_catalyst_type VARCHAR(40);
ALTER TABLE catalyst_evidences ADD COLUMN importance VARCHAR(20);
ALTER TABLE catalyst_evidences ADD COLUMN raw_category VARCHAR(100);

CREATE UNIQUE INDEX uk_catalyst_evidence_stock_receipt
    ON catalyst_evidences (stock_code, receipt_no);

CREATE INDEX idx_catalyst_evidence_importance_published
    ON catalyst_evidences (importance, source_published_at);
