package com.acip.demo;

import com.acip.usage.AttributionStatus;
import com.acip.worktracking.WorkItem;
import com.acip.worktracking.WorkTrackingProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "acip.demo-data", name = "enabled", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {

    private final DemoDataProperties properties;
    private final WorkTrackingProvider workTrackingProvider;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataLoader(DemoDataProperties properties, WorkTrackingProvider workTrackingProvider, JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.workTrackingProvider = workTrackingProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        seedTeams();
        seedUsers();
        seedWorkItems();
        if (eventCount() == 0) {
            seedUsageEvents(Math.max(2000, properties.usageEventCount()));
        }
    }

    private void seedTeams() {
        upsertTeam("payments", "Payments");
        upsertTeam("customer-experience", "Customer Experience");
        upsertTeam("platform", "Platform");
    }

    private void upsertTeam(String teamKey, String name) {
        int updated = jdbcTemplate.update("UPDATE teams SET name = ? WHERE team_key = ?", name, teamKey);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO teams (id, team_key, name) VALUES (?, ?, ?)", UUID.randomUUID(), teamKey, name);
        }
    }

    private void seedUsers() {
        upsertUser("brian", "Brian");
        upsertUser("alex", "Alex Rivera");
        upsertUser("maya", "Maya Chen");
        upsertUser("sam", "Sam Patel");
        upsertUser("jordan", "Jordan Lee");
    }

    private void upsertUser(String userKey, String displayName) {
        int updated = jdbcTemplate.update("UPDATE users SET display_name = ? WHERE user_key = ?", displayName, userKey);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO users (id, user_key, display_name) VALUES (?, ?, ?)", UUID.randomUUID(), userKey, displayName);
        }
    }

    private void seedWorkItems() {
        for (WorkItem epic : workTrackingProvider.fetchEpics()) {
            int updated = jdbcTemplate.update(
                    "UPDATE epics SET summary = ?, status = ?, team_key = ? WHERE epic_key = ?",
                    epic.summary(), epic.status(), epic.teamKey(), epic.key()
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO epics (id, epic_key, summary, status, team_key) VALUES (?, ?, ?, ?, ?)",
                        UUID.randomUUID(), epic.key(), epic.summary(), epic.status(), epic.teamKey()
                );
            }
        }
        for (WorkItem story : workTrackingProvider.fetchStories()) {
            int updated = jdbcTemplate.update(
                    "UPDATE stories SET summary = ?, status = ?, epic_key = ?, work_type = ? WHERE story_key = ?",
                    story.summary(), story.status(), story.epicKey(), story.workType(), story.key()
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "INSERT INTO stories (id, story_key, summary, status, epic_key, work_type) VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(), story.key(), story.summary(), story.status(), story.epicKey(), story.workType()
                );
            }
        }
    }

    private long eventCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_usage_events", Long.class);
        return count == null ? 0 : count;
    }

    private void seedUsageEvents(int count) {
        List<WorkItem> stories = workTrackingProvider.fetchStories();
        String[] users = {"brian", "alex", "maya", "sam", "jordan"};
        OffsetDateTime start = OffsetDateTime.of(2026, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        for (int i = 0; i < count; i++) {
            AttributionStatus status = statusFor(i);
            WorkItem story = stories.get(i % stories.size());
            String storyKey = switch (status) {
                case MISSING_STORY_KEY -> null;
                case UNKNOWN_STORY -> "UNKNOWN-" + (900 + (i % 50));
                default -> story.key();
            };
            String epicKey = status == AttributionStatus.UNKNOWN_EPIC ? "UNKNOWN-EPIC" : story.epicKey();
            String teamKey = status == AttributionStatus.UNKNOWN_STORY || status == AttributionStatus.MISSING_STORY_KEY
                    ? teamFor(i)
                    : story.teamKey();
            String workType = status == AttributionStatus.VALID || status == AttributionStatus.UNKNOWN_EPIC ? story.workType() : "UNKNOWN";
            int promptTokens = 250 + (i % 1300);
            int completionTokens = 100 + (i % 700);
            int totalTokens = promptTokens + completionTokens;
            BigDecimal cost = BigDecimal.valueOf(totalTokens).multiply(new BigDecimal("0.00000008")).setScale(8, java.math.RoundingMode.HALF_UP);
            insertUsageEvent(
                    storyKey,
                    epicKey,
                    teamKey,
                    users[i % users.length],
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    cost,
                    start.plusMinutes(i * 7L),
                    workType,
                    status
            );
        }
    }

    private AttributionStatus statusFor(int index) {
        if (index % 100 < 92) {
            return AttributionStatus.VALID;
        }
        if (index % 100 < 96) {
            return AttributionStatus.UNKNOWN_STORY;
        }
        if (index % 100 < 99) {
            return AttributionStatus.MISSING_STORY_KEY;
        }
        return AttributionStatus.UNKNOWN_EPIC;
    }

    private String teamFor(int index) {
        return switch (index % 3) {
            case 0 -> "payments";
            case 1 -> "customer-experience";
            default -> "platform";
        };
    }

    private void insertUsageEvent(
            String storyKey,
            String epicKey,
            String teamKey,
            String userKey,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            BigDecimal cost,
            OffsetDateTime timestamp,
            String workType,
            AttributionStatus attributionStatus
    ) {
        jdbcTemplate.update("""
                        INSERT INTO ai_usage_events (
                            id, provider, model, story_key, epic_key, team_key, user_key,
                            prompt_tokens, completion_tokens, total_tokens, estimated_cost_usd,
                            latency_ms, request_timestamp, environment, work_type, request_status,
                            attribution_status, request_hash, attribution_source, attribution_confidence, inference_reason
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                "OLLAMA",
                "llama3.2",
                storyKey,
                epicKey,
                teamKey,
                userKey,
                promptTokens,
                completionTokens,
                totalTokens,
                cost,
                25 + (totalTokens % 250),
                timestamp,
                "demo",
                workType,
                "SUCCEEDED",
                attributionStatus.name(),
                requestHash(storyKey, timestamp),
                storyKey == null ? "MISSING" : "EXPLICIT",
                storyKey == null ? "LOW" : "HIGH",
                storyKey == null ? "Demo event intentionally omits a story key." : "Demo event includes an explicit story key."
        );
    }

    private String requestHash(String storyKey, OffsetDateTime timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((String.valueOf(storyKey) + "|" + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for demo request hashing.", exception);
        }
    }
}
