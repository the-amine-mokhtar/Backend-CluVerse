package com.hexaweb.backendcluverse.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SponsorEmailSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SponsorEmailSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Ensure new enum values like INBOUND are accepted regardless of prior enum schema.
        safeExecute("ALTER TABLE sponsor_email MODIFY COLUMN direction VARCHAR(20) NOT NULL");
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore migration errors to avoid blocking application startup.
        }
    }
}
