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
@RequestMapping("/api/print/printerconfig")
public class PrintPrinterConfigCompatController {

    private final PrintCompatRepository repo;

    public PrintPrinterConfigCompatController(PrintCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(
                repo.searchPrinters(params, SysCompatHelper.offset(pn, ps), ps),
                repo.countPrinters(params));
    }

    @GetMapping("/{printerId}")
    public Map<String, Object> getInfo(@PathVariable Long printerId) {
        return repo.findPrinterById(printerId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("打印机不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Long id = repo.insertPrinter(body);
        if ("Y".equalsIgnoreCase(SysCompatHelper.str(body.get("defaultFlag")))) {
            Long clientId = SysCompatHelper.longObj(body.get("clientId"));
            if (clientId != null && id != null) {
                repo.setDefaultPrinter(id, clientId);
            }
        }
        return MesApiResponse.ok(id);
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        int rows = repo.updatePrinter(body);
        if ("Y".equalsIgnoreCase(SysCompatHelper.str(body.get("defaultFlag")))) {
            Long clientId = SysCompatHelper.longObj(body.get("clientId"));
            Long printerId = SysCompatHelper.longObj(body.get("printerId"));
            if (clientId != null && printerId != null) {
                repo.setDefaultPrinter(printerId, clientId);
            }
        }
        return MesApiResponse.toAjax(rows);
    }

    @DeleteMapping("/{printerIds}")
    public Map<String, Object> remove(@PathVariable String printerIds) {
        return MesApiResponse.toAjax(repo.deletePrinters(SysCompatHelper.parseIds(printerIds)));
    }

    @GetMapping("/getDefaultPrint/{clientId}")
    public Map<String, Object> getDefaultPrint(@PathVariable Long clientId) {
        return repo.findDefaultPrinterByClientId(clientId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("未配置默认打印机"));
    }

    @PostMapping("/editDefaultPrint")
    public Map<String, Object> editDefaultPrint(@RequestBody Map<String, Object> body) {
        Long clientId = SysCompatHelper.longObj(body.get("clientId"));
        Long printerId = SysCompatHelper.longObj(body.get("printerId"));
        if (clientId == null || printerId == null) {
            return MesApiResponse.error("clientId 与 printerId 不能为空");
        }
        return MesApiResponse.toAjax(repo.setDefaultPrinter(printerId, clientId));
    }
}
