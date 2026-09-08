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
@RequestMapping("/api/mes/md/vendor")
public class VendorController {

    private final MdCompatRepository repo;

    public VendorController(MdCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = repo.listVendors(params);
        long total = repo.countVendors(params);
        return MesApiResponse.table(rows, total);
    }

    @GetMapping("/{vendorId}")
    public Map<String, Object> getInfo(@PathVariable Long vendorId) {
        return repo.findVendor(vendorId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("供应商不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.get("vendorCode"));
        if (repo.vendorCodeExists(code, null)) {
            return MesApiResponse.error("供应商编码已存在");
        }
        return MesApiResponse.ok(repo.insertVendor(body));
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        Long id = ((Number) body.get("vendorId")).longValue();
        String code = String.valueOf(body.get("vendorCode"));
        if (repo.vendorCodeExists(code, id)) {
            return MesApiResponse.error("供应商编码已存在");
        }
        return MesApiResponse.toAjax(repo.updateVendor(body));
    }

    @DeleteMapping("/{vendorIds}")
    public Map<String, Object> remove(@PathVariable String vendorIds) {
        int n = 0;
        for (String part : vendorIds.split(",")) {
            n += repo.deleteVendor(Long.parseLong(part.trim()));
        }
        return MesApiResponse.toAjax(n);
    }
}
