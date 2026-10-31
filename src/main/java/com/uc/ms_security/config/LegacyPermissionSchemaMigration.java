package com.uc.ms_security.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyPermissionSchemaMigration implements ApplicationRunner {

    private static final String LEGACY_PERMISSION_INDEXES = """
            SELECT index_name
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'permissions'
              AND non_unique = 0
              AND index_name <> 'PRIMARY'
            GROUP BY index_name
            HAVING COUNT(*) = 2
               AND SUM(column_name IN ('url', 'method')) = 2
            """;

    private final JdbcTemplate jdbcTemplate;

    public LegacyPermissionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        dropLegacyPermissionIndexes();

        if (!columnExists("model")) {
            return;
        }

        if (!columnExists("entity")) {
            jdbcTemplate.execute(
                    "ALTER TABLE permissions "
                            + "ADD COLUMN entity VARCHAR(255) NULL AFTER method"
            );
        }

        jdbcTemplate.execute(
                "UPDATE permissions SET entity = model "
                        + "WHERE entity IS NULL OR entity = ''"
        );
        jdbcTemplate.execute(
                "ALTER TABLE permissions "
                        + "MODIFY COLUMN entity VARCHAR(255) NOT NULL"
        );
        jdbcTemplate.execute(
                "ALTER TABLE permissions DROP COLUMN model"
        );
    }

    private void dropLegacyPermissionIndexes() {
        List<String> indexNames = jdbcTemplate.queryForList(
                LEGACY_PERMISSION_INDEXES,
                String.class
        );
        for (String indexName : indexNames) {
            jdbcTemplate.execute(
                    "ALTER TABLE permissions DROP INDEX `"
                            + indexName.replace("`", "``")
                            + "`"
            );
        }
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = ? AND column_name = ?",
                Integer.class,
                "permissions",
                columnName
        );
        return count != null && count > 0;
    }
}
