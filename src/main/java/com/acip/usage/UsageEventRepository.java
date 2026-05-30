package com.acip.usage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UsageEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public UsageEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(UsageEvent event) {
        String sql = """
                INSERT INTO ai_usage_events (
                    id, provider, model, story_key, epic_key, team_key, user_key,
                    prompt_tokens, completion_tokens, total_tokens, estimated_cost_usd,
                    latency_ms, request_timestamp, environment, work_type, request_status, request_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                event.id(),
                event.provider(),
                event.model(),
                event.storyKey(),
                event.epicKey(),
                event.teamKey(),
                event.userKey(),
                event.promptTokens(),
                event.completionTokens(),
                event.totalTokens(),
                event.estimatedCostUsd(),
                event.latencyMs(),
                event.requestTimestamp(),
                event.environment(),
                event.workType(),
                event.requestStatus(),
                event.requestHash()
        );
    }

    public List<UsageEvent> findRecent(int limit) {
        String sql = """
                SELECT id, provider, model, story_key, epic_key, team_key, user_key,
                       prompt_tokens, completion_tokens, total_tokens, estimated_cost_usd,
                       latency_ms, request_timestamp, environment, work_type, request_status, request_hash
                FROM ai_usage_events
                ORDER BY request_timestamp DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapEvent, limit);
    }

    private UsageEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new UsageEvent(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("provider"),
                rs.getString("model"),
                rs.getString("story_key"),
                rs.getString("epic_key"),
                rs.getString("team_key"),
                rs.getString("user_key"),
                rs.getInt("prompt_tokens"),
                rs.getInt("completion_tokens"),
                rs.getInt("total_tokens"),
                rs.getBigDecimal("estimated_cost_usd"),
                rs.getLong("latency_ms"),
                rs.getObject("request_timestamp", java.time.OffsetDateTime.class),
                rs.getString("environment"),
                rs.getString("work_type"),
                rs.getString("request_status"),
                rs.getString("request_hash")
        );
    }
}
