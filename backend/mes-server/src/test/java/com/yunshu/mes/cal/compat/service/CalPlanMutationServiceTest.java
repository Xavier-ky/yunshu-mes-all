package com.yunshu.mes.cal.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CalPlanMutationServiceTest {

    @Test
    void confirmationValidatesSubmittedCandidateFieldsBeforeUpdating() {
        JdbcTemplate jdbc = preparedJdbc();
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);
        Map<String, Object> body = candidateBody();
        when(teamshift.validateForConfirm(eq(51L), anyMap())).thenAnswer(invocation -> {
            Map<String, Object> candidate = invocation.getArgument(1);
            assertEquals("SHIFT_TWO", candidate.get("shift_type"));
            assertEquals(Date.valueOf("2026-08-01"), candidate.get("start_date"));
            assertEquals(Date.valueOf("2026-08-31"), candidate.get("end_date"));
            return "两班倒需要至少 2 个班次和 2 个班组";
        });

        CalPlanMutationService.Result result = new CalPlanMutationService(jdbc, teamshift).edit(body);

        assertEquals("两班倒需要至少 2 个班次和 2 个班组", result.error());
        verify(jdbc, never()).update(contains("UPDATE cal_plan"), org.mockito.ArgumentMatchers.<Object[]>any());
        verify(teamshift, never()).genRecords(51L);
    }

    @Test
    void confirmationUpdateAndGenerationShareTransactionalBoundary() throws Exception {
        JdbcTemplate jdbc = preparedJdbc();
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);
        when(teamshift.validateForConfirm(eq(51L), anyMap())).thenReturn(null);
        org.mockito.Mockito.doThrow(new IllegalStateException("generation failed"))
                .when(teamshift).genRecords(51L);
        CalPlanMutationService service = new CalPlanMutationService(jdbc, teamshift);

        assertThrows(IllegalStateException.class, () -> service.edit(candidateBody()));
        Transactional annotation = CalPlanMutationService.class
                .getMethod("edit", Map.class).getAnnotation(Transactional.class);
        assertNotNull(annotation);
        InOrder order = inOrder(jdbc, teamshift);
        order.verify(jdbc).update(anyString(), any(Object[].class));
        order.verify(teamshift).genRecords(51L);
    }

    @Test
    void allPlanEditsAcquireGlobalCalendarConfirmationLockBeforePlanRowLock() {
        JdbcTemplate jdbc = preparedJdbc();
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);
        when(teamshift.validateForConfirm(eq(51L), anyMap())).thenReturn(null);

        new CalPlanMutationService(jdbc, teamshift).edit(candidateBody());

        InOrder order = inOrder(jdbc, teamshift);
        order.verify(jdbc).queryForObject(
                eq("SELECT GET_LOCK(?, ?)"), eq(Integer.class),
                eq("mes:calendar:confirmation"), eq(10));
        order.verify(jdbc).queryForMap(contains("FOR UPDATE"), eq(51L));
        order.verify(teamshift).validateForConfirm(eq(51L), anyMap());
        order.verify(jdbc).update(contains("UPDATE cal_plan"), any(Object[].class));
        order.verify(teamshift).genRecords(51L);
        order.verify(jdbc).queryForObject(
                eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                eq("mes:calendar:confirmation"));
    }

    @Test
    void lockTimeoutStopsMutationBeforeTargetPlanIsRead() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT GET_LOCK(?, ?)"), eq(Integer.class),
                eq("mes:calendar:confirmation"), eq(10))).thenReturn(0);
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);

        CalPlanMutationService.Result result =
                new CalPlanMutationService(jdbc, teamshift).edit(candidateBody());

        assertEquals("排班确认繁忙，请稍后重试", result.error());
        verify(jdbc, never()).queryForMap(contains("FOR UPDATE"), any());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(teamshift, never()).validateForConfirm(any(), anyMap());
    }

    @Test
    void transactionalInvocationKeepsNamedLockUntilAfterCompletion() {
        JdbcTemplate jdbc = preparedJdbc();
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        CalTeamshiftService teamshift = mock(CalTeamshiftService.class);
        when(teamshift.validateForConfirm(eq(51L), anyMap())).thenReturn(null);
        TransactionSynchronizationManager.initSynchronization();
        try {
            new CalPlanMutationService(jdbc, teamshift).edit(candidateBody());

            verify(jdbc, never()).queryForObject(
                    eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                    eq("mes:calendar:confirmation"));
            assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());

            TransactionSynchronizationManager.getSynchronizations().get(0)
                    .afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

            verify(jdbc).queryForObject(
                    eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                    eq("mes:calendar:confirmation"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private JdbcTemplate preparedJdbc() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                eq("SELECT GET_LOCK(?, ?)"), eq(Integer.class),
                eq("mes:calendar:confirmation"), eq(10))).thenReturn(1);
        when(jdbc.queryForObject(
                eq("SELECT RELEASE_LOCK(?)"), eq(Integer.class),
                eq("mes:calendar:confirmation"))).thenReturn(1);
        when(jdbc.queryForMap(contains("FOR UPDATE"), eq(51L))).thenReturn(Map.ofEntries(
                Map.entry("plan_id", 51L),
                Map.entry("plan_code", "OLD"),
                Map.entry("plan_name", "旧计划"),
                Map.entry("calendar_type", "ZZ"),
                Map.entry("start_date", Date.valueOf("2026-07-01")),
                Map.entry("end_date", Date.valueOf("2026-07-31")),
                Map.entry("shift_type", "SINGLE"),
                Map.entry("shift_method", "DAY"),
                Map.entry("shift_count", 1),
                Map.entry("status", "PREPARE"),
                Map.entry("remark", ""),
                Map.entry("enable_flag", "Y")));
        return jdbc;
    }

    private Map<String, Object> candidateBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("planId", 51L);
        body.put("planCode", "AUG-ASM");
        body.put("planName", "八月组装计划");
        body.put("calendarType", "ZZ");
        body.put("startDate", "2026-08-01");
        body.put("endDate", "2026-08-31");
        body.put("shiftType", "SHIFT_TWO");
        body.put("shiftMethod", "DAY");
        body.put("shiftCount", 1);
        body.put("status", "CONFIRMED");
        body.put("remark", "");
        body.put("enableFlag", "Y");
        return body;
    }
}
