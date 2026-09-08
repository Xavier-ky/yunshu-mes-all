package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.repository.DvCheckMachineryRepository;
import com.yunshu.mes.equipment.compat.repository.DvCheckPlanRepository;
import com.yunshu.mes.equipment.compat.repository.DvCheckSubjectRepository;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/mes/dv/checkplan")
public class DvCheckPlanController {

    private final DvCheckPlanRepository planRepo;
    private final DvCheckMachineryRepository machineryRepo;
    private final DvCheckSubjectRepository subjectRepo;

    public DvCheckPlanController(DvCheckPlanRepository planRepo,
                                 DvCheckMachineryRepository machineryRepo,
                                 DvCheckSubjectRepository subjectRepo) {
        this.planRepo = planRepo;
        this.machineryRepo = machineryRepo;
        this.subjectRepo = subjectRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = planRepo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, planRepo.count(params));
    }

    @GetMapping("/getCheckPlan")
    public Map<String, Object> getCheckPlan(@RequestParam Map<String, String> params) {
        String machineryCode = params.get("machineryCode");
        String planType = params.get("planType");
        List<Long> planIds = machineryRepo.findPlanIdsByMachineryCode(machineryCode);
        if (planIds.isEmpty()) {
            return MesApiResponse.ok(List.of());
        }
        return MesApiResponse.ok(planRepo.findByPlanIdsAndType(planIds, planType));
    }

    @GetMapping("/{planId}")
    public Map<String, Object> getInfo(@PathVariable Long planId) {
        return planRepo.findById(planId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("计划不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("planCode"));
        if (StringUtils.hasText(code) && planRepo.codeExists(code, null)) {
            return MesApiResponse.error("编号已存在！");
        }
        Long id = planRepo.insert(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("planCode"));
        Long id = DvJdbcHelper.longObj(body.get("planId"));
        if (StringUtils.hasText(code) && planRepo.codeExists(code, id)) {
            return MesApiResponse.error("编号已存在！");
        }
        if ("FINISHED".equals(DvJdbcHelper.str(body.get("status")))) {
            if (machineryRepo.count(Map.of("planId", String.valueOf(id))) == 0) {
                return MesApiResponse.error("请指定设备!");
            }
            if (subjectRepo.count(Map.of("planId", String.valueOf(id))) == 0) {
                return MesApiResponse.error("请指定项目!");
            }
        }
        return MesApiResponse.toAjax(planRepo.update(body));
    }

    @DeleteMapping("/{planIds}")
    @Transactional
    public Map<String, Object> remove(@PathVariable String planIds) {
        List<Long> ids = Arrays.stream(planIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        for (Long planId : ids) {
            var plan = planRepo.findById(planId);
            if (plan.isPresent() && !"PREPARE".equals(plan.get().get("status"))) {
                return MesApiResponse.error("只能删除草稿状态单据！");
            }
            machineryRepo.deleteByPlanId(planId);
            subjectRepo.deleteByPlanId(planId);
        }
        return MesApiResponse.toAjax(planRepo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }
}
