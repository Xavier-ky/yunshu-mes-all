package com.yunshu.mes.print.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.print.compat.repository.PrintCompatRepository;
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
@RequestMapping("/api/print/template")
public class PrintTemplateCompatController {

    private final PrintCompatRepository repo;

    public PrintTemplateCompatController(PrintCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(repo.searchTemplates(params, SysCompatHelper.offset(pn, ps), ps), repo.countTemplates(params));
    }

    @GetMapping("/{templateId}")
    public Map<String, Object> getInfo(@PathVariable Long templateId) {
        return repo.findTemplateById(templateId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("模板不存在"));
    }

    @GetMapping("/templateType/{templateType}")
    public Map<String, Object> getByType(@PathVariable String templateType) {
        return repo.findTemplateByType(templateType).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("模板不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(repo.insertTemplate(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.updateTemplate(body));
    }

    @DeleteMapping("/{templateIds}")
    public Map<String, Object> remove(@PathVariable String templateIds) {
        return MesApiResponse.toAjax(repo.deleteTemplates(SysCompatHelper.parseIds(templateIds)));
    }
}
