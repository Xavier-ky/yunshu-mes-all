package com.yunshu.mes.planning.compat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yunshu.mes.planning.compat.repository.CompatWorkorderRepository;
import com.yunshu.mes.planning.compat.repository.WorkOrderBomRepository;
import com.yunshu.mes.planning.compat.repository.WorkorderProgressRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkorderServiceTest {

    @Test
    void listWithTaskJsonIncludesFeedbackAwareRouteProgress() {
        CompatWorkorderRepository workorderRepo = mock(CompatWorkorderRepository.class);
        WorkOrderBomRepository bomRepo = mock(WorkOrderBomRepository.class);
        WorkorderProgressRepository progressRepo = mock(WorkorderProgressRepository.class);
        WorkorderService service = new WorkorderService(workorderRepo, bomRepo, progressRepo);

        Map<String, Object> workorder = new LinkedHashMap<>();
        workorder.put("workorderId", 42L);
        workorder.put("parentId", null);
        workorder.put("quantity", new BigDecimal("120"));
        when(workorderRepo.search(Map.of(), "CONFIRMED")).thenReturn(new ArrayList<>(List.of(workorder)));
        when(progressRepo.tasksForWorkorder(42L)).thenReturn(List.of());
        List<Map<String, Object>> feedbackProgress = List.of(Map.of(
                "processId", 1L,
                "processName", "电机装配",
                "total", new BigDecimal("120"),
                "completeNumber", new BigDecimal("95"),
                "incompleteNumber", new BigDecimal("25")));
        when(progressRepo.routeHomeForWorkorder(42L, new BigDecimal("120"))).thenReturn(feedbackProgress);

        List<Map<String, Object>> result = service.listWithTaskJson(Map.of(), "CONFIRMED");

        assertEquals(feedbackProgress, result.get(0).get("routeHomg"));
    }
}
