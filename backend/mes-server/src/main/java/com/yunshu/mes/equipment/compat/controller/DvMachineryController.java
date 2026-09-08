package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.repository.DvMachineryRepository;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
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
@RequestMapping("/api/mes/dv/machinery")
public class DvMachineryController {

    private final DvMachineryRepository repo;

    public DvMachineryController(DvMachineryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = repo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, repo.count(params));
    }

    @GetMapping("/{machineryId}")
    public Map<String, Object> getInfo(@PathVariable Long machineryId) {
        return repo.findById(machineryId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("设备不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("machineryCode"));
        if (StringUtils.hasText(code) && repo.codeExists(code, null)) {
            return MesApiResponse.error("编号已存在！");
        }
        Long id = repo.insert(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("machineryCode"));
        Long id = DvJdbcHelper.longObj(body.get("machineryId"));
        if (StringUtils.hasText(code) && repo.codeExists(code, id)) {
            return MesApiResponse.error("编号已存在！");
        }
        return MesApiResponse.toAjax(repo.update(body));
    }

    @DeleteMapping("/{machineryIds}")
    public Map<String, Object> remove(@PathVariable String machineryIds) {
        List<Long> ids = Arrays.stream(machineryIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        return MesApiResponse.toAjax(repo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }

    @PostMapping("/importData")
    public Map<String, Object> importData() {
        return MesApiResponse.error("待对接");
    }

    @PostMapping("/importTemplate")
    public Map<String, Object> importTemplate() {
        return MesApiResponse.error("待对接");
    }
}
