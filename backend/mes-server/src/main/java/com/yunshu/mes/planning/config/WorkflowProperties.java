package com.yunshu.mes.planning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 生产流程打通配置，绑定 {@code mes.workflow}。
 */
@ConfigurationProperties(prefix = "mes.workflow")
public class WorkflowProperties {

    /** 为 true 时后端拦截越权操作（领料/报工/入库等）；默认 false 仅展示状态。 */
    private boolean enforce = false;

    /** 排产保存时是否自动生成 dispatch_task。 */
    private boolean autoDispatch = true;

    public boolean isEnforce() {
        return enforce;
    }

    public void setEnforce(boolean enforce) {
        this.enforce = enforce;
    }

    public boolean isAutoDispatch() {
        return autoDispatch;
    }

    public void setAutoDispatch(boolean autoDispatch) {
        this.autoDispatch = autoDispatch;
    }
}
