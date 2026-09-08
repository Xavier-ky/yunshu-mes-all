package com.yunshu.mes.print.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.print.compat.repository.PrintCompatRepository;
import com.yunshu.mes.system.compat.SysCompatHelper;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/print/client")
public class PrintClientCompatController {

    private final PrintCompatRepository repo;

    public PrintClientCompatController(PrintCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping({"/list", "/page"})
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(repo.searchClients(params, SysCompatHelper.offset(pn, ps), ps), repo.countClients(params));
    }

    @GetMapping("/getAll")
    public Map<String, Object> getAll() {
        return MesApiResponse.ok(repo.searchClients(Map.of(), 0, 500));
    }

    @GetMapping("/getWorkshopAndWorkstation")
    public Map<String, Object> getWorkshopAndWorkstation() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workstations", repo.listWorkstationsForClient());
        return MesApiResponse.ok(data);
    }

    @GetMapping("/{clientId}")
    public Map<String, Object> getInfo(@PathVariable Long clientId) {
        var rows = repo.searchClients(Map.of(), 0, 500).stream()
                .filter(r -> clientId.equals(((Number) r.get("clientId")).longValue()))
                .toList();
        if (rows.isEmpty()) {
            return MesApiResponse.error("客户端不存在");
        }
        return MesApiResponse.ok(rows.get(0));
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        return MesApiResponse.ok(repo.insertClient(body));
    }

    @PostMapping("/edit")
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.updateClient(body));
    }

    @GetMapping("/remove/{clientIds}")
    public Map<String, Object> remove(@PathVariable String clientIds) {
        return MesApiResponse.toAjax(repo.deleteClients(SysCompatHelper.parseIds(clientIds)));
    }

    @PutMapping
    public Map<String, Object> editLegacy(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.updateClient(body));
    }

    @DeleteMapping("/{clientIds}")
    public Map<String, Object> removeLegacy(@PathVariable String clientIds) {
        return MesApiResponse.toAjax(repo.deleteClients(SysCompatHelper.parseIds(clientIds)));
    }
}
