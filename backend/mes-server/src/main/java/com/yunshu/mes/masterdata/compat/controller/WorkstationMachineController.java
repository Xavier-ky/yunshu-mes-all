package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
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
@RequestMapping("/api/mes/md/workstationmachine")
public class WorkstationMachineController {

    private final JdbcTemplate jdbc;

    public WorkstationMachineController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "md_workstation_machine", "record_id",
                MdDocSchemas.MD_WORKSTATION_MACHINE, MdDocSchemas.filter(params, "workstationId"),
                PageUtil.offset(pn, ps), ps);
        return MesApiResponse.table(rows, WmSqlHelper.count(jdbc, "md_workstation_machine",
                MdDocSchemas.MD_WORKSTATION_MACHINE, MdDocSchemas.filter(params, "workstationId")));
    }

    @GetMapping("/{recordId}")
    public Map<String, Object> getInfo(@PathVariable Long recordId) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "md_workstation_machine", "record_id", "recordId",
                MdDocSchemas.MD_WORKSTATION_MACHINE, recordId);
        return row == null ? MesApiResponse.error("记录不存在") : MesApiResponse.ok(row);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        normalize(body);
        return MesApiResponse.ok(WmSqlHelper.insert(jdbc, "md_workstation_machine", MdDocSchemas.MD_WORKSTATION_MACHINE, body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        normalize(body);
        Long id = Long.parseLong(String.valueOf(body.get("recordId")));
        return MesApiResponse.toAjax(WmSqlHelper.update(jdbc, "md_workstation_machine", "record_id",
                MdDocSchemas.MD_WORKSTATION_MACHINE, body, id));
    }

    @DeleteMapping("/{recordIds}")
    public Map<String, Object> remove(@PathVariable String recordIds) {
        int n = 0;
        for (String p : recordIds.split(",")) {
            n += WmSqlHelper.delete(jdbc, "md_workstation_machine", "record_id", Long.parseLong(p.trim()));
        }
        return MesApiResponse.toAjax(n);
    }

    private void normalize(Map<String, Object> body) {
        if (body.get("machineId") == null && body.get("machineryId") != null) {
            body.put("machineId", body.get("machineryId"));
        }
        if (body.get("machineCode") == null && body.get("machineryCode") != null) {
            body.put("machineCode", body.get("machineryCode"));
        }
        if (body.get("machineName") == null && body.get("machineryName") != null) {
            body.put("machineName", body.get("machineryName"));
        }
    }
}
