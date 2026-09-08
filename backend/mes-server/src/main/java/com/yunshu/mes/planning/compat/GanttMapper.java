package com.yunshu.mes.planning.compat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GanttMapper {

    private static final DateTimeFormatter GANTT_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GanttMapper() {}

    public static Map<String, Object> toTaskRow(
            String id,
            long workOrderId,
            String text,
            String product,
            BigDecimal quantity,
            BigDecimal completed,
            String process,
            String workstation,
            Timestamp start,
            Integer durationMinutes,
            String color) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", "task");
        node.put("text", text);
        node.put("product", product);
        node.put("quantity", quantity);
        node.put("progress", progress(completed, quantity));
        node.put("process", process);
        node.put("workstation", workstation);
        node.put("parent", "MO" + workOrderId);
        node.put("start_date", formatGanttDate(start));
        node.put("duration", durationHours(durationMinutes));
        if (color != null) {
            node.put("color", color);
        }
        return node;
    }

    public static float progress(BigDecimal done, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return 0f;
        }
        BigDecimal d = done == null ? BigDecimal.ZERO : done;
        return d.divide(total, 4, RoundingMode.HALF_UP).floatValue();
    }

    public static long durationHours(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return 8L;
        }
        return Math.max(1L, (durationMinutes + 59L) / 60L);
    }

    private static String formatGanttDate(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().format(GANTT_DT);
    }
}
