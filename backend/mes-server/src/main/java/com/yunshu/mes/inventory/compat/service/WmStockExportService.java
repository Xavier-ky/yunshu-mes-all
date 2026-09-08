package com.yunshu.mes.inventory.compat.service;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class WmStockExportService {

    private static final int EXPORT_LIMIT = 10000;
    private static final String[] HEADERS = {
            "物料编码", "物料名称", "规格", "在库", "锁定", "可用", "单位",
            "批次号", "仓库", "库区", "库位", "入库日期", "冻结状态"
    };

    private final WmStockService stockService;

    public WmStockExportService(WmStockService stockService) {
        this.stockService = stockService;
    }

    public void writeExport(OutputStream out, Map<String, String> params) throws IOException {
        List<Map<String, Object>> rows = stockService.listAll(params, EXPORT_LIMIT);
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("库存现有量");
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                header.createCell(c).setCellValue(HEADERS[c]);
            }
            int r = 1;
            for (Map<String, Object> row : rows) {
                Row excelRow = sheet.createRow(r++);
                double onhand = doubleVal(row.get("quantityOnhand"));
                double reserved = doubleVal(row.get("quantityReserved"));
                double available = Math.max(0, onhand - reserved);
                excelRow.createCell(0).setCellValue(str(row.get("itemCode")));
                excelRow.createCell(1).setCellValue(str(row.get("itemName")));
                excelRow.createCell(2).setCellValue(str(row.get("specification")));
                excelRow.createCell(3).setCellValue(onhand);
                excelRow.createCell(4).setCellValue(reserved);
                excelRow.createCell(5).setCellValue(available);
                excelRow.createCell(6).setCellValue(str(row.get("unitName")));
                excelRow.createCell(7).setCellValue(str(row.get("batchCode")));
                excelRow.createCell(8).setCellValue(str(row.get("warehouseName")));
                excelRow.createCell(9).setCellValue(str(row.get("locationName")));
                excelRow.createCell(10).setCellValue(str(row.get("areaName")));
                excelRow.createCell(11).setCellValue(formatDate(row.get("recptDate")));
                excelRow.createCell(12).setCellValue(frozenLabel(row.get("frozenFlag")));
            }
            for (int c = 0; c < HEADERS.length; c++) {
                sheet.autoSizeColumn(c);
            }
            wb.write(out);
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static double doubleVal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return 0d;
        }
        return Double.parseDouble(String.valueOf(v));
    }

    private static String frozenLabel(Object v) {
        return "Y".equalsIgnoreCase(str(v)) ? "冻结" : "正常";
    }

    private static String formatDate(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate().toString();
        }
        String text = String.valueOf(v);
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }
}
