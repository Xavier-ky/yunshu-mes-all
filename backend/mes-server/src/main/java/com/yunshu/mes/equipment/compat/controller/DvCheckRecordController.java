package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.repository.DvCheckRecordLineRepository;
import com.yunshu.mes.equipment.compat.repository.DvCheckRecordRepository;
import com.yunshu.mes.equipment.compat.repository.DvCheckSubjectRepository;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/dv/checkrecord")
public class DvCheckRecordController {

    private final DvCheckRecordRepository recordRepo;
    private final DvCheckRecordLineRepository lineRepo;
    private final DvCheckSubjectRepository subjectRepo;

    public DvCheckRecordController(DvCheckRecordRepository recordRepo,
                                   DvCheckRecordLineRepository lineRepo,
                                   DvCheckSubjectRepository subjectRepo) {
        this.recordRepo = recordRepo;
        this.lineRepo = lineRepo;
        this.subjectRepo = subjectRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = recordRepo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, recordRepo.count(params));
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        return recordRepo.findById(recordId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("点检记录不存在"));
    }

    @PostMapping
    @Transactional
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long recordId = recordRepo.insert(body);
        if (recordId != null) {
            Long planId = DvJdbcHelper.longObj(body.get("planId"));
            if (planId != null) {
                List<Map<String, Object>> subjects = subjectRepo.listByPlanId(planId);
                if (!subjects.isEmpty()) {
                    lineRepo.insertFromPlanSubjects(recordId, subjects);
                }
            }
        }
        return MesApiResponse.ok(recordId);
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long recordId = DvJdbcHelper.longObj(body.get("recordId"));
        if ("FINISHED".equals(DvJdbcHelper.str(body.get("status")))) {
            long lineCount = lineRepo.count(Map.of("recordId", String.valueOf(recordId)));
            if (lineCount == 0) {
                return MesApiResponse.error("请添加设备点检项目结果信息");
            }
        }
        var old = recordRepo.findById(recordId);
        Long newPlanId = DvJdbcHelper.longObj(body.get("planId"));
        if (old.isPresent()) {
            Long oldPlanId = DvJdbcHelper.longObj(old.get().get("planId"));
            if (oldPlanId != null && newPlanId != null && !newPlanId.equals(oldPlanId)) {
                lineRepo.deleteByRecordId(recordId);
                List<Map<String, Object>> subjects = subjectRepo.listByPlanId(newPlanId);
                if (!subjects.isEmpty()) {
                    lineRepo.insertFromPlanSubjects(recordId, subjects);
                }
            }
        }
        return MesApiResponse.toAjax(recordRepo.update(body));
    }

    @DeleteMapping("/{recordIds}")
    @Transactional
    public Map<String, Object> remove(@PathVariable String recordIds) {
        List<Long> ids = Arrays.stream(recordIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        for (Long recordId : ids) {
            lineRepo.deleteByRecordId(recordId);
        }
        return MesApiResponse.toAjax(recordRepo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }
}
