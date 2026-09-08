package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.AutocodePartCompatRepository;
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
@RequestMapping("/api/system/autocode/part")
public class AutocodePartCompatController {

    private final AutocodePartCompatRepository partRepo;

    public AutocodePartCompatController(AutocodePartCompatRepository partRepo) {
        this.partRepo = partRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(partRepo.search(params, SysCompatHelper.offset(pn, ps), ps), partRepo.count(params));
    }

    @GetMapping("/{partId}")
    public Map<String, Object> getInfo(@PathVariable Long partId) {
        return partRepo.findById(partId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("分段不存在"));
    }

    @OperLog(title = "编码规则组成", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(partRepo.insert(body));
    }

    @OperLog(title = "编码规则组成", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(partRepo.update(body));
    }

    @OperLog(title = "编码规则组成", businessType = 3)
    @DeleteMapping("/{partIds}")
    public Map<String, Object> remove(@PathVariable String partIds) {
        return MesApiResponse.toAjax(partRepo.deleteByIds(SysCompatHelper.parseIds(partIds)));
    }
}
