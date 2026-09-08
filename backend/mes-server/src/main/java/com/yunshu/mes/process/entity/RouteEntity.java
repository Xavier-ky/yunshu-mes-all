package com.yunshu.mes.process.entity;

import java.time.LocalDateTime;

/**
 * 工艺路线实体 — 对应 process_route 表。
 */
public class RouteEntity {

    private Long routeId;
    private String routeCode;
    private String routeName;
    private String routeVersion;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RouteEntity() {
    }

    public RouteEntity(Long routeId, String routeCode, String routeName, String routeVersion,
                       String status,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.routeId = routeId;
        this.routeCode = routeCode;
        this.routeName = routeName;
        this.routeVersion = routeVersion;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getRouteVersion() { return routeVersion; }
    public void setRouteVersion(String routeVersion) { this.routeVersion = routeVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
