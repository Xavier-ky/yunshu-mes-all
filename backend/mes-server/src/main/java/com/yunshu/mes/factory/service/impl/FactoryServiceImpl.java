package com.yunshu.mes.factory.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.factory.dto.ProductionLineRequest;
import com.yunshu.mes.factory.dto.ShiftRequest;
import com.yunshu.mes.factory.dto.WorkshopRequest;
import com.yunshu.mes.factory.dto.WorkstationRequest;
import com.yunshu.mes.factory.repository.FactoryShiftRepository;
import com.yunshu.mes.factory.repository.ProductionLineRepository;
import com.yunshu.mes.factory.repository.WorkshopRepository;
import com.yunshu.mes.factory.repository.WorkstationRepository;
import com.yunshu.mes.factory.service.FactoryMockDataService;
import com.yunshu.mes.factory.service.FactoryService;
import com.yunshu.mes.factory.vo.LineVO;
import com.yunshu.mes.factory.vo.ShiftVO;
import com.yunshu.mes.factory.vo.WorkshopVO;
import com.yunshu.mes.factory.vo.WorkstationVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 工厂资源域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class FactoryServiceImpl implements FactoryService {

    private static final Logger log = LoggerFactory.getLogger(FactoryServiceImpl.class);

    private final WorkshopRepository workshopRepo;
    private final ProductionLineRepository lineRepo;
    private final WorkstationRepository workstationRepo;
    private final FactoryShiftRepository shiftRepo;
    private final FactoryMockDataService mock;

    public FactoryServiceImpl(WorkshopRepository workshopRepo, ProductionLineRepository lineRepo,
                              WorkstationRepository workstationRepo, FactoryShiftRepository shiftRepo,
                              FactoryMockDataService mock) {
        this.workshopRepo = workshopRepo;
        this.lineRepo = lineRepo;
        this.workstationRepo = workstationRepo;
        this.shiftRepo = shiftRepo;
        this.mock = mock;
    }

    // ==================== 车间 ====================

    @Override
    public List<WorkshopVO> listWorkshops() {
        try { return workshopRepo.findAll(); }
        catch (DataAccessException e) { log.warn("车间列表回退 Mock：{}", e.getMessage()); return mock.listWorkshops(); }
    }

    @Override
    public Optional<WorkshopVO> getWorkshopById(Long id) {
        try { return workshopRepo.findById(id).or(() -> mock.getWorkshopById(id)); }
        catch (DataAccessException e) { log.warn("车间详情回退 Mock：{}", e.getMessage()); return mock.getWorkshopById(id); }
    }

    @Override
    public WorkshopVO createWorkshop(WorkshopRequest req) {
        try {
            Long id = workshopRepo.insert(req.workshopCode(), req.workshopName(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建车间失败");
            return workshopRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建车间后回查失败"));
        } catch (DataAccessException e) { log.warn("创建车间回退 Mock：{}", e.getMessage()); return mock.createWorkshop(req); }
    }

    @Override
    public WorkshopVO updateWorkshop(Long id, WorkshopRequest req) {
        try {
            workshopRepo.update(id, req.workshopCode(), req.workshopName(), req.status());
            return workshopRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "车间不存在"));
        } catch (DataAccessException e) { log.warn("更新车间回退 Mock：{}", e.getMessage()); return mock.updateWorkshop(id, req); }
    }

    @Override
    public void deleteWorkshop(Long id) {
        try { workshopRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除车间回退 Mock：{}", e.getMessage()); mock.deleteWorkshop(id); }
    }

    // ==================== 产线 ====================

    @Override
    public List<LineVO> listLines() {
        try { return lineRepo.findAll(); }
        catch (DataAccessException e) { log.warn("产线列表回退 Mock：{}", e.getMessage()); return mock.listLines(); }
    }

    @Override
    public Optional<LineVO> getLineById(Long id) {
        try { return lineRepo.findById(id).or(() -> mock.getLineById(id)); }
        catch (DataAccessException e) { log.warn("产线详情回退 Mock：{}", e.getMessage()); return mock.getLineById(id); }
    }

    @Override
    public LineVO createLine(ProductionLineRequest req) {
        try {
            Long id = lineRepo.insert(req.workshopId(), req.lineCode(), req.lineName(),
                    req.ratedCapacity(), req.capacityUnit(), req.status(),
                    req.modelPosX(), req.modelPosY(), req.modelPosZ());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建产线失败");
            return lineRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建产线后回查失败"));
        } catch (DataAccessException e) { log.warn("创建产线回退 Mock：{}", e.getMessage()); return mock.createLine(req); }
    }

    @Override
    public LineVO updateLine(Long id, ProductionLineRequest req) {
        try {
            lineRepo.update(id, req.workshopId(), req.lineCode(), req.lineName(),
                    req.ratedCapacity(), req.capacityUnit(), req.status());
            return lineRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "产线不存在"));
        } catch (DataAccessException e) { log.warn("更新产线回退 Mock：{}", e.getMessage()); return mock.updateLine(id, req); }
    }

    @Override
    public void deleteLine(Long id) {
        try { lineRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除产线回退 Mock：{}", e.getMessage()); mock.deleteLine(id); }
    }

    // ==================== 工位 ====================

    @Override
    public List<WorkstationVO> listWorkstations() {
        try { return workstationRepo.findAll(); }
        catch (DataAccessException e) { log.warn("工位列表回退 Mock：{}", e.getMessage()); return mock.listWorkstations(); }
    }

    @Override
    public Optional<WorkstationVO> getWorkstationById(Long id) {
        try { return workstationRepo.findById(id).or(() -> mock.getWorkstationById(id)); }
        catch (DataAccessException e) { log.warn("工位详情回退 Mock：{}", e.getMessage()); return mock.getWorkstationById(id); }
    }

    @Override
    public WorkstationVO createWorkstation(WorkstationRequest req) {
        try {
            Long id = workstationRepo.insert(req.lineId(), req.stationCode(), req.stationName(), req.stationType(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工位失败");
            return workstationRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建工位后回查失败"));
        } catch (DataAccessException e) { log.warn("创建工位回退 Mock：{}", e.getMessage()); return mock.createWorkstation(req); }
    }

    @Override
    public WorkstationVO updateWorkstation(Long id, WorkstationRequest req) {
        try {
            workstationRepo.update(id, req.lineId(), req.stationCode(), req.stationName(), req.stationType(), req.status());
            return workstationRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "工位不存在"));
        } catch (DataAccessException e) { log.warn("更新工位回退 Mock：{}", e.getMessage()); return mock.updateWorkstation(id, req); }
    }

    @Override
    public void deleteWorkstation(Long id) {
        try { workstationRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除工位回退 Mock：{}", e.getMessage()); mock.deleteWorkstation(id); }
    }

    // ==================== 班次 ====================

    @Override
    public List<ShiftVO> listShifts() {
        try { return shiftRepo.findAll(); }
        catch (DataAccessException e) { log.warn("班次列表回退 Mock：{}", e.getMessage()); return mock.listShifts(); }
    }

    @Override
    public Optional<ShiftVO> getShiftById(Long id) {
        try { return shiftRepo.findById(id).or(() -> mock.getShiftById(id)); }
        catch (DataAccessException e) { log.warn("班次详情回退 Mock：{}", e.getMessage()); return mock.getShiftById(id); }
    }

    @Override
    public ShiftVO createShift(ShiftRequest req) {
        try {
            Long id = shiftRepo.insert(req.shiftCode(), req.shiftName(), req.startTime(), req.endTime(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建班次失败");
            return shiftRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建班次后回查失败"));
        } catch (DataAccessException e) { log.warn("创建班次回退 Mock：{}", e.getMessage()); return mock.createShift(req); }
    }

    @Override
    public ShiftVO updateShift(Long id, ShiftRequest req) {
        try {
            shiftRepo.update(id, req.shiftCode(), req.shiftName(), req.startTime(), req.endTime(), req.status());
            return shiftRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "班次不存在"));
        } catch (DataAccessException e) { log.warn("更新班次回退 Mock：{}", e.getMessage()); return mock.updateShift(id, req); }
    }

    @Override
    public void deleteShift(Long id) {
        try { shiftRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除班次回退 Mock：{}", e.getMessage()); mock.deleteShift(id); }
    }
}
