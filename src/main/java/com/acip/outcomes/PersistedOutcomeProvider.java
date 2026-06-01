package com.acip.outcomes;

import com.acip.sourcecontrol.RepositoryOutcomeMetrics;
import com.acip.sourcecontrol.RepositoryOutcomeProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PersistedOutcomeProvider implements OutcomeProvider {

    private final JdbcTemplate jdbcTemplate;
    private final RepositoryOutcomeProvider repositoryOutcomeProvider;

    public PersistedOutcomeProvider(JdbcTemplate jdbcTemplate, RepositoryOutcomeProvider repositoryOutcomeProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.repositoryOutcomeProvider = repositoryOutcomeProvider;
    }

    @Override
    public String providerKey() {
        return "PERSISTED_WORK_AND_USAGE";
    }

    @Override
    public List<TeamAnalyticsSnapshot> teamSnapshots() {
        String sql = """
                WITH story_metrics AS (
                    SELECT
                        COALESCE(ep.team_key, 'Unassigned') AS team_key,
                        COUNT(*) AS story_count,
                        COALESCE(SUM(CASE WHEN LOWER(s.status) IN ('done', 'closed', 'complete', 'completed') THEN 1 ELSE 0 END), 0) AS completed_story_count,
                        COALESCE(SUM(CASE WHEN LOWER(s.status) IN ('cancelled', 'canceled') THEN 1 ELSE 0 END), 0) AS cancelled_story_count,
                        COALESCE(SUM(CASE WHEN s.work_type = 'CAPITALIZED' THEN 1 ELSE 0 END), 0) AS capitalized_count,
                        COALESCE(SUM(CASE WHEN s.work_type IN ('OPERATIONAL', 'SUPPORT') THEN 1 ELSE 0 END), 0) AS operational_count,
                        COALESCE(SUM(CASE WHEN s.work_type = 'RESEARCH' THEN 1 ELSE 0 END), 0) AS research_count
                    FROM stories s
                    LEFT JOIN epics ep ON ep.epic_key = s.epic_key
                    GROUP BY COALESCE(ep.team_key, 'Unassigned')
                ),
                spend_metrics AS (
                    SELECT
                        COALESCE(team_key, 'Unassigned') AS team_key,
                        COALESCE(SUM(estimated_cost_usd), 0) AS ai_spend,
                        COUNT(*) AS ai_request_count
                    FROM ai_usage_events
                    GROUP BY COALESCE(team_key, 'Unassigned')
                ),
                teams_union AS (
                    SELECT team_key FROM story_metrics
                    UNION
                    SELECT team_key FROM spend_metrics
                    UNION
                    SELECT team_key FROM teams
                )
                SELECT
                    t.team_key,
                    COALESCE(sp.ai_spend, 0) AS ai_spend,
                    COALESCE(sp.ai_request_count, 0) AS ai_request_count,
                    COALESCE(sm.story_count, 0) AS story_count,
                    COALESCE(sm.completed_story_count, 0) AS completed_story_count,
                    COALESCE(sm.cancelled_story_count, 0) AS cancelled_story_count,
                    COALESCE(sm.capitalized_count, 0) AS capitalized_count,
                    COALESCE(sm.operational_count, 0) AS operational_count,
                    COALESCE(sm.research_count, 0) AS research_count
                FROM teams_union t
                LEFT JOIN story_metrics sm ON sm.team_key = t.team_key
                LEFT JOIN spend_metrics sp ON sp.team_key = t.team_key
                ORDER BY ai_spend DESC, ai_request_count DESC, t.team_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapTeamSnapshot);
    }

    @Override
    public List<RepositoryAnalyticsSnapshot> repositorySnapshots() {
        String sql = """
                SELECT
                    COALESCE(NULLIF(repository, ''), 'Unassigned') AS repository,
                    COALESCE(SUM(estimated_cost_usd), 0) AS ai_spend,
                    COUNT(*) AS ai_request_count,
                    COALESCE(SUM(total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(CASE WHEN attribution_status IN ('VALID', 'MANUAL') THEN 1 ELSE 0 END), 0) AS attributed_event_count,
                    COALESCE(SUM(CASE WHEN attribution_status NOT IN ('VALID', 'MANUAL') THEN 1 ELSE 0 END), 0) AS unattributed_event_count
                FROM ai_usage_events
                GROUP BY COALESCE(NULLIF(repository, ''), 'Unassigned')
                ORDER BY ai_spend DESC, ai_request_count DESC, repository ASC
                """;
        Map<String, RepositoryAggregate> repositories = new LinkedHashMap<>();
        jdbcTemplate.query(sql, this::mapRepositoryUsage).forEach(usage -> repositories
                .computeIfAbsent(usage.repository(), RepositoryAggregate::new)
                .applyUsage(usage));
        repositoryOutcomeProvider.repositoryMetrics().forEach(metrics -> repositories
                .computeIfAbsent(metrics.repository(), RepositoryAggregate::new)
                .applyOutcome(metrics));
        return repositories.values().stream()
                .map(this::toRepositorySnapshot)
                .sorted(Comparator
                        .comparing(RepositoryAnalyticsSnapshot::aiSpend).reversed()
                        .thenComparing(snapshot -> snapshot.prCount() == null ? 0L : snapshot.prCount(), Comparator.reverseOrder())
                        .thenComparing(RepositoryAnalyticsSnapshot::repository))
                .toList();
    }

    private TeamAnalyticsSnapshot mapTeamSnapshot(ResultSet rs, int rowNum) throws SQLException {
        long storyCount = rs.getLong("story_count");
        long completed = rs.getLong("completed_story_count");
        long cancelled = rs.getLong("cancelled_story_count");
        long capitalized = rs.getLong("capitalized_count");
        long operational = rs.getLong("operational_count");
        long research = rs.getLong("research_count");
        BigDecimal aiSpend = nonNull(rs.getBigDecimal("ai_spend"));
        return new TeamAnalyticsSnapshot(
                rs.getString("team_key"),
                aiSpend,
                rs.getLong("ai_request_count"),
                storyCount,
                completed,
                cancelled,
                percent(completed, storyCount),
                percent(cancelled, storyCount),
                percent(capitalized, storyCount),
                percent(operational, storyCount),
                percent(research, storyCount),
                null,
                null,
                null,
                storyCount > 0 ? OutcomeDataStatus.PARTIAL : OutcomeDataStatus.UNAVAILABLE,
                interpretation(aiSpend, storyCount)
        );
    }

    private RepositoryUsageMetrics mapRepositoryUsage(ResultSet rs, int rowNum) throws SQLException {
        return new RepositoryUsageMetrics(
                rs.getString("repository"),
                nonNull(rs.getBigDecimal("ai_spend")),
                rs.getLong("ai_request_count"),
                rs.getLong("total_tokens"),
                rs.getLong("attributed_event_count"),
                rs.getLong("unattributed_event_count")
        );
    }

    private RepositoryAnalyticsSnapshot toRepositorySnapshot(RepositoryAggregate aggregate) {
        long attributed = aggregate.attributedEventCount;
        long unattributed = aggregate.unattributedEventCount;
        boolean hasUsage = aggregate.aiRequestCount > 0;
        boolean hasOutcome = aggregate.prCount != null || aggregate.commitCount != null;
        return new RepositoryAnalyticsSnapshot(
                aggregate.repository,
                aggregate.owner,
                aggregate.teamKey,
                aggregate.aiSpend,
                aggregate.aiRequestCount,
                aggregate.totalTokens,
                attributed,
                unattributed,
                percent(attributed, attributed + unattributed),
                aggregate.prCount,
                aggregate.commitCount,
                aggregate.reviewCount,
                aggregate.commentCount,
                aggregate.averageMergeTimeHours,
                aggregate.averageReviewTimeHours,
                repositoryStatus(hasUsage, hasOutcome),
                repositoryInterpretation(hasUsage, hasOutcome)
        );
    }

    private OutcomeDataStatus repositoryStatus(boolean hasUsage, boolean hasOutcome) {
        if (hasUsage && hasOutcome) {
            return OutcomeDataStatus.AVAILABLE;
        }
        if (hasUsage || hasOutcome) {
            return OutcomeDataStatus.PARTIAL;
        }
        return OutcomeDataStatus.UNAVAILABLE;
    }

    private String repositoryInterpretation(boolean hasUsage, boolean hasOutcome) {
        if (hasUsage && hasOutcome) {
            return "AI spend and source-control outcome data are both available. Treat movement as correlation, not causation.";
        }
        if (hasUsage) {
            return "Repository spend is available from usage metadata. Source-control outcome metrics are not available for this repository.";
        }
        if (hasOutcome) {
            return "Source-control outcome data is available, but no AI spend is attached to this repository yet.";
        }
        return "No usage or source-control outcome data is available for this repository.";
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String interpretation(BigDecimal aiSpend, long storyCount) {
        if (storyCount == 0 && BigDecimal.ZERO.compareTo(aiSpend) == 0) {
            return "No story outcome or AI spend data is available yet.";
        }
        if (storyCount == 0) {
            return "AI spend is available, but story outcome data is missing for this team.";
        }
        if (BigDecimal.ZERO.compareTo(aiSpend) == 0) {
            return "Story outcome data is available, but no AI spend is attached to this team.";
        }
        return "AI spend and story outcome data are both available. Treat movement as correlation, not causation.";
    }

    private record RepositoryUsageMetrics(
            String repository,
            BigDecimal aiSpend,
            long aiRequestCount,
            long totalTokens,
            long attributedEventCount,
            long unattributedEventCount
    ) {
    }

    private static class RepositoryAggregate {
        private final String repository;
        private String owner;
        private String teamKey;
        private BigDecimal aiSpend = BigDecimal.ZERO;
        private long aiRequestCount;
        private long totalTokens;
        private long attributedEventCount;
        private long unattributedEventCount;
        private Long prCount;
        private Long commitCount;
        private Long reviewCount;
        private Long commentCount;
        private Double averageMergeTimeHours;
        private Double averageReviewTimeHours;

        private RepositoryAggregate(String repository) {
            this.repository = repository;
        }

        private void applyUsage(RepositoryUsageMetrics usage) {
            this.aiSpend = usage.aiSpend();
            this.aiRequestCount = usage.aiRequestCount();
            this.totalTokens = usage.totalTokens();
            this.attributedEventCount = usage.attributedEventCount();
            this.unattributedEventCount = usage.unattributedEventCount();
        }

        private void applyOutcome(RepositoryOutcomeMetrics metrics) {
            this.owner = metrics.owner();
            this.teamKey = metrics.teamKey();
            this.prCount = metrics.prCount();
            this.commitCount = metrics.commitCount();
            this.reviewCount = metrics.reviewCount();
            this.commentCount = metrics.commentCount();
            this.averageMergeTimeHours = metrics.averageMergeTimeHours();
            this.averageReviewTimeHours = metrics.averageReviewTimeHours();
        }
    }
}
