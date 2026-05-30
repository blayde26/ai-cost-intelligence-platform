package com.acip.pricing;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class ProviderPricingRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProviderPricingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ProviderPricing> findEffectivePricing(String provider, String model, LocalDate asOfDate) {
        String sql = """
                SELECT provider, model, input_cost_per_million, output_cost_per_million, effective_date
                FROM provider_pricing
                WHERE provider = ? AND model = ? AND effective_date <= ?
                ORDER BY effective_date DESC
                LIMIT 1
                """;
        return jdbcTemplate.query(sql, this::mapPricing, provider, model, asOfDate)
                .stream()
                .findFirst();
    }

    private ProviderPricing mapPricing(ResultSet rs, int rowNum) throws SQLException {
        return new ProviderPricing(
                rs.getString("provider"),
                rs.getString("model"),
                rs.getBigDecimal("input_cost_per_million"),
                rs.getBigDecimal("output_cost_per_million"),
                rs.getDate("effective_date").toLocalDate()
        );
    }
}
