package com.yunshu.mes.process.service;

import com.yunshu.mes.process.dto.ProcessRouteStepRequest;
import com.yunshu.mes.process.dto.RouteRequest;
import com.yunshu.mes.process.dto.StepRequest;
import com.yunshu.mes.process.vo.ProcessRouteStepVO;
import com.yunshu.mes.process.vo.ProcessRouteVO;
import com.yunshu.mes.process.vo.ProcessStepVO;
import java.util.List;
import java.util.Optional;

/**
 * 工艺管理域服务接口：工序、工艺路线。
 */
public interface ProcessService {

    // ---- 工序 ----
    List<ProcessStepVO> listSteps();

    Optional<ProcessStepVO> getStepById(Long id);

    ProcessStepVO createStep(StepRequest req);

    ProcessStepVO updateStep(Long id, StepRequest req);

    void deleteStep(Long id);

    // ---- 工艺路线 ----
    List<ProcessRouteVO> listRoutes();

    Optional<ProcessRouteVO> getRouteById(Long id);

    ProcessRouteVO createRoute(RouteRequest req);

    ProcessRouteVO updateRoute(Long id, RouteRequest req);

    void deleteRoute(Long id);

    // ---- 工艺路线工序 ----
    List<ProcessRouteStepVO> listRouteSteps(Long routeId);
    ProcessRouteStepVO createRouteStep(ProcessRouteStepRequest req);
    ProcessRouteStepVO updateRouteStep(Long id, ProcessRouteStepRequest req);
    void deleteRouteStep(Long id);
}
