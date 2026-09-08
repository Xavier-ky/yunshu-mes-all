package com.yunshu.mes.reporting.ureport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yunshu.mes.inventory.compat.service.WmBarcodeService;
import com.yunshu.mes.planning.repository.WorkOrderRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MesReportBeanTest {

    @Test
    void resolvesLegacyCodeParameterWhenIdIsNotProvided() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(
                "SELECT work_order_id FROM work_order WHERE work_order_no = ?",
                Long.class,
                "WO-20260701"))
                .thenReturn(42L);
        when(repository.findById(42L)).thenReturn(Optional.empty());
        MesReportBean bean = new MesReportBean(repository, jdbc, mock(WmBarcodeService.class));

        assertTrue(bean.getData("", "", Map.of("code", "WO-20260701")).isEmpty());

        verify(repository).findById(42L);
    }
}
