package com.yunshu.mes.reporting.ureport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class UreportTemplateInitializerTest {

    @Test
    void leavesExistingDatabaseTemplatesUnchanged() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        UreportTemplateInitializer initializer = new UreportTemplateInitializer(jdbc, false);

        initializer.run(mock(ApplicationArguments.class));

        verify(jdbc, atLeastOnce()).queryForObject(anyString(), eq(Integer.class), anyString());
        verify(jdbc, never()).update(
                anyString(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void installsMissingDatabaseTemplates() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        UreportTemplateInitializer initializer = new UreportTemplateInitializer(jdbc, false);

        initializer.run(mock(ApplicationArguments.class));

        verify(jdbc, atLeastOnce()).update(
                anyString(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void explicitlyUpgradesExistingDatabaseTemplates() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        UreportTemplateInitializer initializer = new UreportTemplateInitializer(jdbc, true);

        initializer.run(mock(ApplicationArguments.class));

        verify(jdbc, atLeastOnce()).update(
                org.mockito.ArgumentMatchers.startsWith("UPDATE ureport_file_tbl"),
                any(),
                any(),
                any());
    }
}
