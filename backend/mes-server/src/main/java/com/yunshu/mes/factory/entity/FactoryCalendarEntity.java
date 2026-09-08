package com.yunshu.mes.factory.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工厂日历实体 — 对应 factory_calendar 表。
 */
public class FactoryCalendarEntity {

    private Long calendarId;
    private LocalDate calendarDate;
    private Long shiftId;
    private Boolean isWorkday;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FactoryCalendarEntity() {
    }

    public Long getCalendarId() { return calendarId; }
    public void setCalendarId(Long calendarId) { this.calendarId = calendarId; }

    public LocalDate getCalendarDate() { return calendarDate; }
    public void setCalendarDate(LocalDate calendarDate) { this.calendarDate = calendarDate; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Boolean getIsWorkday() { return isWorkday; }
    public void setIsWorkday(Boolean isWorkday) { this.isWorkday = isWorkday; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
