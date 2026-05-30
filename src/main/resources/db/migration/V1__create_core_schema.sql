CREATE TABLE teams (
    id UUID PRIMARY KEY,
    team_key VARCHAR(128) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    user_key VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL
);

CREATE TABLE epics (
    id UUID PRIMARY KEY,
    epic_key VARCHAR(128) NOT NULL UNIQUE,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(80) NOT NULL,
    team_key VARCHAR(128)
);

CREATE TABLE stories (
    id UUID PRIMARY KEY,
    story_key VARCHAR(128) NOT NULL UNIQUE,
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(80) NOT NULL,
    epic_key VARCHAR(128),
    work_type VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN'
);

CREATE TABLE provider_pricing (
    provider VARCHAR(80) NOT NULL,
    model VARCHAR(160) NOT NULL,
    input_cost_per_million NUMERIC(12, 6) NOT NULL,
    output_cost_per_million NUMERIC(12, 6) NOT NULL,
    effective_date DATE NOT NULL,
    PRIMARY KEY (provider, model, effective_date)
);

CREATE TABLE ai_usage_events (
    id UUID PRIMARY KEY,
    provider VARCHAR(80) NOT NULL,
    model VARCHAR(160) NOT NULL,
    story_key VARCHAR(128) NOT NULL,
    epic_key VARCHAR(128),
    team_key VARCHAR(128) NOT NULL,
    user_key VARCHAR(128) NOT NULL,
    prompt_tokens INTEGER NOT NULL DEFAULT 0,
    completion_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    estimated_cost_usd NUMERIC(18, 8) NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL,
    request_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    environment VARCHAR(80) NOT NULL DEFAULT 'local',
    work_type VARCHAR(40) NOT NULL DEFAULT 'UNKNOWN',
    request_status VARCHAR(40) NOT NULL,
    request_hash VARCHAR(64) NOT NULL
);

CREATE INDEX idx_ai_usage_events_timestamp ON ai_usage_events (request_timestamp DESC);
CREATE INDEX idx_ai_usage_events_story ON ai_usage_events (story_key);
CREATE INDEX idx_ai_usage_events_team ON ai_usage_events (team_key);
CREATE INDEX idx_ai_usage_events_request_hash ON ai_usage_events (request_hash);
