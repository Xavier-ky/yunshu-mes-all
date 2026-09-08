package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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
@RequestMapping("/api/mes/md/sop")
public class MdSopController {

    private static final AtomicLong ID = new AtomicLong(1);
    private static final Map<Long, Map<String, Object>> STORE = new ConcurrentHashMap<>();

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        String itemId = params.get("itemId");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : STORE.values()) {
            if (itemId == null || itemId.equals(String.valueOf(row.get("itemId")))) {
                rows.add(row);
            }
        }
        return MesApiResponse.table(rows, rows.size());
    }

    @GetMapping("/{sopId}")
    public Map<String, Object> getInfo(@PathVariable Long sopId) {
        Map<String, Object> row = STORE.get(sopId);
        return row == null ? MesApiResponse.error("SOP不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        long id = ID.incrementAndGet();
        body.put("sopId", id);
        STORE.put(id, body);
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("sopId")));
        STORE.put(id, body);
        return MesApiResponse.toAjax(1);
    }

    @DeleteMapping("/{sopIds}")
    public Map<String, Object> remove(@PathVariable String sopIds) {
        for (String p : sopIds.split(",")) {
            STORE.remove(Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(1);
    }
}
