package com.acip.reports;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class SpendReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SpendReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SpendByStoryReport> spendByStory() {
        String sql = """
                SELECT
                    e.story_key,
                    MAX(s.summary) AS story_summary,
                    COALESCE(MAX(e.epic_key), MAX(s.epic_key)) AS epic_key,
                    MAX(e.team_key) AS team_key,
                    COUNT(*) AS request_count,
                    COALESCE(SUM(e.total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(e.estimated_cost_usd), 0) AS estimated_cost_usd
                FROM ai_usage_events e
                LEFT JOIN stories s ON s.story_key = e.story_key
                GROUP BY e.story_key
                ORDER BY estimated_cost_usd DESC, request_count DESC, e.story_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapStoryReport);
    }

    public List<SpendByEpicReport> spendByEpic() {
        String sql = """
                SELECT
                    COALESCE(e.epic_key, s.epic_key, 'UNKNOWN') AS epic_key,
                    MAX(ep.summary) AS epic_summary,
                    MAX(e.team_key) AS team_key,
                    COUNT(*) AS request_count,
                    COALESCE(SUM(e.total_tokens), 0) AS total_tokens,
                    COALESCE(SUM(e.estimated_cost_usd), 0) AS estimated_cost_usd
                FROM ai_usage_events e
                LEFT JOIN stories s ON s.story_key = e.story_key
                LEFT JOIN epics ep ON ep.epic_key = COALESCE(e.epic_key, s.epic_key)
                GROUP BY COALESCE(e.epic_key, s.epic_key, 'UNKNOWN')
                ORDER BY estimated_cost_usd DESC, request_count DESC, epic_key ASC
                """;
        return jdbcTemplate.query(sql, this::mapEpicReport);
    }

    private SpendByStoryReport mapStoryReport(ResultSet rs, int rowNum) throws SQLException {
        return new SpendByStoryReport(
                rs.getString("story_key"),
                rs.getString("story_summary"),
                rs.getString("epic_key"),
                rs.getString("team_key"),
                rs.getLong("request_count"),
                rs.getLong("total_tokens"),
                rs.getBigDecimal("estimated_cost_usd")
        );
    }

    private SpendByEpicReport mapEpicReport(ResultSet rs, int rowNum) throws SQLException {
        return new SpendByEpicReport(
                rs.getString("epic_key"),
                rs.getString("epic_summary"),
                rs.getString("team_key"),
                rs.getLong("request_count"),
                rs.getLong("total_tokens"),
                rs.getBigDecimal("estimated_cost_usd")
        );
    }
}
