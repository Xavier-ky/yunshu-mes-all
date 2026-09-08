package com.yunshu.mes.integration.service;

import com.yunshu.mes.integration.dto.ExternalSystemRequest;
import com.yunshu.mes.integration.vo.ApiEndpointVO;
import com.yunshu.mes.integration.vo.ExternalSystemVO;
import com.yunshu.mes.integration.vo.SyncLogVO;
import java.util.List;
import java.util.Optional;

/**
 * 接口集成域服务接口：外部系统、接口定义、同步日志。
 */
public interface IntegrationService {

    // ---- 外部系统 ----
    List<ExternalSystemVO> listSystems();

    Optional<ExternalSystemVO> getSystemById(Long id);

    ExternalSystemVO createSystem(ExternalSystemRequest req);

    ExternalSystemVO updateSystem(Long id, ExternalSystemRequest req);

    void deleteSystem(Long id);

    // ---- 接口定义 ----
    List<ApiEndpointVO> listEndpoints();

    // ---- 同步日志 ----
    List<SyncLogVO> listSyncLogs();
}
