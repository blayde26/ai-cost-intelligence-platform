ALTER TABLE ai_usage_events ADD COLUMN attribution_source VARCHAR(40) NOT NULL DEFAULT 'EXPLICIT';
ALTER TABLE ai_usage_events ADD COLUMN attribution_confidence VARCHAR(40) NOT NULL DEFAULT 'HIGH';
ALTER TABLE ai_usage_events ADD COLUMN inferred_story_key VARCHAR(128);
ALTER TABLE ai_usage_events ADD COLUMN inference_reason VARCHAR(500);

UPDATE ai_usage_events
SET attribution_source = 'MISSING',
    attribution_confidence = 'LOW',
    inference_reason = 'No explicit story key or source metadata was provided.'
WHERE story_key IS NULL;

UPDATE ai_usage_events
SET attribution_source = 'MANUAL',
    attribution_confidence = 'HIGH',
    inference_reason = 'Manual correction was previously applied.'
WHERE attribution_corrected = TRUE;

CREATE INDEX idx_ai_usage_events_attribution_source ON ai_usage_events (attribution_source);
