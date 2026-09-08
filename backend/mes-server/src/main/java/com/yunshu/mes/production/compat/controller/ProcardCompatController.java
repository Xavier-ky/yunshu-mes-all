package com.yunshu.mes.production.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.production.compat.repository.ProcardCompatRepository;
import com.yunshu.mes.system.compat.SysCompatHelper;
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
@RequestMapping("/api/mes/pro/procard")
public class ProcardCompatController {

    private final ProcardCompatRepository repo;

    public ProcardCompatController(ProcardCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(repo.searchCards(params, SysCompatHelper.offset(pn, ps), ps), repo.countCards(params));
    }

    @GetMapping("/{cardId}")
    public Map<String, Object> getInfo(@PathVariable Long cardId) {
        return repo.findCardById(cardId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("流转卡不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(repo.insertCard(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.updateCard(body));
    }

    @DeleteMapping("/{cardIds}")
    public Map<String, Object> remove(@PathVariable String cardIds) {
        return MesApiResponse.toAjax(repo.deleteCards(SysCompatHelper.parseIds(cardIds)));
    }
}
