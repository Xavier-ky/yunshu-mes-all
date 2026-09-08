package com.yunshu.mes.integration.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.integration.dto.ExternalSystemRequest;
import com.yunshu.mes.integration.repository.ApiEndpointRepository;
import com.yunshu.mes.integration.repository.ExternalSystemRepository;
import com.yunshu.mes.integration.repository.SyncLogRepository;
import com.yunshu.mes.integration.service.IntegrationMockDataService;
import com.yunshu.mes.integration.service.IntegrationService;
import com.yunshu.mes.integration.vo.ApiEndpointVO;
import com.yunshu.mes.integration.vo.ExternalSystemVO;
import com.yunshu.mes.integration.vo.SyncLogVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 接口集成域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class IntegrationServiceImpl implements IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationServiceImpl.class);

    private final ExternalSystemRepository systemRepo;
    private final ApiEndpointRepository endpointRepo;
    private final SyncLogRepository syncLogRepo;
    private final IntegrationMockDataService mock;

    public IntegrationServiceImpl(ExternalSystemRepository systemRepo,
                                  ApiEndpointRepository endpointRepo,
                                  SyncLogRepository syncLogRepo,
                                  IntegrationMockDataService mock) {
        this.systemRepo = systemRepo;
        this.endpointRepo = endpointRepo;
        this.syncLogRepo = syncLogRepo;
        this.mock = mock;
    }

    // ==================== 外部系统 ====================

    @Override
    public List<ExternalSystemVO> listSystems() {
        try { return systemRepo.findAll(); }
        catch (DataAccessException e) { log.warn("外部系统列表回退 Mock：{}", e.getMessage()); return mock.listSystems(); }
    }

    @Override
    public Optional<ExternalSystemVO> getSystemById(Long id) {
        try { return systemRepo.findById(id).or(() -> mock.getSystemById(id)); }
        catch (DataAccessException e) { log.warn("外部系统详情回退 Mock：{}", e.getMessage()); return mock.getSystemById(id); }
    }

    @Override
    public ExternalSystemVO createSystem(ExternalSystemRequest req) {
        try {
            Long id = systemRepo.insert(req.systemCode(), req.systemName(), req.systemType(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建外部系统失败");
            return systemRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建外部系统后回查失败"));
        } catch (DataAccessException e) { log.warn("创建外部系统回退 Mock：{}", e.getMessage()); return mock.createSystem(req); }
    }

    @Override
    public ExternalSystemVO updateSystem(Long id, ExternalSystemRequest req) {
        try {
            systemRepo.update(id, req.systemCode(), req.systemName(), req.systemType(), req.status());
            return systemRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "外部系统不存在"));
        } catch (DataAccessException e) { log.warn("更新外部系统回退 Mock：{}", e.getMessage()); return mock.updateSystem(id, req); }
    }

    @Override
    public void deleteSystem(Long id) {
        try { systemRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除外部系统回退 Mock：{}", e.getMessage()); mock.deleteSystem(id); }
    }

    // ==================== 接口定义 ====================

    @Override
    public List<ApiEndpointVO> listEndpoints() {
        try { return endpointRepo.findAll(); }
        catch (DataAccessException e) { log.warn("接口定义列表回退 Mock：{}", e.getMessage()); return mock.listEndpoints(); }
    }

    // ==================== 同步日志 ====================

    @Override
    public List<SyncLogVO> listSyncLogs() {
        try { return syncLogRepo.findAll(); }
        catch (DataAccessException e) { log.warn("同步日志列表回退 Mock：{}", e.getMessage()); return mock.listSyncLogs(); }
    }
}
