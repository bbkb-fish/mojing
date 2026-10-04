package com.mojing.novel.writing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class LegacyNovelOwnershipMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final long legacyOwnerId;

    LegacyNovelOwnershipMigration(JdbcTemplate jdbcTemplate,
                                  @Value("${mojing.migration.legacy-owner-id:0}") long legacyOwnerId) {
        this.jdbcTemplate = jdbcTemplate;
        this.legacyOwnerId = legacyOwnerId;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (legacyOwnerId > 0)
            jdbcTemplate.update("UPDATE novel SET owner_user_id=? WHERE owner_user_id IS NULL OR owner_user_id=0", legacyOwnerId);
    }
}
