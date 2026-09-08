package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.OperlogCompatRepository;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor/operlog")
public class OperlogCompatController {

    private final OperlogCompatRepository operlogRepo;

    public OperlogCompatController(OperlogCompatRepository operlogRepo) {
        this.operlogRepo = operlogRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(operlogRepo.search(params, SysCompatHelper.offset(pn, ps), ps), operlogRepo.count(params));
    }

    @OperLog(title = "操作日志", businessType = 3)
    @DeleteMapping("/{operIds}")
    public Map<String, Object> remove(@PathVariable String operIds) {
        return MesApiResponse.toAjax(operlogRepo.deleteByIds(SysCompatHelper.parseIds(operIds)));
    }

    @OperLog(title = "操作日志", businessType = 9)
    @DeleteMapping("/clean")
    public Map<String, Object> clean() {
        operlogRepo.clean();
        return MesApiResponse.ok();
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
