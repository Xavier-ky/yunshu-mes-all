package com.yunshu.mes.integration.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.integration.dto.ExternalSystemRequest;
import com.yunshu.mes.integration.vo.ApiEndpointVO;
import com.yunshu.mes.integration.vo.ExternalSystemVO;
import com.yunshu.mes.integration.vo.SyncLogVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 接口集成域 Mock 实现，与 seed SQL 保持一致。写操作在无库时不可用。
 */
@Service
public class IntegrationMockDataService implements IntegrationService {

    private static final List<ExternalSystemVO> SYSTEMS = new ArrayList<>(List.of(
            new ExternalSystemVO(1L, "ERP", "企业资源计划系统", "BUSINESS", "ENABLED"),
            new ExternalSystemVO(2L, "WMS", "仓储管理系统", "LOGISTICS", "ENABLED")
    ));

    private final AtomicLong systemNextId = new AtomicLong(10);

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public IntegrationMockDataService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    // ---- 外部系统 ----
    @Override
    public List<ExternalSystemVO> listSystems() {
        return new ArrayList<>(SYSTEMS);
    }

    @Override
    public Optional<ExternalSystemVO> getSystemById(Long id) {
        return SYSTEMS.stream().filter(s -> s.systemId().equals(id)).findFirst();
    }

    @Override
    public ExternalSystemVO createSystem(ExternalSystemRequest req) {
        throw mockWriteError();
    }

    @Override
    public ExternalSystemVO updateSystem(Long id, ExternalSystemRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteSystem(Long id) {
        throw mockWriteError();
    }

    // ---- 接口定义 ----
    @Override
    public List<ApiEndpointVO> listEndpoints() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return List.of();
        }
        return jdbc.query("""
                SELECT endpoint_id, endpoint_code, endpoint_name, api_path, http_method, direction, status
                FROM api_endpoint ORDER BY endpoint_id
                """, (rs, n) -> new ApiEndpointVO(
                rs.getLong("endpoint_id"),
                rs.getString("endpoint_code"),
                rs.getString("endpoint_name"),
                rs.getString("api_path"),
                rs.getString("http_method"),
                rs.getString("direction"),
                rs.getString("status")));
    }

    // ---- 同步日志 ----
    @Override
    public List<SyncLogVO> listSyncLogs() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return List.of();
        }
        return jdbc.query("""
                SELECT sync_log_id, sync_type, biz_no, sync_status, sync_time
                FROM sync_log ORDER BY sync_time DESC
                """, (rs, n) -> new SyncLogVO(
                rs.getLong("sync_log_id"),
                rs.getString("sync_type"),
                rs.getString("biz_no"),
                rs.getString("sync_status"),
                rs.getString("sync_time")));
    }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
