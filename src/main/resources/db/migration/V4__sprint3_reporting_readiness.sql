ALTER TABLE ai_usage_events ADD COLUMN repository VARCHAR(255);
ALTER TABLE ai_usage_events ADD COLUMN branch VARCHAR(255);
ALTER TABLE ai_usage_events ADD COLUMN commit_hash VARCHAR(128);
ALTER TABLE ai_usage_events ADD COLUMN initiative_key VARCHAR(128);
ALTER TABLE ai_usage_events ADD COLUMN initiative_name VARCHAR(500);
ALTER TABLE ai_usage_events ADD COLUMN attribution_corrected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_usage_events ADD COLUMN corrected_timestamp TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_usage_events ADD COLUMN corrected_by VARCHAR(128);

CREATE INDEX idx_ai_usage_events_epic ON ai_usage_events (epic_key);
CREATE INDEX idx_ai_usage_events_team_timestamp ON ai_usage_events (team_key, request_timestamp DESC);
CREATE INDEX idx_ai_usage_events_story_timestamp ON ai_usage_events (story_key, request_timestamp DESC);
CREATE INDEX idx_ai_usage_events_epic_timestamp ON ai_usage_events (epic_key, request_timestamp DESC);
