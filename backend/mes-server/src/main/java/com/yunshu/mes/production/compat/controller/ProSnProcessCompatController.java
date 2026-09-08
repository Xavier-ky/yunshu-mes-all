package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.production.compat.repository.ProcardCompatRepository;
import com.yunshu.mes.system.compat.SysCompatHelper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/pro/prosnprocess")
public class ProSnProcessCompatController {

    private final ProcardCompatRepository repo;

    public ProSnProcessCompatController(ProcardCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(
                repo.listSnProcess(params, SysCompatHelper.offset(pn, ps), ps),
                repo.countSnProcess(params));
    }
}
