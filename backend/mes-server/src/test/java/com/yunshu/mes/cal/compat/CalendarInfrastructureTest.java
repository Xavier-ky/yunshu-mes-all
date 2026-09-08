package com.yunshu.mes.cal.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarInfrastructureTest {

    private static final Path CONTROLLERS = Path.of(
            "src/main/java/com/yunshu/mes/cal/compat/controller");

    @Test
    void calendarControllersUseMetadataForEmptyTableDetection() throws IOException {
        for (String file : List.of(
                "CalTeamController.java",
                "CalTeammemberController.java",
                "CalPlanteamController.java",
                "CalShiftController.java",
                "CalHolidayController.java")) {
            String source = Files.readString(CONTROLLERS.resolve(file));
            assertTrue(source.contains("information_schema.tables"), file);
            assertFalse(source.contains("SELECT 1 FROM \" + table + \" LIMIT 1"), file);
        }
    }

    @Test
    void calendarTeamMemberSeedUsesExistenceChecks() throws IOException {
        String seed = Files.readString(Path.of(
                "src/main/resources/db/seed/R__seed_story_06_calendar_system.sql"));

        assertFalse(seed.contains("INSERT IGNORE INTO cal_team_member"));
        assertTrue(seed.contains("FROM cal_team_member tm"));
        assertTrue(seed.contains("tm.team_id = t.team_id AND tm.user_id = u.user_id"));
    }

    @Test
    void schedulingServicesShareWorkdayPolicyAndLoadPriorOvernightRows() throws IOException {
        String teamshift = Files.readString(Path.of(
                "src/main/java/com/yunshu/mes/cal/compat/service/CalTeamshiftService.java"));
        String calendar = Files.readString(Path.of(
                "src/main/java/com/yunshu/mes/cal/compat/service/CalCalendarService.java"));

        assertTrue(teamshift.contains("CalWorkdayPolicy.isWorkday"));
        assertTrue(calendar.contains("CalWorkdayPolicy.isWorkday"));
        assertTrue(calendar.contains("CalScheduleConflictDetector.countConflicts"));
        assertFalse(calendar.contains("GROUP BY f.the_day, ctm.user_id"));
        assertTrue(teamshift.contains("start.minusDays(1).format(DAY_FMT)"));
        assertTrue(teamshift.contains("end.plusDays(1).format(DAY_FMT)"));
    }

    @Test
    void webConfigurationAppliesMutationAuthorizationToEntireCalendarApi() throws IOException {
        String config = Files.readString(Path.of(
                "src/main/java/com/yunshu/mes/config/WebMvcConfig.java"));

        assertTrue(config.contains("CalMutationAuthorizationInterceptor"));
        assertTrue(config.contains("addPathPatterns(\"/api/mes/cal/**\")"));
    }
}
