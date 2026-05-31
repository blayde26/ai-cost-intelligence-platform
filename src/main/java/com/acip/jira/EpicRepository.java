package com.acip.jira;

import com.acip.worktracking.WorkItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EpicRepository {

    private final JdbcTemplate jdbcTemplate;

    public EpicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(WorkItem issue) {
        String updateSql = """
                UPDATE epics
                SET summary = ?, status = ?, team_key = ?
                WHERE epic_key = ?
                """;
        int updated = jdbcTemplate.update(updateSql, issue.summary(), issue.status(), issue.teamKey(), issue.key());
        if (updated == 0) {
            String insertSql = """
                    INSERT INTO epics (id, epic_key, summary, status, team_key)
                    VALUES (?, ?, ?, ?, ?)
                    """;
            jdbcTemplate.update(insertSql, java.util.UUID.randomUUID(), issue.key(), issue.summary(), issue.status(), issue.teamKey());
        }
    }
}
