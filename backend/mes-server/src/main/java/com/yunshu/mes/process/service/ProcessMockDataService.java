package com.yunshu.mes.process.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.process.dto.ProcessRouteStepRequest;
import com.yunshu.mes.process.dto.RouteRequest;
import com.yunshu.mes.process.dto.StepRequest;
import com.yunshu.mes.process.vo.ProcessRouteStepVO;
import com.yunshu.mes.process.vo.ProcessRouteVO;
import com.yunshu.mes.process.vo.ProcessStepVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 工艺管理域 Mock 实现。写操作在无库时不可用。
 */
@Service
public class ProcessMockDataService implements ProcessService {

    private static final List<ProcessStepVO> STEPS = new ArrayList<>(List.of(
            new ProcessStepVO(1L, "STEP-001", "底座装配", "ASSEMBLY", "5.5", "ENABLED"),
            new ProcessStepVO(2L, "STEP-002", "电机安装", "ASSEMBLY", "8.0", "ENABLED"),
            new ProcessStepVO(3L, "STEP-003", "扇叶装配", "ASSEMBLY", "6.0", "ENABLED"),
            new ProcessStepVO(4L, "STEP-004", "整机测试", "TEST", "10.0", "ENABLED"),
            new ProcessStepVO(5L, "STEP-005", "包装", "PACK", "3.0", "ENABLED")
    ));

    private static final List<ProcessRouteVO> ROUTES = new ArrayList<>(List.of(
            new ProcessRouteVO(1L, "ROUTE-001", "台扇标准工艺路线", "V1.0", "ENABLED"),
            new ProcessRouteVO(2L, "ROUTE-002", "落地扇标准工艺路线", "V1.0", "ENABLED"),
            new ProcessRouteVO(3L, "ROUTE-003", "吊扇工艺路线", "V2.0", "ENABLED")
    ));

    // ---- 工序 ----
    @Override
    public List<ProcessStepVO> listSteps() {
        return new ArrayList<>(STEPS);
    }

    @Override
    public Optional<ProcessStepVO> getStepById(Long id) {
        return STEPS.stream().filter(s -> s.stepId().equals(id)).findFirst();
    }

    @Override
    public ProcessStepVO createStep(StepRequest req) {
        throw mockWriteError();
    }

    @Override
    public ProcessStepVO updateStep(Long id, StepRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteStep(Long id) {
        throw mockWriteError();
    }

    // ---- 工艺路线 ----
    @Override
    public List<ProcessRouteVO> listRoutes() {
        return new ArrayList<>(ROUTES);
    }

    @Override
    public Optional<ProcessRouteVO> getRouteById(Long id) {
        return ROUTES.stream().filter(r -> r.routeId().equals(id)).findFirst();
    }

    @Override
    public ProcessRouteVO createRoute(RouteRequest req) {
        throw mockWriteError();
    }

    @Override
    public ProcessRouteVO updateRoute(Long id, RouteRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteRoute(Long id) {
        throw mockWriteError();
    }

    @Override public List<ProcessRouteStepVO> listRouteSteps(Long routeId) { return List.of(); }
    @Override public ProcessRouteStepVO createRouteStep(ProcessRouteStepRequest req) { throw mockWriteError(); }
    @Override public ProcessRouteStepVO updateRouteStep(Long id, ProcessRouteStepRequest req) { throw mockWriteError(); }
    @Override public void deleteRouteStep(Long id) { throw mockWriteError(); }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
