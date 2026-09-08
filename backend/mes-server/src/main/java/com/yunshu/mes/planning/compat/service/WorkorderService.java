package com.yunshu.mes.planning.compat.service;

import com.yunshu.mes.planning.compat.repository.CompatWorkorderRepository;
import com.yunshu.mes.planning.compat.repository.WorkOrderBomRepository;
import com.yunshu.mes.planning.compat.repository.WorkorderProgressRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkorderService {

    private final CompatWorkorderRepository workorderRepo;
    private final WorkOrderBomRepository bomRepo;
    private final WorkorderProgressRepository progressRepo;

    public WorkorderService(CompatWorkorderRepository workorderRepo, WorkOrderBomRepository bomRepo,
            WorkorderProgressRepository progressRepo) {
        this.workorderRepo = workorderRepo;
        this.bomRepo = bomRepo;
        this.progressRepo = progressRepo;
    }

    public List<Map<String, Object>> list(Map<String, String> query, String statusFilter) {
        List<Map<String, Object>> flat = workorderRepo.search(query, statusFilter);
        return buildTree(flat);
    }

    public long count(Map<String, String> query, String statusFilter) {
        return workorderRepo.count(query, statusFilter);
    }

    public List<Map<String, Object>> listWithTaskJson(Map<String, String> query, String statusFilter) {
        List<Map<String, Object>> flat = workorderRepo.search(query, statusFilter);
        for (Map<String, Object> row : flat) {
            long woId = ((Number) row.get("workorderId")).longValue();
            row.put("tasks", progressRepo.tasksForWorkorder(woId));
            BigDecimal qty = row.get("quantity") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            row.put("routeHomg", progressRepo.routeHomeForWorkorder(woId, qty));
        }
        return buildTree(flat);
    }

    public List<Map<String, Object>> getHomeList(Map<String, String> query) {
        Map<String, String> q = new LinkedHashMap<>(query);
        q.putIfAbsent("status", "CONFIRMED");
        List<Map<String, Object>> flat = workorderRepo.search(q, q.get("status"));
        for (Map<String, Object> row : flat) {
            long woId = ((Number) row.get("workorderId")).longValue();
            BigDecimal qty = row.get("quantity") instanceof BigDecimal bd ? bd : BigDecimal.ZERO;
            row.put("routeHomg", progressRepo.routeHomeForWorkorder(woId, qty));
        }
        return buildTree(flat);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return workorderRepo.findById(id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        String code = String.valueOf(body.get("workorderCode"));
        if (workorderRepo.existsCode(code, null)) {
            throw new IllegalArgumentException("生产工单编号已存在！");
        }
        Long productId = requireLong(body, "productId");
        Long orderId = longVal(body, "orderId");
        if (orderId != null && !workorderRepo.orderMatchesProduct(orderId, productId)) {
            throw new IllegalArgumentException("订单与产品不匹配，请检查 orderId 与 productId");
        }
        Long bomId = workorderRepo.findDefaultBomId(productId).orElse(null);
        if (bomId != null) {
            body.put("bomId", bomId);
        }
        if (!body.containsKey("lifecycleStatus")) {
            body.put("lifecycleStatus", "RELEASED");
        }
        Long id = workorderRepo.insert(body);
        if (id != null) {
            generateBomSnapshot(id, requireLong(body, "productId"), decimal(body, "quantity", BigDecimal.ONE));
        }
        return id;
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = requireLong(body, "workorderId");
        if (workorderRepo.existsCode(String.valueOf(body.get("workorderCode")), id)) {
            throw new IllegalArgumentException("生产工单编号已存在！");
        }
        var existing = workorderRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("工单不存在"));
        int rows = workorderRepo.update(id, body);
        Long oldProduct = ((Number) existing.get("productId")).longValue();
        Long newProduct = longVal(body, "productId");
        BigDecimal oldQty = (BigDecimal) existing.get("quantity");
        BigDecimal newQty = decimal(body, "quantity", oldQty);
        if (newProduct != null && (!newProduct.equals(oldProduct) || newQty.compareTo(oldQty) != 0)) {
            bomRepo.deleteByWorkOrderId(id);
            generateBomSnapshot(id, newProduct != null ? newProduct : oldProduct, newQty);
        }
        return rows;
    }

    @Transactional
    public int delete(Long id) {
        var wo = workorderRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("工单不存在"));
        if (!"PREPARE".equals(wo.get("status"))) {
            throw new IllegalArgumentException("只能删除草稿状态单据！");
        }
        bomRepo.deleteByWorkOrderId(id);
        return workorderRepo.delete(id);
    }

    public int finish(Long id) {
        return workorderRepo.finish(id);
    }

    public int cancel(Long id) {
        return workorderRepo.cancel(id);
    }

    public List<Map<String, Object>> listMaterialRequirements(Long workOrderId) {
        List<Map<String, Object>> boms = bomRepo.findByWorkOrderId(workOrderId);
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> bom : boms) {
            String code = String.valueOf(bom.get("itemCode"));
            if (merged.containsKey(code)) {
                Map<String, Object> ex = merged.get(code);
                BigDecimal q1 = (BigDecimal) ex.get("quantity");
                BigDecimal q2 = (BigDecimal) bom.get("quantity");
                ex.put("quantity", q1.add(q2));
            } else {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("bomItemId", bom.get("itemId"));
                row.put("bomItemCode", bom.get("itemCode"));
                row.put("bomItemName", bom.get("itemName"));
                row.put("bomItemSpec", bom.get("itemSpc"));
                row.put("itemOrProduct", bom.get("itemOrProduct"));
                row.put("unitOfMeasure", bom.get("unitOfMeasure"));
                row.put("unitName", bom.get("unitName"));
                row.put("quantity", bom.get("quantity"));
                merged.put(code, row);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void generateBomSnapshot(Long workOrderId, Long productId, BigDecimal orderQty) {
        Optional<Long> bomIdOpt = workorderRepo.findDefaultBomId(productId);
        if (bomIdOpt.isEmpty()) {
            return;
        }
        List<Map<String, Object>> items = workorderRepo.findBomItems(bomIdOpt.get());
        for (Map<String, Object> item : items) {
            BigDecimal qtyPer = (BigDecimal) item.get("qtyPer");
            BigDecimal qty = orderQty.multiply(qtyPer);
            String matType = String.valueOf(item.get("materialType"));
            String itemOrProduct = "FINISHED".equals(matType) ? "PRODUCT" : "ITEM";
            bomRepo.insert(workOrderId,
                    ((Number) item.get("materialId")).longValue(),
                    String.valueOf(item.get("materialCode")),
                    String.valueOf(item.get("materialName")),
                    null, "PCS", itemOrProduct, qty, null);
        }
    }

    private static List<Map<String, Object>> buildTree(List<Map<String, Object>> flat) {
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : flat) {
            row.put("children", new ArrayList<Map<String, Object>>());
            byId.put(((Number) row.get("workorderId")).longValue(), row);
        }
        for (Map<String, Object> row : flat) {
            Object parentObj = row.get("parentId");
            if (parentObj == null) {
                roots.add(row);
                continue;
            }
            long parentId = ((Number) parentObj).longValue();
            if (parentId == 0) {
                roots.add(row);
                continue;
            }
            Map<String, Object> parent = byId.get(parentId);
            if (parent == null) {
                roots.add(row);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(row);
            }
        }
        for (Map<String, Object> row : flat) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) row.get("children");
            if (children.isEmpty()) {
                row.remove("children");
            }
        }
        return roots;
    }

    private static Long requireLong(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            throw new IllegalArgumentException(key + " required");
        }
        return ((Number) v).longValue();
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || "".equals(v)) {
            return null;
        }
        return ((Number) v).longValue();
    }

    private static BigDecimal decimal(Map<String, Object> body, String key, BigDecimal def) {
        Object v = body.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(String.valueOf(v));
    }
}
