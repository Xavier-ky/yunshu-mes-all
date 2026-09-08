package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.LogininforCompatRepository;
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
@RequestMapping("/api/monitor/logininfor")
public class LogininforCompatController {

    private final LogininforCompatRepository logininforRepo;

    public LogininforCompatController(LogininforCompatRepository logininforRepo) {
        this.logininforRepo = logininforRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(logininforRepo.search(params, SysCompatHelper.offset(pn, ps), ps), logininforRepo.count(params));
    }

    @OperLog(title = "登录日志", businessType = 3)
    @DeleteMapping("/{infoIds}")
    public Map<String, Object> remove(@PathVariable String infoIds) {
        return MesApiResponse.toAjax(logininforRepo.deleteByIds(SysCompatHelper.parseIds(infoIds)));
    }

    @OperLog(title = "登录日志", businessType = 9)
    @DeleteMapping("/clean")
    public Map<String, Object> clean() {
        logininforRepo.clean();
        return MesApiResponse.ok();
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
