package com.yunshu.mes.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MaterialRequisitionVO {
    private Long requisitionId;
    private String requisitionNo;
    private Long workOrderId;
    private String workOrderNo;
    private Long requestUserId;
    private String requestUserName;
    private LocalDateTime requestTime;
    private String status;
    private String remark;
    private List<RequisitionItemVO> items;

    public MaterialRequisitionVO() {}

    public MaterialRequisitionVO(Long requisitionId, String requisitionNo, Long workOrderId, String workOrderNo,
                                  Long requestUserId, String requestUserName, LocalDateTime requestTime,
                                  String status, String remark) {
        this.requisitionId = requisitionId; this.requisitionNo = requisitionNo;
        this.workOrderId = workOrderId; this.workOrderNo = workOrderNo;
        this.requestUserId = requestUserId; this.requestUserName = requestUserName;
        this.requestTime = requestTime; this.status = status; this.remark = remark;
    }

    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long v) { this.requisitionId = v; }
    public String getRequisitionNo() { return requisitionNo; }
    public void setRequisitionNo(String v) { this.requisitionNo = v; }
    public Long getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(Long v) { this.workOrderId = v; }
    public String getWorkOrderNo() { return workOrderNo; }
    public void setWorkOrderNo(String v) { this.workOrderNo = v; }
    public Long getRequestUserId() { return requestUserId; }
    public void setRequestUserId(Long v) { this.requestUserId = v; }
    public String getRequestUserName() { return requestUserName; }
    public void setRequestUserName(String v) { this.requestUserName = v; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime v) { this.requestTime = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public List<RequisitionItemVO> getItems() { return items; }
    public void setItems(List<RequisitionItemVO> v) { this.items = v; }

    public static class RequisitionItemVO {
        private Long requisitionItemId;
        private Long requisitionId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal requestQty;
        private BigDecimal approvedQty;
        private BigDecimal issuedQty;

        public Long getRequisitionItemId() { return requisitionItemId; }
        public void setRequisitionItemId(Long v) { this.requisitionItemId = v; }
        public Long getRequisitionId() { return requisitionId; }
        public void setRequisitionId(Long v) { this.requisitionId = v; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long v) { this.materialId = v; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String v) { this.materialCode = v; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String v) { this.materialName = v; }
        public BigDecimal getRequestQty() { return requestQty; }
        public void setRequestQty(BigDecimal v) { this.requestQty = v; }
        public BigDecimal getApprovedQty() { return approvedQty; }
        public void setApprovedQty(BigDecimal v) { this.approvedQty = v; }
        public BigDecimal getIssuedQty() { return issuedQty; }
        public void setIssuedQty(BigDecimal v) { this.issuedQty = v; }
    }
}
