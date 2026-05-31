package com.acip.usage;

import com.acip.capture.UsageCaptureConfidence;
import com.acip.capture.UsageCaptureMethod;
import com.acip.capture.UsageCaptureSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                    latency_ms, request_timestamp, environment, work_type, request_status, attribution_status, request_hash,
                    capture_source, capture_provider, capture_method, capture_confidence,
                    attribution_source, attribution_confidence, inferred_story_key, inference_reason,
                    repository, branch, commit_hash, initiative_key, initiative_name,
                    attribution_corrected, corrected_timestamp, corrected_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                event.attributionStatus().name(),
                event.requestHash(),
                event.captureSource().name(),
                event.captureProvider(),
                event.captureMethod().name(),
                event.captureConfidence().name(),
                event.attributionSource().name(),
                event.attributionConfidence().name(),
                event.inferredStoryKey(),
                event.inferenceReason(),
                event.repository(),
                event.branch(),
                event.commitHash(),
                event.initiativeKey(),
                event.initiativeName(),
                event.attributionCorrected(),
                event.correctedTimestamp(),
                event.correctedBy()
        );
    }

    public List<UsageEvent> findRecent(int limit) {
        String sql = """
                SELECT id, provider, model, story_key, epic_key, team_key, user_key,
                       prompt_tokens, completion_tokens, total_tokens, estimated_cost_usd,
                       latency_ms, request_timestamp, environment, work_type, request_status, attribution_status, request_hash,
                       capture_source, capture_provider, capture_method, capture_confidence,
                       attribution_source, attribution_confidence, inferred_story_key, inference_reason,
                       repository, branch, commit_hash, initiative_key, initiative_name,
                       attribution_corrected, corrected_timestamp, corrected_by
                FROM ai_usage_events
                ORDER BY request_timestamp DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapEvent, limit);
    }

    public Optional<UsageEvent> findById(UUID id) {
        String sql = """
                SELECT id, provider, model, story_key, epic_key, team_key, user_key,
                       prompt_tokens, completion_tokens, total_tokens, estimated_cost_usd,
                       latency_ms, request_timestamp, environment, work_type, request_status, attribution_status, request_hash,
                       capture_source, capture_provider, capture_method, capture_confidence,
                       attribution_source, attribution_confidence, inferred_story_key, inference_reason,
                       repository, branch, commit_hash, initiative_key, initiative_name,
                       attribution_corrected, corrected_timestamp, corrected_by
                FROM ai_usage_events
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, this::mapEvent, id)
                .stream()
                .findFirst();
    }

    public void applyCorrection(UsageEvent originalEvent, AttributionCorrection correction) {
        jdbcTemplate.update("""
                        INSERT INTO usage_event_attribution_corrections (
                            id, usage_event_id,
                            original_story_key, original_epic_key, original_team_key,
                            original_work_type, original_attribution_status,
                            corrected_story_key, corrected_epic_key, corrected_team_key,
                            corrected_work_type, corrected_attribution_status,
                            corrected_by, corrected_timestamp, note
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                originalEvent.id(),
                originalEvent.storyKey(),
                originalEvent.epicKey(),
                originalEvent.teamKey(),
                originalEvent.workType(),
                originalEvent.attributionStatus().name(),
                correction.storyKey(),
                correction.epicKey(),
                correction.teamKey(),
                correction.workType(),
                correction.attributionStatus().name(),
                correction.correctedBy(),
                correction.correctedTimestamp(),
                correction.note()
        );
        int updated = jdbcTemplate.update("""
                        UPDATE ai_usage_events
                        SET story_key = ?,
                            epic_key = ?,
                            team_key = ?,
                            work_type = ?,
                            attribution_status = ?,
                            attribution_source = 'MANUAL',
                            attribution_confidence = 'HIGH',
                            inference_reason = ?,
                            attribution_corrected = TRUE,
                            corrected_timestamp = ?,
                            corrected_by = ?
                        WHERE id = ?
                        """,
                correction.storyKey(),
                correction.epicKey(),
                correction.teamKey(),
                correction.workType(),
                correction.attributionStatus().name(),
                "Manual correction by " + correction.correctedBy() + ".",
                correction.correctedTimestamp(),
                correction.correctedBy(),
                originalEvent.id()
        );
        if (updated != 1) {
            throw new IllegalStateException("Expected to update exactly one usage event.");
        }
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
                AttributionStatus.valueOf(rs.getString("attribution_status")),
                rs.getString("request_hash"),
                UsageCaptureSource.valueOf(rs.getString("capture_source")),
                rs.getString("capture_provider"),
                UsageCaptureMethod.valueOf(rs.getString("capture_method")),
                UsageCaptureConfidence.valueOf(rs.getString("capture_confidence")),
                AttributionSource.valueOf(rs.getString("attribution_source")),
                AttributionConfidence.valueOf(rs.getString("attribution_confidence")),
                rs.getString("inferred_story_key"),
                rs.getString("inference_reason"),
                rs.getString("repository"),
                rs.getString("branch"),
                rs.getString("commit_hash"),
                rs.getString("initiative_key"),
                rs.getString("initiative_name"),
                rs.getBoolean("attribution_corrected"),
                rs.getObject("corrected_timestamp", java.time.OffsetDateTime.class),
                rs.getString("corrected_by")
        );
    }
}
