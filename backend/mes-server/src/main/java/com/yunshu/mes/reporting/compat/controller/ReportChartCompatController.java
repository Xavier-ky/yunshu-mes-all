package com.yunshu.mes.reporting.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.reporting.compat.repository.ReportChartCompatRepository;
import com.yunshu.mes.system.compat.SysCompatHelper;
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

@RestController
@RequestMapping("/api/mes/report/chart")
public class ReportChartCompatController {

    private final ReportChartCompatRepository repo;

    public ReportChartCompatController(ReportChartCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(repo.search(params, SysCompatHelper.offset(pn, ps), ps), repo.count(params));
    }

    @GetMapping("/getMyCharts")
    public Map<String, Object> getMyCharts() {
        Object rolesObj = SysCompatHelper.currentUserId() == null ? List.of() : null;
        List<Long> roleIds = List.of();
        if (rolesObj == null) {
            // fallback: all enabled charts for demo
            roleIds = repo.search(Map.of(), 0, 100).stream()
                    .map(r -> SysCompatHelper.longVal(r.get("chartId")))
                    .toList();
        }
        return MesApiResponse.ok(repo.getMyCharts(roleIds.isEmpty() ? List.of(1L, 2L) : roleIds));
    }

    @GetMapping("/{chartId}")
    public Map<String, Object> getInfo(@PathVariable Long chartId) {
        return repo.findById(chartId).map(row -> {
            Map<String, Object> data = new LinkedHashMap<>(row);
            data.put("roleIds", repo.roleIds(chartId));
            Map<String, Object> resp = MesApiResponse.ok(data);
            return resp;
        }).orElseGet(() -> MesApiResponse.error("图表不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = repo.insert(body);
        if (id != null) {
            repo.replaceRoles(id, body.get("roleIds"));
            body.put("chartId", id);
        }
        return MesApiResponse.ok(body);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        int rows = repo.update(body);
        repo.replaceRoles(SysCompatHelper.longVal(body.get("chartId")), body.get("roleIds"));
        return MesApiResponse.toAjax(rows);
    }

    @DeleteMapping("/{chartIds}")
    public Map<String, Object> remove(@PathVariable String chartIds) {
        return MesApiResponse.toAjax(repo.deleteByIds(SysCompatHelper.parseIds(chartIds)));
    }
}
