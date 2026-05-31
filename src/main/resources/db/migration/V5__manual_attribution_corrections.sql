CREATE TABLE usage_event_attribution_corrections (
    id UUID PRIMARY KEY,
    usage_event_id UUID NOT NULL,
    original_story_key VARCHAR(128),
    original_epic_key VARCHAR(128),
    original_team_key VARCHAR(128),
    original_work_type VARCHAR(40),
    original_attribution_status VARCHAR(40) NOT NULL,
    corrected_story_key VARCHAR(128),
    corrected_epic_key VARCHAR(128),
    corrected_team_key VARCHAR(128),
    corrected_work_type VARCHAR(40),
    corrected_attribution_status VARCHAR(40) NOT NULL,
    corrected_by VARCHAR(128) NOT NULL,
    corrected_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    note VARCHAR(1000)
);

CREATE INDEX idx_usage_event_corrections_event_id ON usage_event_attribution_corrections (usage_event_id);
CREATE INDEX idx_usage_event_corrections_timestamp ON usage_event_attribution_corrections (corrected_timestamp DESC);
