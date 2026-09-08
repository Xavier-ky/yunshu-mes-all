package com.yunshu.mes.inventory.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.inventory.compat.WmDocSchemas;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WmPackageService {

    private final JdbcTemplate jdbc;
    private final WmBarcodeService barcodeService;

    public WmPackageService(JdbcTemplate jdbc, WmBarcodeService barcodeService) {
        this.jdbc = jdbc;
        this.barcodeService = barcodeService;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "wm_package", "package_id", WmDocSchemas.PACKAGE,
                WmDocSchemas.filter(params, "parentId", "packageCode", "barcodeContent", "soCode", "clientName", "inspectorName", "status"),
                PageUtil.offset(pn, ps), ps);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "wm_package", WmDocSchemas.PACKAGE,
                WmDocSchemas.filter(params, "parentId", "packageCode", "barcodeContent", "soCode", "clientName", "inspectorName", "status"));
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "wm_package", "package_id", "packageId", WmDocSchemas.PACKAGE, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        ensureCodeUnique(body, null);
        applyParentDefaults(body, true);
        Long packageId = WmSqlHelper.insert(jdbc, "wm_package", WmDocSchemas.PACKAGE, body);
        if (packageId == null) {
            throw new IllegalArgumentException("创建装箱单失败");
        }
        String code = String.valueOf(body.get("packageCode"));
        String clientName = body.get("clientName") == null ? "" : String.valueOf(body.get("clientName"));
        Long barcodeId = barcodeService.createForPackage(packageId, code, clientName);
        Map<String, Object> barcode = barcodeService.getById(barcodeId);
        Map<String, Object> patch = new LinkedHashMap<>();
        patch.put("packageId", packageId);
        patch.put("barcodeId", barcodeId);
        patch.put("barcodeContent", barcode.get("barcodeContent"));
        patch.put("barcodeUrl", barcode.get("barcodeUrl"));
        WmSqlHelper.update(jdbc, "wm_package", "package_id", WmDocSchemas.PACKAGE, patch, packageId);
        return packageId;
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("packageId")));
        ensureCodeUnique(body, id);
        return WmSqlHelper.update(jdbc, "wm_package", "package_id", WmDocSchemas.PACKAGE, body, id);
    }

    @Transactional
    public int addSubPackage(Map<String, Object> body) {
        Long packageId = Long.parseLong(String.valueOf(body.get("packageId")));
        Long parentId = Long.parseLong(String.valueOf(body.get("parentId")));
        if (packageId.equals(parentId)) {
            throw new IllegalArgumentException("不能添加自己为子箱！");
        }
        Map<String, Object> sub = getById(packageId);
        if (sub == null) {
            throw new IllegalArgumentException("子箱不存在");
        }
        String ancestors = String.valueOf(sub.get("ancestors"));
        if (!"0".equals(ancestors)) {
            throw new IllegalArgumentException("当前子箱已经有外箱包装！");
        }
        Map<String, Object> parent = getById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父箱不存在");
        }
        Map<String, Object> patch = new LinkedHashMap<>();
        patch.put("packageId", packageId);
        patch.put("parentId", parentId);
        patch.put("ancestors", parent.get("ancestors") + "," + parentId);
        return WmSqlHelper.update(jdbc, "wm_package", "package_id", WmDocSchemas.PACKAGE, patch, packageId);
    }

    @Transactional
    public int delete(Long id) {
        jdbc.update("DELETE FROM wm_package_line WHERE package_id = ?", id);
        return WmSqlHelper.delete(jdbc, "wm_package", "package_id", id);
    }

    private void applyParentDefaults(Map<String, Object> body, boolean onCreate) {
        if (!body.containsKey("parentId") || body.get("parentId") == null) {
            body.put("parentId", 0);
        }
        Long parentId = Long.parseLong(String.valueOf(body.get("parentId")));
        if (parentId == 0) {
            if (onCreate && !body.containsKey("ancestors")) {
                body.put("ancestors", "0");
            }
            return;
        }
        Map<String, Object> parent = getById(parentId);
        if (parent != null) {
            body.put("ancestors", parent.get("ancestors") + "," + parentId);
        }
    }

    private void ensureCodeUnique(Map<String, Object> body, Long excludeId) {
        if (!body.containsKey("packageCode") || !StringUtils.hasText(String.valueOf(body.get("packageCode")))) {
            return;
        }
        String code = String.valueOf(body.get("packageCode")).trim();
        Long cnt;
        if (excludeId != null) {
            cnt = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM wm_package WHERE package_code = ? AND package_id <> ?",
                    Long.class, code, excludeId);
        } else {
            cnt = jdbc.queryForObject("SELECT COUNT(*) FROM wm_package WHERE package_code = ?", Long.class, code);
        }
        if (cnt != null && cnt > 0) {
            throw new IllegalArgumentException("装箱单编号已存在!");
        }
    }
}
