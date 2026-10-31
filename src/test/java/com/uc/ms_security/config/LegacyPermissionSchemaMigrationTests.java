package com.uc.ms_security.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyPermissionSchemaMigrationTests {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final LegacyPermissionSchemaMigration migration =
            new LegacyPermissionSchemaMigration(jdbcTemplate);

    @Test
    void migratesLegacyModelColumnBeforeSeederUsesPermissions() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(java.util.List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(1);

        migration.run(null);

        var order = inOrder(jdbcTemplate);
        order.verify(jdbcTemplate).execute(
                "UPDATE permissions SET entity = model "
                        + "WHERE entity IS NULL OR entity = ''"
        );
        order.verify(jdbcTemplate).execute(
                "ALTER TABLE permissions "
                        + "MODIFY COLUMN entity VARCHAR(255) NOT NULL"
        );
        order.verify(jdbcTemplate).execute(
                "ALTER TABLE permissions DROP COLUMN model"
        );
    }

    @Test
    void addsEntityColumnWhenOnlyLegacyColumnExists() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(java.util.List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(1, 0);

        migration.run(null);

        verify(jdbcTemplate).execute(
                "ALTER TABLE permissions "
                        + "ADD COLUMN entity VARCHAR(255) NULL AFTER method"
        );
        verify(jdbcTemplate).execute(
                "UPDATE permissions SET entity = model "
                        + "WHERE entity IS NULL OR entity = ''"
        );
    }

    @Test
    void doesNothingWhenLegacyColumnIsAbsent() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(java.util.List.of());
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(0);

        migration.run(null);

        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void dropsOldUniqueIndexThatOnlyUsedUrlAndMethod() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(java.util.List.of("UK_old_permission_url_method"));
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(0);

        migration.run(null);

        verify(jdbcTemplate).execute(
                "ALTER TABLE permissions "
                        + "DROP INDEX `UK_old_permission_url_method`"
        );
    }

    @Test
    void escapesIndexNameBeforeDroppingIt() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(java.util.List.of("old`index"));
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(0);

        migration.run(null);

        verify(jdbcTemplate).execute(
                "ALTER TABLE permissions DROP INDEX `old``index`"
        );
    }
}
