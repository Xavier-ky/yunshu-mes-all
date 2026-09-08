package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.AutocodePartCompatRepository;
import com.yunshu.mes.system.compat.repository.AutocodeRuleCompatRepository;
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
@RequestMapping("/api/system/autocode/rule")
public class AutocodeRuleCompatController {

    private final AutocodeRuleCompatRepository ruleRepo;

    public AutocodeRuleCompatController(AutocodeRuleCompatRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(ruleRepo.search(params, SysCompatHelper.offset(pn, ps), ps), ruleRepo.count(params));
    }

    @GetMapping("/{ruleId}")
    public Map<String, Object> getInfo(@PathVariable Long ruleId) {
        return ruleRepo.findById(ruleId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("规则不存在"));
    }

    @OperLog(title = "编码规则", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String ruleCode = SysCompatHelper.str(body.get("ruleCode"));
        if (ruleRepo.ruleCodeExists(ruleCode, null)) {
            return MesApiResponse.error("自动编码规则的编号重复");
        }
        return MesApiResponse.ok(ruleRepo.insert(body));
    }

    @OperLog(title = "编码规则", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long ruleId = SysCompatHelper.longObj(body.get("ruleId"));
        String ruleCode = SysCompatHelper.str(body.get("ruleCode"));
        if (StringUtils.hasText(ruleCode) && ruleRepo.ruleCodeExists(ruleCode, ruleId)) {
            return MesApiResponse.error("自动编码规则的编号重复");
        }
        return MesApiResponse.toAjax(ruleRepo.update(body));
    }

    @OperLog(title = "编码规则", businessType = 3)
    @DeleteMapping("/{ruleIds}")
    public Map<String, Object> remove(@PathVariable String ruleIds) {
        return MesApiResponse.toAjax(ruleRepo.deleteByIds(SysCompatHelper.parseIds(ruleIds)));
    }
}
