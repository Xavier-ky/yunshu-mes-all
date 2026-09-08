package com.yunshu.mes.equipment.compat.controller;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.repository.DvSubjectRepository;
import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/mes/dv/dvsubject")
public class DvSubjectController {

    private final DvSubjectRepository repo;

    public DvSubjectController(DvSubjectRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = repo.search(params, PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, repo.count(params));
    }

    @GetMapping("/{subjectId}")
    public Map<String, Object> getInfo(@PathVariable Long subjectId) {
        return repo.findById(subjectId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("项目不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("subjectCode"));
        if (StringUtils.hasText(code) && repo.codeExists(code, null)) {
            return MesApiResponse.error("项目编码已存在！");
        }
        Long id = repo.insert(body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        String code = DvJdbcHelper.str(body.get("subjectCode"));
        Long id = DvJdbcHelper.longObj(body.get("subjectId"));
        if (StringUtils.hasText(code) && repo.codeExists(code, id)) {
            return MesApiResponse.error("项目编码已存在！");
        }
        return MesApiResponse.toAjax(repo.update(body));
    }

    @DeleteMapping("/{subjectIds}")
    public Map<String, Object> remove(@PathVariable String subjectIds) {
        List<Long> ids = Arrays.stream(subjectIds.split(",")).map(String::trim).map(Long::parseLong).toList();
        return MesApiResponse.toAjax(repo.deleteByIds(ids));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("待对接");
    }
}
