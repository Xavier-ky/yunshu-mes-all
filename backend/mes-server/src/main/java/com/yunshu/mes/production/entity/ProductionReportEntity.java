package com.yunshu.mes.production.entity;

import java.time.LocalDateTime;

/**
 * 生产报工实体 — 对应 production_report 表。
 */
public class ProductionReportEntity {

    private Long reportId;
    private String reportNo;
    private Long dispatchId;
    private Long workOrderId;
    private Long stepId;
    private Long stationId;
    private Long operatorId;
    private Long snId;
    private String reportType;
    private String goodQty;
    private String defectQty;
    private LocalDateTime reportTime;
    private String remark;

    public ProductionReportEntity() {
    }

    public ProductionReportEntity(Long reportId, String reportNo, Long dispatchId, Long workOrderId,
                                   Long stepId, Long stationId, Long operatorId, Long snId,
                                   String reportType, String goodQty, String defectQty,
                                   LocalDateTime reportTime, String remark) {
        this.reportId = reportId;
        this.reportNo = reportNo;
        this.dispatchId = dispatchId;
        this.workOrderId = workOrderId;
        this.stepId = stepId;
        this.stationId = stationId;
        this.operatorId = operatorId;
        this.snId = snId;
        this.reportType = reportType;
        this.goodQty = goodQty;
        this.defectQty = defectQty;
        this.reportTime = reportTime;
        this.remark = remark;
    }

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }

    public Long getDispatchId() { return dispatchId; }
    public void setDispatchId(Long dispatchId) { this.dispatchId = dispatchId; }

    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long workOrderId) { this.workOrderId = workOrderId; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public Long getSnId() { return snId; }
    public void setSnId(Long snId) { this.snId = snId; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getGoodQty() { return goodQty; }
    public void setGoodQty(String goodQty) { this.goodQty = goodQty; }

    public String getDefectQty() { return defectQty; }
    public void setDefectQty(String defectQty) { this.defectQty = defectQty; }

    public LocalDateTime getReportTime() { return reportTime; }
    public void setReportTime(LocalDateTime reportTime) { this.reportTime = reportTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
