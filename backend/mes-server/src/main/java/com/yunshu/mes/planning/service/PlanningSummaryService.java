package com.yunshu.mes.planning.service;

import com.yunshu.mes.planning.vo.CustomerOrderItemVO;
import java.util.List;
import java.util.Map;

public interface PlanningSummaryService {

    Map<String, Object> getSummary();

    List<CustomerOrderItemVO> listOrderItems(Long orderId);

    Map<String, Object> getSchedulingOverview();
}
