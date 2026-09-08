package com.yunshu.mes.masterdata.vo;

import java.math.BigDecimal;

public class BomItemVO {
    private Long bomItemId;
    private Long bomId;
    private Long materialId;
    private String materialName;
    private String materialCode;
    private BigDecimal qtyPer;
    private BigDecimal lossRate;
    private Boolean isKeyMaterial;
    private String remark;

    public BomItemVO() {}

    public BomItemVO(Long bomItemId, Long bomId, Long materialId, String materialName, String materialCode,
                     BigDecimal qtyPer, BigDecimal lossRate, Boolean isKeyMaterial, String remark) {
        this.bomItemId = bomItemId; this.bomId = bomId; this.materialId = materialId;
        this.materialName = materialName; this.materialCode = materialCode;
        this.qtyPer = qtyPer; this.lossRate = lossRate; this.isKeyMaterial = isKeyMaterial; this.remark = remark;
    }

    public Long getBomItemId() { return bomItemId; }
    public void setBomItemId(Long v) { this.bomItemId = v; }
    public Long getBomId() { return bomId; }
    public void setBomId(Long v) { this.bomId = v; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long v) { this.materialId = v; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String v) { this.materialName = v; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String v) { this.materialCode = v; }
    public BigDecimal getQtyPer() { return qtyPer; }
    public void setQtyPer(BigDecimal v) { this.qtyPer = v; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal v) { this.lossRate = v; }
    public Boolean getIsKeyMaterial() { return isKeyMaterial; }
    public void setIsKeyMaterial(Boolean v) { this.isKeyMaterial = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
}
