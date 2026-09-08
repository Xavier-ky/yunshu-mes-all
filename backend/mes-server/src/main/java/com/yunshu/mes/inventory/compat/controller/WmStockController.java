package com.yunshu.mes.inventory.compat.controller;

import com.yunshu.mes.inventory.compat.service.WmStockExportService;
import com.yunshu.mes.inventory.compat.service.WmStockService;
import com.yunshu.mes.planning.compat.MesApiResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/mes/wm/wmstock")
public class WmStockController {

    private final WmStockService service;
    private final WmStockExportService exportService;

    public WmStockController(WmStockService service, WmStockExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> rows = service.list(params);
        return MesApiResponse.table(rows, service.count(params));
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return MesApiResponse.ok(service.overview());
    }

    @GetMapping("/bin-map")
    public Map<String, Object> binMap(@RequestParam Map<String, String> params) {
        return service.binMap(params)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("仓库不存在或未指定 warehouseId"));
    }

    @GetMapping("/{materialStockId}")
    public Map<String, Object> getInfo(@PathVariable Long materialStockId) {
        return service.getById(materialStockId)
                .map(MesApiResponse::ok)
                .orElseGet(() -> MesApiResponse.error("库存记录不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.ok(service.create(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.toAjax(service.update(body));
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{materialStockIds}")
    public Map<String, Object> remove(@PathVariable String materialStockIds) {
        try {
            int total = 0;
            for (String part : materialStockIds.split(",")) {
                total += service.delete(Long.parseLong(part.trim()));
            }
            return MesApiResponse.toAjax(total);
        } catch (IllegalArgumentException e) {
            return MesApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/export")
    public void export(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"wmstock_export.xlsx\"");
        exportService.writeExport(response.getOutputStream(), params);
    }
}
