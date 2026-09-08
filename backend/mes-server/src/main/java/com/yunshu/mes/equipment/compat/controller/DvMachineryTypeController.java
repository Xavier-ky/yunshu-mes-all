package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.repository.DvMachineryRepository;
import com.yunshu.mes.equipment.compat.repository.DvMachineryTypeRepository;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
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
@RequestMapping("/api/mes/dv/machinerytype")
public class DvMachineryTypeController {

    private final DvMachineryTypeRepository typeRepo;
    private final DvMachineryRepository machineryRepo;

    public DvMachineryTypeController(DvMachineryTypeRepository typeRepo, DvMachineryRepository machineryRepo) {
        this.typeRepo = typeRepo;
        this.machineryRepo = machineryRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        return MesApiResponse.ok(typeRepo.listAll(params));
    }

    @GetMapping("/{machineryTypeId}")
    public Map<String, Object> getInfo(@PathVariable Long machineryTypeId) {
        return typeRepo.findById(machineryTypeId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("设备类型不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = typeRepo.insert(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(typeRepo.update(body));
    }

    @DeleteMapping("/{machineryTypeIds}")
    public Map<String, Object> remove(@PathVariable String machineryTypeIds) {
        List<Long> ids = parseIds(machineryTypeIds);
        for (Long typeId : ids) {
            if (machineryRepo.countByTypeId(typeId) > 0) {
                return MesApiResponse.error("设备类型下已配置了设备，不能删除！");
            }
        }
        return MesApiResponse.toAjax(typeRepo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }

    private static List<Long> parseIds(String raw) {
        return Arrays.stream(raw.split(",")).map(String::trim).map(Long::parseLong).toList();
    }
}
