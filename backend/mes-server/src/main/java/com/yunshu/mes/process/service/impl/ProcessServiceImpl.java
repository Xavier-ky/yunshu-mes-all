package com.yunshu.mes.process.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.process.dto.ProcessRouteStepRequest;
import com.yunshu.mes.process.dto.RouteRequest;
import com.yunshu.mes.process.dto.StepRequest;
import com.yunshu.mes.process.repository.ProcessRouteStepRepository;
import com.yunshu.mes.process.repository.RouteRepository;
import com.yunshu.mes.process.repository.StepRepository;
import com.yunshu.mes.process.service.ProcessMockDataService;
import com.yunshu.mes.process.service.ProcessService;
import com.yunshu.mes.process.vo.ProcessRouteStepVO;
import com.yunshu.mes.process.vo.ProcessRouteVO;
import com.yunshu.mes.process.vo.ProcessStepVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 工艺管理域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class ProcessServiceImpl implements ProcessService {

    private static final Logger log = LoggerFactory.getLogger(ProcessServiceImpl.class);

    private final StepRepository stepRepo;
    private final RouteRepository routeRepo;
    private final ProcessRouteStepRepository routeStepRepo;
    private final ProcessMockDataService mock;

    public ProcessServiceImpl(StepRepository stepRepo, RouteRepository routeRepo,
                              ProcessRouteStepRepository routeStepRepo,
                              ProcessMockDataService mock) {
        this.stepRepo = stepRepo;
        this.routeRepo = routeRepo;
        this.routeStepRepo = routeStepRepo;
        this.mock = mock;
    }

    // ==================== 工序 ====================

    @Override
    public List<ProcessStepVO> listSteps() {
        try { return stepRepo.findAll(); }
        catch (DataAccessException e) { log.warn("工序列表回退 Mock：{}", e.getMessage()); return mock.listSteps(); }
    }

    @Override
    public Optional<ProcessStepVO> getStepById(Long id) {
        try { return stepRepo.findById(id).or(() -> mock.getStepById(id)); }
        catch (DataAccessException e) { log.warn("工序详情回退 Mock：{}", e.getMessage()); return mock.getStepById(id); }
    }

    @Override
    public ProcessStepVO createStep(StepRequest req) {
        try {
            Long id = stepRepo.insert(req.stepCode(), req.stepName(), req.stepType(),
                    req.standardHours(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工序失败");
            return stepRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工序后回查失败"));
        } catch (DataAccessException e) { log.warn("创建工序回退 Mock：{}", e.getMessage()); return mock.createStep(req); }
    }

    @Override
    public ProcessStepVO updateStep(Long id, StepRequest req) {
        try {
            stepRepo.update(id, req.stepCode(), req.stepName(), req.stepType(),
                    req.standardHours(), req.status());
            return stepRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工序不存在"));
        } catch (DataAccessException e) { log.warn("更新工序回退 Mock：{}", e.getMessage()); return mock.updateStep(id, req); }
    }

    @Override
    public void deleteStep(Long id) {
        try { stepRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除工序回退 Mock：{}", e.getMessage()); mock.deleteStep(id); }
    }

    // ==================== 工艺路线 ====================

    @Override
    public List<ProcessRouteVO> listRoutes() {
        try { return routeRepo.findAll(); }
        catch (DataAccessException e) { log.warn("工艺路线列表回退 Mock：{}", e.getMessage()); return mock.listRoutes(); }
    }

    @Override
    public Optional<ProcessRouteVO> getRouteById(Long id) {
        try { return routeRepo.findById(id).or(() -> mock.getRouteById(id)); }
        catch (DataAccessException e) { log.warn("工艺路线详情回退 Mock：{}", e.getMessage()); return mock.getRouteById(id); }
    }

    @Override
    public ProcessRouteVO createRoute(RouteRequest req) {
        try {
            Long id = routeRepo.insert(req.routeCode(), req.routeName(), req.routeVersion(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工艺路线失败");
            return routeRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工艺路线后回查失败"));
        } catch (DataAccessException e) { log.warn("创建工艺路线回退 Mock：{}", e.getMessage()); return mock.createRoute(req); }
    }

    @Override
    public ProcessRouteVO updateRoute(Long id, RouteRequest req) {
        try {
            routeRepo.update(id, req.routeCode(), req.routeName(), req.routeVersion(), req.status());
            return routeRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工艺路线不存在"));
        } catch (DataAccessException e) { log.warn("更新工艺路线回退 Mock：{}", e.getMessage()); return mock.updateRoute(id, req); }
    }

    @Override
    public void deleteRoute(Long id) {
        try { routeRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除工艺路线回退 Mock：{}", e.getMessage()); mock.deleteRoute(id); }
    }

    // ==================== 工艺路线工序 ====================

    @Override
    public List<ProcessRouteStepVO> listRouteSteps(Long routeId) {
        try { return routeStepRepo.findByRouteId(routeId); }
        catch (DataAccessException e) { log.warn("路线工序列表回退 Mock：{}", e.getMessage()); return List.of(); }
    }

    @Override
    public ProcessRouteStepVO createRouteStep(ProcessRouteStepRequest req) {
        try {
            Long id = routeStepRepo.insert(req.routeId(), req.stepId(), req.stepSeq(), req.stationType(), req.isMustPass());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建路线工序失败");
            var list = routeStepRepo.findByRouteId(req.routeId());
            return list.stream().filter(s -> s.getRouteStepId().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "回查失败"));
        } catch (DataAccessException e) { log.warn("创建路线工序回退 Mock：{}", e.getMessage()); throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }

    @Override
    public ProcessRouteStepVO updateRouteStep(Long id, ProcessRouteStepRequest req) {
        try {
            routeStepRepo.update(id, req.stepSeq(), req.stationType(), req.isMustPass());
            var list = routeStepRepo.findByRouteId(req.routeId());
            return list.stream().filter(s -> s.getRouteStepId().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "路线工序不存在"));
        } catch (DataAccessException e) { log.warn("更新路线工序回退 Mock：{}", e.getMessage()); throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage()); }
    }

    @Override
    public void deleteRouteStep(Long id) {
        try { routeStepRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除路线工序回退 Mock：{}", e.getMessage()); }
    }
}
