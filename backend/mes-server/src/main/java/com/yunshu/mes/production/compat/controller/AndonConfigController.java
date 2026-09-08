package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.production.compat.repository.ProAndonConfigRepository;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/pro/andonconfig")
public class AndonConfigController {

    private final ProAndonConfigRepository repo;

    public AndonConfigController(ProAndonConfigRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        return MesApiResponse.ok(repo.listAll());
    }

    @GetMapping("/{configId}")
    public Map<String, Object> getInfo(@PathVariable Long configId) {
        return repo.findById(configId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("安灯呼叫配置不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(repo.insert(body));
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody List<Map<String, Object>> configs) {
        if (configs != null) {
            for (Map<String, Object> cfg : configs) {
                Object id = cfg.get("configId");
                if (id == null) {
                    repo.insert(cfg);
                } else {
                    repo.update(cfg);
                }
            }
        }
        return MesApiResponse.ok();
    }

    @DeleteMapping("/{configIds}")
    public Map<String, Object> remove(@PathVariable Long configIds) {
        return MesApiResponse.toAjax(repo.deleteById(configIds));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
