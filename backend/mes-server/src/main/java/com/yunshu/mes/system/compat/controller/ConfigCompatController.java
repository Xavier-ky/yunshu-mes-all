package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.ConfigCompatRepository;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
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
@RequestMapping("/api/system/config")
public class ConfigCompatController {

    private final ConfigCompatRepository configRepo;

    public ConfigCompatController(ConfigCompatRepository configRepo) {
        this.configRepo = configRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(configRepo.search(params, SysCompatHelper.offset(pn, ps), ps), configRepo.count(params));
    }

    @GetMapping("/{configId}")
    public Map<String, Object> getInfo(@PathVariable Integer configId) {
        return configRepo.findById(configId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("参数不存在"));
    }

    @GetMapping("/configKey/{configKey}")
    public Map<String, Object> getByKey(@PathVariable String configKey) {
        return configRepo.findValueByKey(configKey)
                .map(v -> {
                    Map<String, Object> m = MesApiResponse.ok();
                    m.put("msg", v);
                    return m;
                })
                .orElseGet(() -> MesApiResponse.error("参数不存在"));
    }

    @OperLog(title = "参数管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String configKey = SysCompatHelper.str(body.get("configKey"));
        if (configRepo.configKeyExists(configKey, null)) {
            return MesApiResponse.error("新增参数'" + configKey + "'失败，参数键名已存在");
        }
        return MesApiResponse.ok(configRepo.insert(body));
    }

    @OperLog(title = "参数管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Integer configId = SysCompatHelper.intObj(body.get("configId"));
        String configKey = SysCompatHelper.str(body.get("configKey"));
        if (StringUtils.hasText(configKey) && configRepo.configKeyExists(configKey, configId)) {
            return MesApiResponse.error("修改参数失败，参数键名已存在");
        }
        return MesApiResponse.toAjax(configRepo.update(body));
    }

    @OperLog(title = "参数管理", businessType = 3)
    @DeleteMapping("/{configIds}")
    public Map<String, Object> remove(@PathVariable String configIds) {
        List<Integer> ids = SysCompatHelper.parseIds(configIds).stream().map(Long::intValue).toList();
        return MesApiResponse.toAjax(configRepo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
