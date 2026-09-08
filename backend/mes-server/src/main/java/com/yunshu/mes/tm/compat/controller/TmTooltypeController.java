package com.yunshu.mes.tm.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mes/tm/tooltype")
public class TmTooltypeController {

    @GetMapping("/list")
    public Map<String, Object> list() {
        return MesApiResponse.table(defaultTypes(), defaultTypes().size());
    }

    @GetMapping("/listAll")
    public Map<String, Object> listAll() {
        return MesApiResponse.ok(defaultTypes());
    }

    private List<Map<String, Object>> defaultTypes() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("toolTypeId", 1L);
        a.put("toolTypeCode", "FIX-01");
        a.put("toolTypeName", "通用夹具");
        a.put("enableFlag", "Y");
        rows.add(a);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("toolTypeId", 2L);
        b.put("toolTypeCode", "FIX-02");
        b.put("toolTypeName", "专用夹具");
        b.put("enableFlag", "Y");
        rows.add(b);
        return rows;
    }
}
