package com.yunshu.mes.masterdata.compat.controller;

import com.yunshu.mes.masterdata.compat.repository.MdCompatRepository;
import com.yunshu.mes.planning.compat.MesApiResponse;
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
@RequestMapping("/api/mes/md/client")
public class ClientController {

    private final MdCompatRepository repo;

    public ClientController(MdCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = repo.listClients(params);
        long total = repo.countClients(params);
        return MesApiResponse.table(rows, total);
    }

    @GetMapping("/{clientId}")
    public Map<String, Object> getInfo(@PathVariable Long clientId) {
        return repo.findClient(clientId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("客户不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.get("clientCode"));
        if (repo.clientCodeExists(code, null)) {
            return MesApiResponse.error("客户编码已存在");
        }
        return MesApiResponse.ok(repo.insertClient(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = ((Number) body.get("clientId")).longValue();
        String code = String.valueOf(body.get("clientCode"));
        if (repo.clientCodeExists(code, id)) {
            return MesApiResponse.error("客户编码已存在");
        }
        return MesApiResponse.toAjax(repo.updateClient(body));
    }

    @DeleteMapping("/{clientIds}")
    public Map<String, Object> remove(@PathVariable String clientIds) {
        int n = 0;
        for (String part : clientIds.split(",")) {
            n += repo.deleteClient(Long.parseLong(part.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
