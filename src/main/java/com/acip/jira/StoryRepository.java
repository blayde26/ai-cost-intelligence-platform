package com.acip.jira;

import com.acip.worktracking.WorkItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class StoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public StoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(WorkItem issue) {
        String updateSql = """
                UPDATE stories
                SET summary = ?, status = ?, epic_key = ?, work_type = ?
                WHERE story_key = ?
                """;
        int updated = jdbcTemplate.update(updateSql, issue.summary(), issue.status(), issue.epicKey(), issue.workType(), issue.key());
        if (updated == 0) {
            String insertSql = """
                    INSERT INTO stories (id, story_key, summary, status, epic_key, work_type)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            jdbcTemplate.update(insertSql, java.util.UUID.randomUUID(), issue.key(), issue.summary(), issue.status(), issue.epicKey(), issue.workType());
        }
    }

    public Optional<Story> findByStoryKey(String storyKey) {
        String sql = """
                SELECT story_key, summary, status, epic_key, work_type
                FROM stories
                WHERE story_key = ?
                """;
        return jdbcTemplate.query(sql, this::mapStory, storyKey).stream().findFirst();
    }

    private Story mapStory(ResultSet rs, int rowNum) throws SQLException {
        return new Story(
                rs.getString("story_key"),
                rs.getString("summary"),
                rs.getString("status"),
                rs.getString("epic_key"),
                rs.getString("work_type")
        );
    }
}
