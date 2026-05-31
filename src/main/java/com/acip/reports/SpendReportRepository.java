package com.acip.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SpendReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SpendReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SpendByStoryReport> spendByStory() {
        return spendByStory(ReportDateRange.parse(null, null));
    }

    public List<SpendByStoryReport> spendByStory(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("e");
        String sql = """
                SELECT
                    e.story_key,
                    MAX(s.summary) AS story_name,
                    COALESCE(MAX(e.epic_key), MAX(s.epic_key)) AS epic_key,
                    MAX(e.team_key) AS team_key,
                    COUNT(*) AS request_count,
                    COALESCE(SUM(e.total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(e.estimated_cost_usd), 0) AS total_cost
                FROM ai_usage_events e
                LEFT JOIN stories s ON s.story_key = e.story_key
                """ + dateQuery.whereClause() + """
                GROUP BY e.story_key
                ORDER BY total_cost DESC, request_count DESC, e.story_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapStoryReport, dateQuery.args().toArray());
    }

    public SpendOverviewReport overview() {
        return overview(ReportDateRange.parse(null, null));
    }

    public SpendOverviewReport overview(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("");
        String sql = """
                SELECT
                    COALESCE(SUM(estimated_cost_usd), 0) AS total_spend,
                    COALESCE(SUM(total_tokens), 0) AS total_tokens,
                    COUNT(*) AS total_requests
                FROM ai_usage_events
                """ + dateQuery.whereClause();
        return jdbcTemplate.queryForObject(sql, this::mapOverview, dateQuery.args().toArray());
    }

    public List<SpendByTeamReport> spendByTeam() {
        return spendByTeam(ReportDateRange.parse(null, null));
    }

    public List<SpendByTeamReport> spendByTeam(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("");
        String sql = """
                SELECT
                    team_key,
                    COUNT(*) AS request_count,
                    COALESCE(SUM(total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(estimated_cost_usd), 0) AS total_cost,
                    COUNT(DISTINCT COALESCE(epic_key, 'UNKNOWN')) AS epic_count,
                    COUNT(DISTINCT COALESCE(story_key, 'MISSING')) AS story_count
                FROM ai_usage_events
                """ + dateQuery.whereClause() + """
                GROUP BY team_key
                ORDER BY total_cost DESC, request_count DESC, team_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapTeamReport, dateQuery.args().toArray());
    }

    public List<SpendByEpicReport> spendByEpic() {
        return spendByEpic(ReportDateRange.parse(null, null));
    }

    public List<SpendByEpicReport> spendByEpic(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("e");
        String sql = """
                SELECT
                    COALESCE(e.epic_key, s.epic_key, 'UNKNOWN') AS epic_key,
                    MAX(ep.summary) AS epic_name,
                    MAX(e.team_key) AS team_key,
                    COUNT(*) AS request_count,
                    COALESCE(SUM(e.total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(e.estimated_cost_usd), 0) AS total_cost,
                    COUNT(DISTINCT COALESCE(e.story_key, 'MISSING')) AS story_count
                FROM ai_usage_events e
                LEFT JOIN stories s ON s.story_key = e.story_key
                LEFT JOIN epics ep ON ep.epic_key = COALESCE(e.epic_key, s.epic_key)
                """ + dateQuery.whereClause() + """
                GROUP BY COALESCE(e.epic_key, s.epic_key, 'UNKNOWN')
                ORDER BY total_cost DESC, request_count DESC, epic_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapEpicReport, dateQuery.args().toArray());
    }

    public PotentialWasteReport potentialWaste(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("e");
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN LOWER(COALESCE(s.status, '')) IN ('cancelled', 'canceled') THEN e.estimated_cost_usd ELSE 0 END), 0) AS cancelled_story_spend,
                    COALESCE(SUM(CASE WHEN e.work_type IN ('OPERATIONAL', 'SUPPORT') THEN e.estimated_cost_usd ELSE 0 END), 0) AS operational_spend,
                    COALESCE(SUM(CASE WHEN e.attribution_status <> 'VALID' THEN e.estimated_cost_usd ELSE 0 END), 0) AS unknown_attribution_spend,
                    COALESCE(SUM(CASE WHEN e.request_status <> 'SUCCEEDED' THEN e.estimated_cost_usd ELSE 0 END), 0) AS failed_request_spend
                FROM ai_usage_events e
                LEFT JOIN stories s ON s.story_key = e.story_key
                """ + dateQuery.whereClause();
        return jdbcTemplate.queryForObject(sql, this::mapPotentialWaste, dateQuery.args().toArray());
    }

    public SpendAllocationReport allocation(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("e");
        String sql = """
                SELECT category,
                       COALESCE(SUM(estimated_cost_usd), 0) AS total_cost
                FROM (
                    SELECT e.estimated_cost_usd,
                           CASE
                               WHEN e.attribution_status <> 'VALID' THEN 'Unattributed'
                               WHEN e.request_status <> 'SUCCEEDED'
                                    OR LOWER(COALESCE(s.status, '')) IN ('cancelled', 'canceled') THEN 'Potential Waste'
                               WHEN e.work_type IN ('OPERATIONAL', 'SUPPORT') THEN 'Operations'
                               WHEN e.work_type = 'RESEARCH' THEN 'Research'
                               ELSE 'Revenue Features'
                           END AS category
                    FROM ai_usage_events e
                    LEFT JOIN stories s ON s.story_key = e.story_key
                    """ + dateQuery.whereClause() + """
                ) allocation
                GROUP BY category
                """;
        List<SpendAllocationBucket> rawBuckets = jdbcTemplate.query(sql, this::mapRawAllocationBucket, dateQuery.args().toArray());
        BigDecimal totalCost = rawBuckets.stream()
                .map(SpendAllocationBucket::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SpendAllocationBucket> buckets = new ArrayList<>();
        List<String> order = List.of("Revenue Features", "Operations", "Research", "Unattributed", "Potential Waste");
        for (String category : order) {
            BigDecimal cost = rawBuckets.stream()
                    .filter(bucket -> bucket.category().equals(category))
                    .map(SpendAllocationBucket::totalCost)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            double percent = BigDecimal.ZERO.compareTo(totalCost) == 0
                    ? 0.0
                    : cost.multiply(new BigDecimal("100")).divide(totalCost, 2, RoundingMode.HALF_UP).doubleValue();
            buckets.add(new SpendAllocationBucket(category, cost, percent));
        }
        return new SpendAllocationReport(totalCost, buckets);
    }

    private SpendOverviewReport mapOverview(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal totalSpend = rs.getBigDecimal("total_spend");
        return new SpendOverviewReport(
                totalSpend == null ? BigDecimal.ZERO : totalSpend,
                rs.getLong("total_tokens"),
                rs.getLong("total_requests")
        );
    }

    private SpendByStoryReport mapStoryReport(ResultSet rs, int rowNum) throws SQLException {
        return new SpendByStoryReport(
                rs.getString("story_key"),
                rs.getString("story_name"),
                rs.getString("epic_key"),
                rs.getString("team_key"),
                rs.getBigDecimal("total_cost"),
                rs.getLong("total_tokens"),
                rs.getLong("request_count")
        );
    }

    private SpendByTeamReport mapTeamReport(ResultSet rs, int rowNum) throws SQLException {
        return new SpendByTeamReport(
                rs.getString("team_key"),
                rs.getBigDecimal("total_cost"),
                rs.getLong("total_tokens"),
                rs.getLong("request_count"),
                rs.getLong("epic_count"),
                rs.getLong("story_count")
        );
    }

    private SpendByEpicReport mapEpicReport(ResultSet rs, int rowNum) throws SQLException {
        return new SpendByEpicReport(
                rs.getString("epic_key"),
                rs.getString("epic_name"),
                rs.getString("team_key"),
                rs.getBigDecimal("total_cost"),
                rs.getLong("total_tokens"),
                rs.getLong("request_count"),
                rs.getLong("story_count")
        );
    }

    private PotentialWasteReport mapPotentialWaste(ResultSet rs, int rowNum) throws SQLException {
        return new PotentialWasteReport(
                rs.getBigDecimal("cancelled_story_spend"),
                rs.getBigDecimal("operational_spend"),
                rs.getBigDecimal("unknown_attribution_spend"),
                rs.getBigDecimal("failed_request_spend")
        );
    }

    private SpendAllocationBucket mapRawAllocationBucket(ResultSet rs, int rowNum) throws SQLException {
        return new SpendAllocationBucket(
                rs.getString("category"),
                rs.getBigDecimal("total_cost"),
                0.0
        );
    }
}
