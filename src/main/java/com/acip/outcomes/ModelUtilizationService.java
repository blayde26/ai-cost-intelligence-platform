package com.acip.outcomes;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ModelUtilizationService {

    private final JdbcTemplate jdbcTemplate;

    public ModelUtilizationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProviderUtilizationSnapshot> providerSnapshots() {
        BigDecimal totalCost = totalCost();
        String sql = """
                SELECT
                    provider,
                    COALESCE(SUM(estimated_cost_usd), 0) AS total_cost,
                    COALESCE(SUM(total_tokens), 0) AS total_tokens,
                    COUNT(*) AS request_count,
                    COUNT(DISTINCT model) AS model_count
                FROM ai_usage_events
                GROUP BY provider
                ORDER BY total_cost DESC, request_count DESC, provider ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapProvider(rs, totalCost));
    }

    public List<ModelUtilizationSnapshot> modelSnapshots() {
        BigDecimal totalCost = totalCost();
        String sql = """
                SELECT
                    provider,
                    model,
                    COALESCE(SUM(estimated_cost_usd), 0) AS total_cost,
                    COALESCE(SUM(total_tokens), 0) AS total_tokens,
                    COUNT(*) AS request_count,
                    COUNT(DISTINCT team_key) AS team_count,
                    COUNT(DISTINCT work_type) AS work_type_count
                FROM ai_usage_events
                GROUP BY provider, model
                ORDER BY total_cost DESC, request_count DESC, provider ASC, model ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapModel(rs, totalCost));
    }

    private ProviderUtilizationSnapshot mapProvider(ResultSet rs, BigDecimal grandTotalCost) throws SQLException {
        BigDecimal providerCost = nonNull(rs.getBigDecimal("total_cost"));
        return new ProviderUtilizationSnapshot(
                rs.getString("provider"),
                providerCost,
                rs.getLong("total_tokens"),
                rs.getLong("request_count"),
                rs.getLong("model_count"),
                percent(providerCost, grandTotalCost)
        );
    }

    private ModelUtilizationSnapshot mapModel(ResultSet rs, BigDecimal grandTotalCost) throws SQLException {
        BigDecimal modelCost = nonNull(rs.getBigDecimal("total_cost"));
        return new ModelUtilizationSnapshot(
                rs.getString("provider"),
                rs.getString("model"),
                modelCost,
                rs.getLong("total_tokens"),
                rs.getLong("request_count"),
                rs.getLong("team_count"),
                rs.getLong("work_type_count"),
                percent(modelCost, grandTotalCost)
        );
    }

    private BigDecimal totalCost() {
        BigDecimal value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(estimated_cost_usd), 0) FROM ai_usage_events",
                BigDecimal.class
        );
        return nonNull(value);
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double percent(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return value
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
