package com.acip.reports;

import com.acip.usage.AttributionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class AttributionReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttributionReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AttributionCoverageReport coverage() {
        return coverage(ReportDateRange.parse(null, null));
    }

    public AttributionCoverageReport coverage(ReportDateRange range) {
        ReportDateRange.QueryParts dateQuery = range.whereParts("");
        String sql = """
                SELECT
                    COALESCE(SUM(estimated_cost_usd), 0) AS total_cost,
                    COALESCE(SUM(CASE WHEN attribution_status = 'VALID' THEN estimated_cost_usd ELSE 0 END), 0) AS attributed_cost,
                    COALESCE(SUM(CASE WHEN attribution_status <> 'VALID' THEN estimated_cost_usd ELSE 0 END), 0) AS unattributed_cost,
                    COUNT(*) AS event_count,
                    COALESCE(SUM(CASE WHEN attribution_status = 'VALID' THEN 1 ELSE 0 END), 0) AS valid_event_count,
                    COALESCE(SUM(CASE WHEN attribution_status <> 'VALID' THEN 1 ELSE 0 END), 0) AS invalid_event_count
                FROM ai_usage_events
                """ + dateQuery.whereClause();
        return jdbcTemplate.queryForObject(sql, this::mapCoverage, dateQuery.args().toArray());
    }

    public List<UnattributedSpendEvent> unattributedEvents(UnattributedSpendFilter filter) {
        QueryParts query = unattributedWhere(filter);
        String orderBy = switch (filter.sort() == null ? "cost_desc" : filter.sort()) {
            case "tokens_desc" -> "total_tokens DESC, request_timestamp DESC";
            case "recent" -> "request_timestamp DESC";
            default -> "estimated_cost_usd DESC, request_timestamp DESC";
        };
        String sql = """
                SELECT id, story_key, team_key, user_key, provider, model, estimated_cost_usd,
                       total_tokens, attribution_status, request_timestamp
                FROM ai_usage_events
                """ + query.whereClause() + " ORDER BY " + orderBy;
        return jdbcTemplate.query(sql, this::mapUnattributedEvent, query.args().toArray());
    }

    public UnattributedSpendSummary unattributedSummary(UnattributedSpendFilter filter) {
        QueryParts query = unattributedWhere(filter);
        String sql = """
                SELECT attribution_status,
                       COALESCE(SUM(estimated_cost_usd), 0) AS total_cost,
                       COALESCE(SUM(total_tokens), 0) AS total_tokens,
                       COUNT(*) AS event_count
                FROM ai_usage_events
                """ + query.whereClause() + " GROUP BY attribution_status";
        List<SummaryRow> rows = jdbcTemplate.query(sql, this::mapSummaryRow, query.args().toArray());
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0;
        long eventCount = 0;
        Map<AttributionStatus, BigDecimal> breakdown = new EnumMap<>(AttributionStatus.class);
        for (SummaryRow row : rows) {
            totalCost = totalCost.add(row.cost());
            totalTokens += row.tokens();
            eventCount += row.count();
            breakdown.put(row.status(), row.cost());
        }
        return new UnattributedSpendSummary(totalCost, totalTokens, eventCount, breakdown);
    }

    private QueryParts unattributedWhere(UnattributedSpendFilter filter) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        conditions.add("attribution_status <> 'VALID'");
        if (filter.fromDate() != null) {
            conditions.add("request_timestamp >= ?");
            args.add(filter.fromDate());
        }
        if (filter.toDate() != null) {
            conditions.add("request_timestamp < ?");
            args.add(filter.toDate());
        }
        if (filter.teamKey() != null && !filter.teamKey().isBlank()) {
            conditions.add("team_key = ?");
            args.add(filter.teamKey());
        }
        if (filter.userKey() != null && !filter.userKey().isBlank()) {
            conditions.add("user_key = ?");
            args.add(filter.userKey());
        }
        if (filter.attributionStatus() != null) {
            conditions.add("attribution_status = ?");
            args.add(filter.attributionStatus().name());
        }
        return new QueryParts(" WHERE " + String.join(" AND ", conditions), args);
    }

    private AttributionCoverageReport mapCoverage(ResultSet rs, int rowNum) throws SQLException {
        return new AttributionCoverageReport(
                rs.getBigDecimal("total_cost"),
                rs.getBigDecimal("attributed_cost"),
                rs.getBigDecimal("unattributed_cost"),
                0.0,
                rs.getLong("event_count"),
                rs.getLong("valid_event_count"),
                rs.getLong("invalid_event_count")
        );
    }

    private UnattributedSpendEvent mapUnattributedEvent(ResultSet rs, int rowNum) throws SQLException {
        return new UnattributedSpendEvent(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("story_key"),
                rs.getString("team_key"),
                rs.getString("user_key"),
                rs.getString("provider"),
                rs.getString("model"),
                rs.getBigDecimal("estimated_cost_usd"),
                rs.getLong("total_tokens"),
                AttributionStatus.valueOf(rs.getString("attribution_status")),
                rs.getObject("request_timestamp", OffsetDateTime.class)
        );
    }

    private SummaryRow mapSummaryRow(ResultSet rs, int rowNum) throws SQLException {
        return new SummaryRow(
                AttributionStatus.valueOf(rs.getString("attribution_status")),
                rs.getBigDecimal("total_cost"),
                rs.getLong("total_tokens"),
                rs.getLong("event_count")
        );
    }

    private record QueryParts(String whereClause, List<Object> args) {
    }

    private record SummaryRow(AttributionStatus status, BigDecimal cost, long tokens, long count) {
    }
}
