package com.yunshu.mes.print.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/print")
public class PrintSummaryController {

    private final JdbcTemplate jdbc;

    public PrintSummaryController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateTotal", count("SELECT COUNT(*) FROM print_template"));
        data.put("enabledTotal", count("SELECT COUNT(*) FROM print_template WHERE enable_flag = 'Y'"));
        data.put("clientTotal", count("SELECT COUNT(*) FROM print_client"));
        return MesApiResponse.ok(data);
    }

    private long count(String sql) {
        Long c = jdbc.queryForObject(sql, Long.class);
        return c == null ? 0 : c;
    }
}
