package com.yunshu.mes.reporting.compat.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yunshu.mes.reporting.compat.repository.UreportCompatRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UreportCompatControllerTest {

    @Test
    void refusesToDeleteBuiltInReportTemplates() {
        UreportCompatRepository repository = org.mockito.Mockito.mock(UreportCompatRepository.class);
        when(repository.findNamesByIds(List.of(7L)))
                .thenReturn(List.of("产量统计报表.ureport.xml"));
        UreportCompatController controller = new UreportCompatController(repository);

        Map<String, Object> response = controller.remove("7");

        assertNotEquals(200, response.get("code"));
        verify(repository, never()).deleteByIds(List.of(7L));
    }
}
