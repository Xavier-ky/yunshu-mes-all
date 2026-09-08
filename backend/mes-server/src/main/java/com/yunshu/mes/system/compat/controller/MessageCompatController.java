package com.yunshu.mes.system.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.system.compat.SysCompatHelper;
import com.yunshu.mes.system.compat.annotation.OperLog;
import com.yunshu.mes.system.compat.repository.MessageCompatRepository;
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
@RequestMapping("/api/system/message")
public class MessageCompatController {

    private final MessageCompatRepository messageRepo;

    public MessageCompatController(MessageCompatRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(messageRepo.search(params, SysCompatHelper.offset(pn, ps), ps), messageRepo.count(params));
    }

    @GetMapping("/{messageId}")
    public Map<String, Object> getInfo(@PathVariable Long messageId) {
        return messageRepo.findById(messageId).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("消息不存在"));
    }

    @OperLog(title = "消息管理", businessType = 1)
    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(messageRepo.insert(body));
    }

    @OperLog(title = "消息管理", businessType = 2)
    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(messageRepo.update(body));
    }

    @OperLog(title = "消息管理", businessType = 3)
    @DeleteMapping("/{messageIds}")
    public Map<String, Object> remove(@PathVariable String messageIds) {
        return MesApiResponse.toAjax(messageRepo.deleteByIds(SysCompatHelper.parseIds(messageIds)));
    }
}
