package com.yunshu.mes.planning.compat.service;

import com.yunshu.mes.planning.compat.GanttMapper;
import com.yunshu.mes.planning.compat.repository.ProTaskRepository;
import com.yunshu.mes.planning.compat.repository.CompatWorkorderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProTaskService {

    private static final String[] GANTT_COLORS = {
            "#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399", "#9B59B6", "#1ABC9C"
    };

    private final ProTaskRepository taskRepo;
    private final CompatWorkorderRepository workorderRepo;
    private final DispatchSyncService dispatchSync;

    public ProTaskService(
            ProTaskRepository taskRepo,
            CompatWorkorderRepository workorderRepo,
            DispatchSyncService dispatchSync) {
        this.taskRepo = taskRepo;
        this.workorderRepo = workorderRepo;
        this.dispatchSync = dispatchSync;
    }

    public List<Map<String, Object>> list(Map<String, String> query) {
        return taskRepo.search(query);
    }

    public Optional<Map<String, Object>> getById(Long id) {
        return taskRepo.findById(id);
    }

    public List<Map<String, Object>> listByWorkOrder(Long workOrderId) {
        return taskRepo.findByWorkOrderId(workOrderId);
    }

    public Long create(Map<String, Object> body) {
        Long id = taskRepo.insert(body);
        if (id != null && body.get("workorderId") != null) {
            taskRepo.refreshScheduledQty(((Number) body.get("workorderId")).longValue());
            dispatchSync.syncFromProductionTask(id);
        }
        return id;
    }

    public int update(Map<String, Object> body) {
        int rows = taskRepo.update(body);
        Object taskId = body.get("taskId");
        if (rows > 0 && taskId != null) {
            dispatchSync.syncFromProductionTask(Long.parseLong(String.valueOf(taskId)));
        }
        return rows;
    }

    public int delete(Long id) {
        Optional<Map<String, Object>> taskOpt = getById(id);
        int rows = taskRepo.delete(id);
        if (taskOpt.isPresent()) {
            Map<String, Object> task = taskOpt.get();
            Object woId = task.get("workorderId");
            if (woId instanceof Number n) {
                taskRepo.refreshScheduledQty(n.longValue());
            }
        }
        return rows;
    }

    /** 按筛选工单生成 project + task 甘特数据。 */
    public Map<String, Object> ganttPayload(Map<String, String> query) {
        String statusFilter = query.get("status");
        List<Map<String, Object>> workorders = workorderRepo.search(query, statusFilter);
        List<Map<String, Object>> data = new ArrayList<>();
        int colorIdx = 0;

        for (Map<String, Object> wo : workorders) {
            data.add(buildProjectNode(wo));
            Long workOrderId = ((Number) wo.get("workorderId")).longValue();
            List<Map<String, Object>> taskRows = taskRepo.listGanttDispatchRows(workOrderId);
            if (taskRows.isEmpty()) {
                taskRows = taskRepo.listGanttTaskRows(workOrderId);
            }
            for (Map<String, Object> row : taskRows) {
                row.put("color", GANTT_COLORS[colorIdx % GANTT_COLORS.length]);
                colorIdx++;
                data.add(row);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("links", List.of());
        return result;
    }

    private Map<String, Object> buildProjectNode(Map<String, Object> wo) {
        Map<String, Object> node = new LinkedHashMap<>();
        long woId = ((Number) wo.get("workorderId")).longValue();
        BigDecimal qty = decimal(wo.get("quantity"));
        BigDecimal produced = decimal(wo.get("quantityProduced"));
        String unit = stringOr(wo.get("unitOfMeasure"), "PCS");
        String productName = stringOr(wo.get("productName"), "产品");

        node.put("id", "MO" + woId);
        node.put("type", "project");
        node.put("text", productName + qty.stripTrailingZeros().toPlainString() + unit);
        node.put("product", productName);
        node.put("quantity", qty);
        node.put("progress", GanttMapper.progress(produced, qty));
        node.put("duration", 0L);

        Object parentId = wo.get("parentId");
        if (parentId instanceof Number n && n.longValue() != 0L) {
            node.put("parent", "MO" + n.longValue());
        }
        return node;
    }

    private static BigDecimal decimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(v));
    }

    private static String stringOr(Object v, String def) {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }
}
