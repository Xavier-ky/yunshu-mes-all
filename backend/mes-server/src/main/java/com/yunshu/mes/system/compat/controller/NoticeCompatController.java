package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.NoticeCompatRepository;
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
@RequestMapping("/api/system/notice")
public class NoticeCompatController {

    private final NoticeCompatRepository noticeRepo;

    public NoticeCompatController(NoticeCompatRepository noticeRepo) {
        this.noticeRepo = noticeRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(noticeRepo.search(params, SysCompatHelper.offset(pn, ps), ps), noticeRepo.count(params));
    }

    @GetMapping("/{noticeId}")
    public Map<String, Object> getInfo(@PathVariable Integer noticeId) {
        return noticeRepo.findById(noticeId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("公告不存在"));
    }

    @OperLog(title = "通知公告", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(noticeRepo.insert(body));
    }

    @OperLog(title = "通知公告", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(noticeRepo.update(body));
    }

    @OperLog(title = "通知公告", businessType = 3)
    @DeleteMapping("/{noticeIds}")
    public Map<String, Object> remove(@PathVariable String noticeIds) {
        List<Integer> ids = SysCompatHelper.parseIds(noticeIds).stream().map(Long::intValue).toList();
        return MesApiResponse.toAjax(noticeRepo.deleteByIds(ids));
    }
}
