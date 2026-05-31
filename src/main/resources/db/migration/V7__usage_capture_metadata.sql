ALTER TABLE ai_usage_events ADD COLUMN capture_source VARCHAR(40) NOT NULL DEFAULT 'PROXY';
ALTER TABLE ai_usage_events ADD COLUMN capture_provider VARCHAR(80) NOT NULL DEFAULT 'OPENAI_COMPATIBLE_PROXY';
ALTER TABLE ai_usage_events ADD COLUMN capture_method VARCHAR(40) NOT NULL DEFAULT 'REAL_TIME_PROXY';
ALTER TABLE ai_usage_events ADD COLUMN capture_confidence VARCHAR(40) NOT NULL DEFAULT 'HIGH';

UPDATE ai_usage_events
SET capture_source = 'DEMO_DATA',
    capture_provider = 'DEMO_DATA_LOADER',
    capture_method = 'SEEDED_DEMO',
    capture_confidence = 'HIGH'
WHERE environment = 'demo';

CREATE INDEX idx_ai_usage_events_capture_source ON ai_usage_events (capture_source);
