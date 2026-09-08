package com.yunshu.mes.planning.compat.service;

import com.yunshu.mes.planning.compat.repository.WorkOrderBomRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkorderBomService {

    private final WorkOrderBomRepository bomRepo;

    public WorkorderBomService(WorkOrderBomRepository bomRepo) {
        this.bomRepo = bomRepo;
    }

    public List<Map<String, Object>> listByWorkOrder(Long workOrderId) {
        return bomRepo.findByWorkOrderId(workOrderId);
    }

    public void update(Long lineId, Map<String, Object> body) {
        bomRepo.update(lineId, body);
    }

    public void delete(Long lineId) {
        bomRepo.delete(lineId);
    }
}
