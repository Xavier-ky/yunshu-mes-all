package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.PostCompatRepository;
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
@RequestMapping("/api/system/post")
public class PostCompatController {

    private final PostCompatRepository postRepo;

    public PostCompatController(PostCompatRepository postRepo) {
        this.postRepo = postRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(postRepo.search(params, SysCompatHelper.offset(pn, ps), ps), postRepo.count(params));
    }

    @GetMapping("/{postId}")
    public Map<String, Object> getInfo(@PathVariable Long postId) {
        return postRepo.findById(postId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("岗位不存在"));
    }

    @OperLog(title = "岗位管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String postCode = SysCompatHelper.str(body.get("postCode"));
        String postName = SysCompatHelper.str(body.get("postName"));
        if (postRepo.postCodeExists(postCode, null)) {
            return MesApiResponse.error("新增岗位'" + postName + "'失败，岗位编码已存在");
        }
        if (postRepo.postNameExists(postName, null)) {
            return MesApiResponse.error("新增岗位'" + postName + "'失败，岗位名称已存在");
        }
        return MesApiResponse.ok(postRepo.insert(body));
    }

    @OperLog(title = "岗位管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long postId = SysCompatHelper.longObj(body.get("postId"));
        String postCode = SysCompatHelper.str(body.get("postCode"));
        String postName = SysCompatHelper.str(body.get("postName"));
        if (StringUtils.hasText(postCode) && postRepo.postCodeExists(postCode, postId)) {
            return MesApiResponse.error("修改岗位失败，岗位编码已存在");
        }
        if (StringUtils.hasText(postName) && postRepo.postNameExists(postName, postId)) {
            return MesApiResponse.error("修改岗位失败，岗位名称已存在");
        }
        return MesApiResponse.toAjax(postRepo.update(body));
    }

    @OperLog(title = "岗位管理", businessType = 3)
    @DeleteMapping("/{postIds}")
    public Map<String, Object> remove(@PathVariable String postIds) {
        return MesApiResponse.toAjax(postRepo.deleteByIds(SysCompatHelper.parseIds(postIds)));
    }

    @PostMapping("/export")
    public Map<String, Object> export() {
        return MesApiResponse.error("导出功能待对接");
    }
}
