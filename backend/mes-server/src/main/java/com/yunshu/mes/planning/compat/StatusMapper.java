package com.yunshu.mes.planning.compat;

/** 云枢 work_order.status ↔ MES API ProWorkorder.status 映射。 */
public final class StatusMapper {

    private StatusMapper() {}

    public static String toApiWorkorderStatus(String yunshuStatus) {
        if (yunshuStatus == null || yunshuStatus.isBlank()) {
            return "PREPARE";
        }
        return switch (yunshuStatus) {
            case "CREATED" -> "PREPARE";
            case "CONFIRMED", "DISPATCHED", "RUNNING" -> "CONFIRMED";
            case "COMPLETED" -> "FINISHED";
            case "CANCELLED" -> "CANCELED";
            default -> yunshuStatus;
        };
    }

    public static String toYunshuWorkorderStatus(String apiStatus) {
        if (apiStatus == null || apiStatus.isBlank()) {
            return "CREATED";
        }
        return switch (apiStatus) {
            case "PREPARE" -> "CREATED";
            case "CONFIRMED" -> "CONFIRMED";
            case "FINISHED" -> "COMPLETED";
            case "CANCELED" -> "CANCELLED";
            default -> apiStatus;
        };
    }
}
