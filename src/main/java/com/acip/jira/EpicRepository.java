package com.acip.jira;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EpicRepository {

    private final JdbcTemplate jdbcTemplate;

    public EpicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(JiraIssue issue) {
        String updateSql = """
                UPDATE epics
                SET summary = ?, status = ?, team_key = ?
                WHERE epic_key = ?
                """;
        int updated = jdbcTemplate.update(updateSql, issue.summary(), issue.status(), issue.teamKey(), issue.issueKey());
        if (updated == 0) {
            String insertSql = """
                    INSERT INTO epics (id, epic_key, summary, status, team_key)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            jdbcTemplate.update(insertSql, java.util.UUID.randomUUID(), issue.issueKey(), issue.summary(), issue.status(), issue.teamKey());
        }
    }
}
