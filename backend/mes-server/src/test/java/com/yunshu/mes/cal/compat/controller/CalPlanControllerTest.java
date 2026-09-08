package com.yunshu.mes.cal.compat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yunshu.mes.cal.compat.service.CalTeamshiftService;
import com.yunshu.mes.cal.compat.service.CalPlanMutationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CalPlanControllerTest {

    @Test
    void emptyPlanTableReturnsNoRowsInsteadOfFallbackData() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("cal_plan")))
                .thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForObject("SELECT COUNT(*) FROM cal_plan", Long.class)).thenReturn(0L);

        CalPlanController controller = new CalPlanController(
                jdbc, mock(CalTeamshiftService.class), mock(CalPlanMutationService.class));
        Map<String, Object> response = controller.list(Map.of());

        assertEquals(List.of(), response.get("rows"));
        assertEquals(0L, response.get("total"));
    }

    @Test
    void creationForcesPrepareAndReportsEffectiveStatus() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("cal_plan")))
                .thenReturn(1);
        when(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).thenReturn(73L);
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);
        CalPlanMutationService mutation = mock(CalPlanMutationService.class);
        CalPlanController controller = new CalPlanController(jdbc, teamshift, mutation);

        Map<String, Object> response = controller.add(Map.of(
                "planCode", "PLAN-DIRECT-CONFIRM",
                "planName", "禁止直接确认",
                "calendarType", "ZZ",
                "startDate", "2026-08-01",
                "endDate", "2026-08-31",
                "shiftType", "SHIFT",
                "status", "CONFIRMED"));

        verify(jdbc).update(contains("INSERT INTO cal_plan"),
                eq("PLAN-DIRECT-CONFIRM"), eq("禁止直接确认"), eq("ZZ"),
                eq("2026-08-01"), eq("2026-08-31"), eq("SHIFT"),
                eq(null), eq(1), eq("PREPARE"), eq(""), eq("Y"));
        verify(teamshift).addDefaultShifts(73L, "SHIFT");
        verify(mutation, never()).edit(any());
        assertEquals(73L, response.get("data"));
        assertEquals("PREPARE", response.get("status"));
        assertEquals("排班计划已按草稿状态创建；请通过修改接口完成确认", response.get("msg"));
    }
}
