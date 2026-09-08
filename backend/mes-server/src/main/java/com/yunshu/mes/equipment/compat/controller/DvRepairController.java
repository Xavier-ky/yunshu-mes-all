package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.repository.DvRepairRepository;
import com.yunshu.mes.equipment.compat.service.EquipmentBridgeService;
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
@RequestMapping("/api/mes/dv/repair")
public class DvRepairController {

    private final DvRepairRepository repo;
    private final EquipmentBridgeService equipmentBridge;

    public DvRepairController(DvRepairRepository repo, EquipmentBridgeService equipmentBridge) {
        this.repo = repo;
        this.equipmentBridge = equipmentBridge;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = repo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, repo.count(params));
    }

    @GetMapping("/getRepairList")
    public Map<String, Object> getRepairList(@RequestParam Map<String, String> params) {
        String machineryCode = params.get("machineryCode");
        return MesApiResponse.ok(repo.listByMachineryCode(machineryCode));
    }

    @GetMapping("/{repairId}")
    public Map<String, Object> getInfo(@PathVariable Long repairId) {
        return repo.findById(repairId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("维修单不存在"));
    }

    @PostMapping
    @Transactional
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("repairCode"));
        if (StringUtils.hasText(code) && repo.codeExists(code, null)) {
            return MesApiResponse.error("维修单编号已存！");
        }
        Long id = repo.insert(body);
        if (id != null) {
            body.put("repairId", id);
            equipmentBridge.afterRepairSaved(id, body);
        }
        return MesApiResponse.ok(id);
    }

    @PutMapping
    @Transactional
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("repairCode"));
        Long id = DvJdbcHelper.longObj(body.get("repairId"));
        if (StringUtils.hasText(code) && repo.codeExists(code, id)) {
            return MesApiResponse.error("维修单编号已存！");
        }
        int rows = repo.update(body);
        if (rows > 0 && id != null) {
            equipmentBridge.afterRepairSaved(id, body);
        }
        return MesApiResponse.toAjax(rows);
    }

    @DeleteMapping("/{repairIds}")
    public Map<String, Object> remove(@PathVariable String repairIds) {
        List<Long> ids = Arrays.stream(repairIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        return MesApiResponse.toAjax(repo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }
}
