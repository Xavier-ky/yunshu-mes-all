package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.DictDataCompatRepository;
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
@RequestMapping("/api/system/dict/data")
public class DictDataCompatController {

    private final DictDataCompatRepository dictDataRepo;

    public DictDataCompatController(DictDataCompatRepository dictDataRepo) {
        this.dictDataRepo = dictDataRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(dictDataRepo.search(params, SysCompatHelper.offset(pn, ps), ps), dictDataRepo.count(params));
    }

    @GetMapping("/{dictCode}")
    public Map<String, Object> getInfo(@PathVariable Long dictCode) {
        return dictDataRepo.findById(dictCode).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("字典数据不存在"));
    }

    @OperLog(title = "字典数据", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(dictDataRepo.insert(body));
    }

    @OperLog(title = "字典数据", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(dictDataRepo.update(body));
    }

    @OperLog(title = "字典数据", businessType = 3)
    @DeleteMapping("/{dictCodes}")
    public Map<String, Object> remove(@PathVariable String dictCodes) {
        return MesApiResponse.toAjax(dictDataRepo.deleteByIds(SysCompatHelper.parseIds(dictCodes)));
    }
}
