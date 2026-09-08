package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WmBarcodeService {

    private static final String QR_PREFIX = "https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=";

    private final JdbcTemplate jdbc;

    public WmBarcodeService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        List<Map<String, Object>> rows = WmSqlHelper.list(jdbc, "wm_barcode", "barcode_id", WmDocSchemas.BARCODE,
                WmDocSchemas.filter(params, "barcodeType", "bussinessCode", "bussinessName"),
                PageUtil.offset(pn, ps), ps);
        rows.forEach(this::enrichBarcodeUrl);
        return rows;
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_barcode", WmDocSchemas.BARCODE,
                WmDocSchemas.filter(params, "barcodeType", "bussinessCode", "bussinessName"));
    }

    public Map<String, Object> getById(Long id) {
        Map<String, Object> row = WmSqlHelper.getById(jdbc, "wm_barcode", "barcode_id", "barcodeId", WmDocSchemas.BARCODE, id);
        if (row != null) {
            enrichBarcodeUrl(row);
        }
        return row;
    }

    public Map<String, Object> findFirst(Map<String, String> params) {
        List<Map<String, Object>> rows = list(params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        ensureUnique(body, null);
        applyBarcodeUrl(body);
        return WmSqlHelper.insert(jdbc, "wm_barcode", WmDocSchemas.BARCODE, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("barcodeId")));
        ensureUnique(body, id);
        applyBarcodeUrl(body);
        return WmSqlHelper.update(jdbc, "wm_barcode", "barcode_id", WmDocSchemas.BARCODE, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "wm_barcode", "barcode_id", id);
    }

    @Transactional
    public Long createForPackage(Long packageId, String packageCode, String clientName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("barcodeFormart", "QR_CODE");
        body.put("barcodeType", "PACKAGE");
        body.put("barcodeContent", "PACKAGE-" + packageCode);
        body.put("bussinessId", packageId);
        body.put("bussinessCode", packageCode);
        body.put("bussinessName", clientName);
        body.put("enableFlag", "Y");
        applyBarcodeUrl(body);
        return WmSqlHelper.insert(jdbc, "wm_barcode", WmDocSchemas.BARCODE, body);
    }

    public String generateBarcodeUrl(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return QR_PREFIX + URLEncoder.encode(content, StandardCharsets.UTF_8);
    }

    private void applyBarcodeUrl(Map<String, Object> body) {
        Object content = body.get("barcodeContent");
        if (content != null && StringUtils.hasText(String.valueOf(content))) {
            body.put("barcodeUrl", generateBarcodeUrl(String.valueOf(content)));
        }
    }

    private void enrichBarcodeUrl(Map<String, Object> row) {
        Object url = row.get("barcodeUrl");
        if (url != null && StringUtils.hasText(String.valueOf(url))) {
            return;
        }
        Object content = row.get("barcodeContent");
        if (content != null && StringUtils.hasText(String.valueOf(content))) {
            row.put("barcodeUrl", generateBarcodeUrl(String.valueOf(content)));
        }
    }

    private void ensureUnique(Map<String, Object> body, Long excludeId) {
        if (body.get("bussinessId") == null || body.get("barcodeType") == null) {
            return;
        }
        Long bizId = Long.parseLong(String.valueOf(body.get("bussinessId")));
        String type = String.valueOf(body.get("barcodeType"));
        String sql = "SELECT COUNT(*) FROM wm_barcode WHERE bussiness_id = ? AND barcode_type = ?";
        Long cnt;
        if (excludeId != null) {
            cnt = jdbc.queryForObject(sql + " AND barcode_id <> ?", Long.class, bizId, type, excludeId);
        } else {
            cnt = jdbc.queryForObject(sql, Long.class, bizId, type);
        }
        if (cnt != null && cnt > 0) {
            throw new IllegalArgumentException("当前业务内容的条码已存在!");
        }
    }
}
