package com.yunshu.mes.cal.compat.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class CalScheduleConflictDetector {

    private CalScheduleConflictDetector() {
    }

    static String detect(List<Assignment> candidate, List<Assignment> existing,
            Map<Long, Set<Long>> teamMembers, Map<Long, String> memberNames) {
        List<Assignment> eligibleExisting = existing.stream()
                .filter(Assignment::isEnabledConfirmed)
                .toList();

        for (Assignment next : candidate) {
            for (Assignment current : eligibleExisting) {
                ConflictType type = conflictType(next, current, teamMembers);
                if (type == ConflictType.SAME_TEAM_DAY) {
                    return "排班冲突：" + next.teamName() + "在 " + next.day()
                            + " 已由计划“" + current.planName() + "”排班";
                }
                if (type == ConflictType.SAME_TEAM_OVERLAP) {
                    return shiftConflict(next, current);
                }
                if (type == ConflictType.MEMBER_OVERLAP) {
                    return memberConflict(next, current, teamMembers, memberNames);
                }
            }
        }

        for (int i = 0; i < candidate.size(); i++) {
            for (int j = i + 1; j < candidate.size(); j++) {
                Assignment first = candidate.get(i);
                Assignment second = candidate.get(j);
                ConflictType type = conflictType(first, second, teamMembers);
                if (type == ConflictType.SAME_TEAM_DAY
                        || type == ConflictType.SAME_TEAM_OVERLAP) {
                    return shiftConflict(first, second);
                }
                if (type == ConflictType.MEMBER_OVERLAP) {
                    return memberConflict(first, second, teamMembers, memberNames);
                }
            }
        }
        return null;
    }

    static int countConflicts(List<Assignment> assignments,
            Map<Long, Set<Long>> teamMembers, LocalDate scopeStart, LocalDate scopeEnd) {
        return countConflicts(
                assignments, teamMembers, scopeStart, scopeEnd, ignored -> true);
    }

    static int countConflicts(List<Assignment> assignments,
            Map<Long, Set<Long>> teamMembers, LocalDate scopeStart, LocalDate scopeEnd,
            Predicate<Assignment> scope) {
        List<Assignment> eligible = assignments.stream()
                .filter(Assignment::isEnabledConfirmed)
                .toList();
        int conflicts = 0;
        for (int i = 0; i < eligible.size(); i++) {
            for (int j = i + 1; j < eligible.size(); j++) {
                Assignment first = eligible.get(i);
                Assignment second = eligible.get(j);
                if (!inScope(first, scopeStart, scopeEnd)
                        && !inScope(second, scopeStart, scopeEnd)) {
                    continue;
                }
                if (!scope.test(first) && !scope.test(second)) {
                    continue;
                }
                if (conflictType(first, second, teamMembers) != ConflictType.NONE) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    private static ConflictType conflictType(Assignment first, Assignment second,
            Map<Long, Set<Long>> teamMembers) {
        if (first.day().equals(second.day()) && first.teamId().equals(second.teamId())) {
            return ConflictType.SAME_TEAM_DAY;
        }
        if (!overlaps(first, second)) {
            return ConflictType.NONE;
        }
        if (first.teamId().equals(second.teamId())) {
            return ConflictType.SAME_TEAM_OVERLAP;
        }
        Set<Long> shared = new HashSet<>(teamMembers.getOrDefault(first.teamId(), Set.of()));
        shared.retainAll(teamMembers.getOrDefault(second.teamId(), Set.of()));
        return shared.isEmpty() ? ConflictType.NONE : ConflictType.MEMBER_OVERLAP;
    }

    private static boolean inScope(Assignment assignment, LocalDate start, LocalDate end) {
        return !assignment.day().isBefore(start) && !assignment.day().isAfter(end);
    }

    private enum ConflictType {
        NONE,
        SAME_TEAM_DAY,
        SAME_TEAM_OVERLAP,
        MEMBER_OVERLAP
    }

    private static String shiftConflict(Assignment first, Assignment second) {
        return "班次冲突：" + first.teamName() + "的" + first.shiftName()
                + "与" + second.shiftName() + "时间重叠";
    }

    private static String memberConflict(Assignment first, Assignment second,
            Map<Long, Set<Long>> teamMembers, Map<Long, String> memberNames) {
        if (!overlaps(first, second)) {
            return null;
        }
        Set<Long> shared = new HashSet<>(teamMembers.getOrDefault(first.teamId(), Set.of()));
        shared.retainAll(teamMembers.getOrDefault(second.teamId(), Set.of()));
        if (shared.isEmpty()) {
            return null;
        }
        Long userId = new ArrayList<>(shared).get(0);
        String name = memberNames.getOrDefault(userId, "用户" + userId);
        return "人员冲突：" + name + "在 " + first.day()
                + " 同时分配到" + first.teamName() + "和" + second.teamName();
    }

    static boolean overlaps(LocalTime firstStart, LocalTime firstEnd,
            LocalTime secondStart, LocalTime secondEnd) {
        LocalDate anchor = LocalDate.of(2000, 1, 1);
        return intervalOverlaps(
                anchor.atTime(firstStart),
                endDateTime(anchor, firstStart, firstEnd),
                anchor.atTime(secondStart),
                endDateTime(anchor, secondStart, secondEnd));
    }

    private static boolean overlaps(Assignment first, Assignment second) {
        return intervalOverlaps(
                first.startDateTime(), first.endDateTime(),
                second.startDateTime(), second.endDateTime());
    }

    private static LocalDateTime endDateTime(LocalDate day, LocalTime start, LocalTime end) {
        LocalDate endDay = end.isAfter(start) ? day : day.plusDays(1);
        return endDay.atTime(end);
    }

    private static boolean intervalOverlaps(LocalDateTime firstStart, LocalDateTime firstEnd,
            LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    record Assignment(
            Long planId,
            String planName,
            LocalDate day,
            Long teamId,
            String teamName,
            Long shiftId,
            String shiftName,
            Integer orderNum,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            String enableFlag,
            String calendarType) {

        boolean isEnabledConfirmed() {
            return "CONFIRMED".equals(status) && "Y".equals(enableFlag);
        }

        LocalDateTime startDateTime() {
            return day.atTime(startTime);
        }

        LocalDateTime endDateTime() {
            return CalScheduleConflictDetector.endDateTime(day, startTime, endTime);
        }
    }
}
