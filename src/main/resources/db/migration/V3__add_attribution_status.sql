ALTER TABLE ai_usage_events ALTER COLUMN story_key DROP NOT NULL;

ALTER TABLE ai_usage_events
    ADD COLUMN attribution_status VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN_STORY';

CREATE INDEX idx_ai_usage_events_attribution_status ON ai_usage_events (attribution_status);
