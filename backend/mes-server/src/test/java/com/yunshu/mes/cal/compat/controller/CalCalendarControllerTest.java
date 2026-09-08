package com.yunshu.mes.cal.compat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yunshu.mes.cal.compat.service.CalCalendarService;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CalCalendarControllerTest {

    @ParameterizedTest
    @MethodSource("invalidQueries")
    void everyCalendarQueryRejectsInvalidScopeBeforeServiceCall(
            Function<CalCalendarController, Map<String, Object>> call, String expectedMessage) {
        CalCalendarService service = mock(CalCalendarService.class);
        CalCalendarController controller = new CalCalendarController(service);

        Map<String, Object> response = call.apply(controller);

        assertEquals(400, response.get("code"));
        assertEquals(expectedMessage, response.get("msg"));
        verifyNoInteractions(service);
    }

    @Test
    void validTeamQueryPreservesExistingResponseShape() {
        CalCalendarService service = mock(CalCalendarService.class);
        CalCalendarController controller = new CalCalendarController(service);

        Map<String, Object> response = controller.day(Map.of(
                "queryType", "TEAM", "teamId", "11", "date", "2026-07-09"));

        assertEquals(200, response.get("code"));
    }

    private static Stream<Arguments> invalidQueries() {
        return Stream.of(
                Arguments.of(
                        (Function<CalCalendarController, Map<String, Object>>) controller ->
                                controller.list(Map.of("queryType", "TYPE")),
                        "TYPE查询必须提供calendarType"),
                Arguments.of(
                        (Function<CalCalendarController, Map<String, Object>>) controller ->
                                controller.summary(Map.of("queryType", "TEAM")),
                        "TEAM查询必须提供teamId"),
                Arguments.of(
                        (Function<CalCalendarController, Map<String, Object>>) controller ->
                                controller.day(Map.of("queryType", "USER")),
                        "USER查询必须提供userId"),
                Arguments.of(
                        (Function<CalCalendarController, Map<String, Object>>) controller ->
                                controller.week(Map.of("queryType", "ALL")),
                        "不支持的queryType: ALL"));
    }
}
