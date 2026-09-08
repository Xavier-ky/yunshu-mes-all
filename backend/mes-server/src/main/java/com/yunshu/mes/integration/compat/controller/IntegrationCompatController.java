package com.yunshu.mes.integration.compat.controller;

import com.yunshu.mes.integration.dto.ExternalSystemRequest;
import com.yunshu.mes.integration.repository.ApiEndpointRepository;
import com.yunshu.mes.integration.repository.ExternalSystemRepository;
import com.yunshu.mes.integration.repository.SyncLogRepository;
import com.yunshu.mes.integration.vo.ApiEndpointVO;
import com.yunshu.mes.integration.vo.ExternalSystemVO;
import com.yunshu.mes.integration.vo.SyncLogVO;
import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 接口集成 — MesApiResponse compat。 */
@RestController
@RequestMapping("/api/integration")
public class IntegrationCompatController {

    private final ExternalSystemRepository systemRepo;
    private final ApiEndpointRepository endpointRepo;
    private final SyncLogRepository syncLogRepo;

    public IntegrationCompatController(ExternalSystemRepository systemRepo,
                                       ApiEndpointRepository endpointRepo,
                                       SyncLogRepository syncLogRepo) {
        this.systemRepo = systemRepo;
        this.endpointRepo = endpointRepo;
        this.syncLogRepo = syncLogRepo;
    }

    @GetMapping("/systems/list")
    public Map<String, Object> listSystems(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = systemRepo.search(params, SysCompatHelper.offset(pn, ps), ps).stream()
                .map(this::systemRow).toList();
        return MesApiResponse.table(rows, systemRepo.count(params));
    }

    @GetMapping("/systems/{id}")
    public Map<String, Object> getSystem(@PathVariable Long id) {
        return systemRepo.findById(id).map(vo -> MesApiResponse.ok(systemRow(vo)))
                .orElseGet(() -> MesApiResponse.error("外部系统不存在"));
    }

    @PostMapping("/systems")
    public Map<String, Object> createSystem(@Valid @RequestBody ExternalSystemRequest req) {
        Long id = systemRepo.insert(req.systemCode(), req.systemName(), req.systemType(), req.status());
        return systemRepo.findById(id).map(vo -> MesApiResponse.ok(systemRow(vo)))
                .orElseGet(() -> MesApiResponse.error("创建失败"));
    }

    @PutMapping("/systems/{id}")
    public Map<String, Object> updateSystem(@PathVariable Long id, @Valid @RequestBody ExternalSystemRequest req) {
        systemRepo.update(id, req.systemCode(), req.systemName(), req.systemType(), req.status());
        return systemRepo.findById(id).map(vo -> MesApiResponse.ok(systemRow(vo)))
                .orElseGet(() -> MesApiResponse.error("外部系统不存在"));
    }

    @DeleteMapping("/systems/{id}")
    public Map<String, Object> deleteSystem(@PathVariable Long id) {
        systemRepo.delete(id);
        return MesApiResponse.ok();
    }

    @GetMapping("/endpoints/list")
    public Map<String, Object> listEndpoints(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = endpointRepo.search(params, SysCompatHelper.offset(pn, ps), ps).stream()
                .map(this::endpointRow).toList();
        return MesApiResponse.table(rows, endpointRepo.count(params));
    }

    @GetMapping("/sync-logs/list")
    public Map<String, Object> listSyncLogs(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        List<Map<String, Object>> rows = syncLogRepo.search(params, SysCompatHelper.offset(pn, ps), ps).stream()
                .map(this::syncLogRow).toList();
        return MesApiResponse.table(rows, syncLogRepo.count(params));
    }

    private Map<String, Object> systemRow(ExternalSystemVO vo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("systemId", vo.systemId());
        m.put("systemCode", vo.systemCode());
        m.put("systemName", vo.systemName());
        m.put("systemType", vo.systemType());
        m.put("status", vo.status());
        return m;
    }

    private Map<String, Object> endpointRow(ApiEndpointVO vo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("endpointId", vo.endpointId());
        m.put("endpointCode", vo.endpointCode());
        m.put("endpointName", vo.endpointName());
        m.put("endpointPath", vo.apiPath());
        m.put("apiPath", vo.apiPath());
        m.put("httpMethod", vo.httpMethod());
        m.put("direction", vo.direction());
        m.put("status", vo.status());
        return m;
    }

    private Map<String, Object> syncLogRow(SyncLogVO vo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("syncLogId", vo.syncLogId());
        m.put("syncType", vo.syncType());
        m.put("syncKey", vo.bizNo());
        m.put("bizNo", vo.bizNo());
        m.put("syncStatus", vo.syncStatus());
        m.put("syncTime", vo.syncTime());
        return m;
    }
}
