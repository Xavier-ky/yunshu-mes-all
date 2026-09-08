package com.yunshu.mes.cal.compat.controller;

import com.yunshu.mes.cal.compat.service.CalCalendarService;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/cal/calendar")
public class CalCalendarController {

    private final CalCalendarService calendarService;

    public CalCalendarController(CalCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        try {
            QueryScope scope = queryScope(params);
            List<Map<String, Object>> days = calendarService.list(
                    scope.queryType(), params.get("date"), params.get("calendarType"),
                    scope.teamId(), scope.userId());
            return MesApiResponse.ok(days);
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam Map<String, String> params) {
        try {
            QueryScope scope = queryScope(params);
            return MesApiResponse.ok(calendarService.summary(
                    scope.queryType(), params.get("date"), params.get("calendarType"),
                    scope.teamId(), scope.userId()));
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/day")
    public Map<String, Object> day(@RequestParam Map<String, String> params) {
        try {
            QueryScope scope = queryScope(params);
            return MesApiResponse.ok(calendarService.day(
                    scope.queryType(), params.get("date"), params.get("calendarType"),
                    scope.teamId(), scope.userId()));
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.badRequest(ex.getMessage());
        }
    }

    @GetMapping("/week")
    public Map<String, Object> week(@RequestParam Map<String, String> params) {
        try {
            QueryScope scope = queryScope(params);
            return MesApiResponse.ok(calendarService.week(
                    scope.queryType(), params.get("date"), params.get("calendarType"),
                    scope.teamId(), scope.userId()));
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.badRequest(ex.getMessage());
        }
    }

    private String queryType(Map<String, String> params) {
        return params.getOrDefault("queryType", "TYPE").trim().toUpperCase();
    }

    private QueryScope queryScope(Map<String, String> params) {
        String queryType = queryType(params);
        Long teamId = parseLong(params.get("teamId"));
        Long userId = parseLong(params.get("userId"));
        CalCalendarService.requireValidQuery(
                queryType, params.get("calendarType"), teamId, userId);
        return new QueryScope(queryType, teamId, userId);
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return Long.parseLong(v.trim());
    }

    private record QueryScope(String queryType, Long teamId, Long userId) {
    }
}
