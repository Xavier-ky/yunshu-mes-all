package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.DictTypeCompatRepository;
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
@RequestMapping("/api/system/dict/type")
public class DictTypeCompatController {

    private final DictTypeCompatRepository dictTypeRepo;

    public DictTypeCompatController(DictTypeCompatRepository dictTypeRepo) {
        this.dictTypeRepo = dictTypeRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(dictTypeRepo.search(params, SysCompatHelper.offset(pn, ps), ps), dictTypeRepo.count(params));
    }

    @GetMapping("/{dictId}")
    public Map<String, Object> getInfo(@PathVariable Long dictId) {
        return dictTypeRepo.findById(dictId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("字典类型不存在"));
    }

    @OperLog(title = "字典类型", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String dictType = SysCompatHelper.str(body.get("dictType"));
        if (dictTypeRepo.dictTypeExists(dictType, null)) {
            return MesApiResponse.error("新增字典'" + dictType + "'失败，字典类型已存在");
        }
        return MesApiResponse.ok(dictTypeRepo.insert(body));
    }

    @OperLog(title = "字典类型", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long dictId = SysCompatHelper.longObj(body.get("dictId"));
        String dictType = SysCompatHelper.str(body.get("dictType"));
        if (dictTypeRepo.dictTypeExists(dictType, dictId)) {
            return MesApiResponse.error("修改字典'" + dictType + "'失败，字典类型已存在");
        }
        return MesApiResponse.toAjax(dictTypeRepo.update(body));
    }

    @OperLog(title = "字典类型", businessType = 3)
    @DeleteMapping("/{dictIds}")
    public Map<String, Object> remove(@PathVariable String dictIds) {
        return MesApiResponse.toAjax(dictTypeRepo.deleteByIds(SysCompatHelper.parseIds(dictIds)));
    }

    @DeleteMapping("/refreshCache")
    public Map<String, Object> refreshCache() {
        return MesApiResponse.ok();
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
