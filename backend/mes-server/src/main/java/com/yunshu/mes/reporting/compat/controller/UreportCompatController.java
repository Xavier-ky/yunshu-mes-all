package com.yunshu.mes.reporting.compat.controller;

import com.yunshu.mes.planning.compat.MesApiResponse;
import com.yunshu.mes.reporting.compat.repository.UreportCompatRepository;
import com.yunshu.mes.system.compat.SysCompatHelper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
@RequestMapping("/api/ureportM")
public class UreportCompatController {

    private static final Set<String> PROTECTED_TEMPLATES = Set.of(
            "MES通用表格模板.ureport.xml",
            "产量统计报表.ureport.xml",
            "质量检验报告.ureport.xml",
            "生产工单打印模版.ureport.xml");

    private final UreportCompatRepository repo;

    public UreportCompatController(UreportCompatRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        int pn = SysCompatHelper.pageNum(params);
        int ps = SysCompatHelper.pageSize(params);
        return MesApiResponse.table(repo.search(params, SysCompatHelper.offset(pn, ps), ps), repo.count(params));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getInfo(@PathVariable Long id) {
        return repo.findById(id).map(MesApiResponse::ok).orElseGet(() -> MesApiResponse.error("报表不存在"));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        try {
            return MesApiResponse.ok(repo.insert(body));
        } catch (IllegalArgumentException ex) {
            return MesApiResponse.error(ex.getMessage());
        }
    }

    @PutMapping
    public Map<String, Object> edit(@RequestBody Map<String, Object> body) {
        return MesApiResponse.toAjax(repo.update(body));
    }

    @DeleteMapping("/{ids}")
    public Map<String, Object> remove(@PathVariable String ids) {
        List<Long> idList = SysCompatHelper.parseIds(ids);
        if (repo.findNamesByIds(idList).stream().anyMatch(PROTECTED_TEMPLATES::contains)) {
            return MesApiResponse.error("内置报表模板不可删除");
        }
        return MesApiResponse.toAjax(repo.deleteByIds(idList));
    }

    /** 导出报表档案元数据列表（名称/时间）；单报表取数导出请用 /ureport/excel?_u=mysql:文件名 */
    @PostMapping("/export")
    public void export(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"ureport_archive.xlsx\"");
        List<Map<String, Object>> rows = repo.listForExport(params);
        try (Workbook wb = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("报表档案");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("报表名称");
            header.createCell(1).setCellValue("创建时间");
            header.createCell(2).setCellValue("更新时间");
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(str(row.get("name")));
                excelRow.createCell(1).setCellValue(str(row.get("createTime")));
                excelRow.createCell(2).setCellValue(str(row.get("updateTime")));
            }
            for (int c = 0; c < 3; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(out);
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
