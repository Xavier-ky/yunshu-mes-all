package com.yunshu.mes.process.vo;

public class ProcessRouteStepVO {
    private Long routeStepId;
    private Long routeId;
    private Long stepId;
    private String stepCode;
    private String stepName;
    private Integer stepSeq;
    private String stationType;
    private Boolean isMustPass;

    public ProcessRouteStepVO() {}

    public ProcessRouteStepVO(Long routeStepId, Long routeId, Long stepId, String stepCode, String stepName,
                              Integer stepSeq, String stationType, Boolean isMustPass) {
        this.routeStepId = routeStepId; this.routeId = routeId; this.stepId = stepId;
        this.stepCode = stepCode; this.stepName = stepName;
        this.stepSeq = stepSeq; this.stationType = stationType; this.isMustPass = isMustPass;
    }

    public Long getRouteStepId() { return routeStepId; }
    public void setRouteStepId(Long v) { this.routeStepId = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public Long getStepId() { return stepId; }
    public void setStepId(Long v) { this.stepId = v; }
    public String getStepCode() { return stepCode; }
    public void setStepCode(String v) { this.stepCode = v; }
    public String getStepName() { return stepName; }
    public void setStepName(String v) { this.stepName = v; }
    public Integer getStepSeq() { return stepSeq; }
    public void setStepSeq(Integer v) { this.stepSeq = v; }
    public String getStationType() { return stationType; }
    public void setStationType(String v) { this.stationType = v; }
    public Boolean getIsMustPass() { return isMustPass; }
    public void setIsMustPass(Boolean v) { this.isMustPass = v; }
}
