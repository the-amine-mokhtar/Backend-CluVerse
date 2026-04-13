package com.hexaweb.backendcluverse.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SponsorshipSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SponsorshipSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        safeExecute("ALTER TABLE sponsorship MODIFY COLUMN status VARCHAR(30) NOT NULL");
        safeExecute("ALTER TABLE sponsorship MODIFY COLUMN club_id BIGINT NULL");
        safeExecute("ALTER TABLE sponsorship ADD COLUMN outreach_response_token VARCHAR(120) NULL");
        safeExecute("ALTER TABLE sponsorship ADD COLUMN outreach_decision VARCHAR(20) NULL");
        safeExecute("ALTER TABLE sponsorship ADD COLUMN outreach_responded_at DATETIME NULL");
        safeExecute("ALTER TABLE sponsorship ADD COLUMN signed_upload_token VARCHAR(120) NULL");
        safeExecute("CREATE UNIQUE INDEX uk_sponsorship_outreach_response_token ON sponsorship(outreach_response_token)");
        safeExecute("CREATE UNIQUE INDEX uk_sponsorship_signed_upload_token ON sponsorship(signed_upload_token)");
        safeExecute("UPDATE sponsorship SET status='PROSPECTING' WHERE status IN ('PROPOSED', 'CANCELLED')");
        safeExecute("UPDATE sponsorship SET status='CONTRACT_SENT' WHERE status='PROPOSAL_SENT'");
        safeExecute("UPDATE sponsorship SET status='SIGNED' WHERE status='ACTIVE'");
        safeExecute("UPDATE sponsorship SET status='PAID' WHERE status='COMPLETED'");
        safeExecute("UPDATE sponsorship SET outreach_decision='PENDING' WHERE outreach_decision IS NULL");
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore migration errors to avoid blocking startup.
        }
    }
}
