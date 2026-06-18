ALTER TABLE incidents
    DROP CONSTRAINT IF EXISTS chk_incident_status;

ALTER TABLE incidents
    ADD CONSTRAINT chk_incident_status
    CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED'));
