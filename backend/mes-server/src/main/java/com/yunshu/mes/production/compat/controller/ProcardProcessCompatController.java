package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.production.compat.repository.ProcardCompatRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/pro/procardprocess")
public class ProcardProcessCompatController {

    private final ProcardCompatRepository repo;

    public ProcardProcessCompatController(ProcardCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@org.springframework.web.bind.annotation.RequestParam Map<String, String> params) {
        Long cardId = params.get("cardId") == null ? null : Long.parseLong(params.get("cardId"));
        if (cardId == null) {
            return MesApiResponse.table(java.util.List.of(), 0);
        }
        var rows = repo.listCardProcess(cardId);
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        return MesApiResponse.error("请通过 cardId 查询工序列表");
    }
}
