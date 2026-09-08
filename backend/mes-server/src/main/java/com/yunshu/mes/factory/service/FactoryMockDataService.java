package com.yunshu.mes.factory.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.factory.dto.ProductionLineRequest;
import com.yunshu.mes.factory.dto.ShiftRequest;
import com.yunshu.mes.factory.dto.WorkshopRequest;
import com.yunshu.mes.factory.dto.WorkstationRequest;
import com.yunshu.mes.factory.vo.LineVO;
import com.yunshu.mes.factory.vo.ShiftVO;
import com.yunshu.mes.factory.vo.WorkshopVO;
import com.yunshu.mes.factory.vo.WorkstationVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 工厂资源域 Mock 实现，与 seed SQL 保持一致。写操作在无库时不可用。
 */
@Service
public class FactoryMockDataService implements FactoryService {

    private static final List<WorkshopVO> WORKSHOPS = new ArrayList<>(List.of(
            new WorkshopVO(1L, "WS-001", "电风扇总装车间", "ENABLED")
    ));

    private static final List<LineVO> LINES = new ArrayList<>(List.of(
            new LineVO(1L, "L-001", "台扇组装一线", "电风扇总装车间", "800", "台/班", "ENABLED",
                    300.0, 10.0, -1000.0),
            new LineVO(2L, "L-002", "落地扇组装一线", "电风扇总装车间", "600", "台/班", "ENABLED",
                    529.0, 10.0, -2500.0)
    ));

    private static final List<WorkstationVO> WORKSTATIONS = new ArrayList<>(List.of(
            new WorkstationVO(1L, "ST-001", "底座装配工位", "ASSEMBLY", 1L, "台扇组装一线", "ENABLED"),
            new WorkstationVO(2L, "ST-002", "电机安装工位", "ASSEMBLY", 1L, "台扇组装一线", "ENABLED"),
            new WorkstationVO(3L, "ST-003", "扇叶装配工位", "ASSEMBLY", 1L, "台扇组装一线", "ENABLED"),
            new WorkstationVO(4L, "ST-004", "整机测试工位", "TEST", 1L, "台扇组装一线", "ENABLED"),
            new WorkstationVO(5L, "ST-005", "底座装配工位", "ASSEMBLY", 2L, "落地扇组装一线", "ENABLED"),
            new WorkstationVO(6L, "ST-006", "整机测试工位", "TEST", 2L, "落地扇组装一线", "ENABLED")
    ));

    private static final List<ShiftVO> SHIFTS = new ArrayList<>(List.of(
            new ShiftVO(1L, "DAY", "白班", "08:00", "16:00", "ENABLED"),
            new ShiftVO(2L, "NIGHT", "夜班", "16:00", "00:00", "ENABLED")
    ));

    private final AtomicLong workshopNextId = new AtomicLong(10);
    private final AtomicLong lineNextId = new AtomicLong(10);
    private final AtomicLong stationNextId = new AtomicLong(10);
    private final AtomicLong shiftNextId = new AtomicLong(10);

    // ---- 车间 ----
    @Override
    public List<WorkshopVO> listWorkshops() {
        return new ArrayList<>(WORKSHOPS);
    }

    @Override
    public Optional<WorkshopVO> getWorkshopById(Long id) {
        return WORKSHOPS.stream().filter(w -> w.workshopId().equals(id)).findFirst();
    }

    @Override
    public WorkshopVO createWorkshop(WorkshopRequest req) {
        throw mockWriteError();
    }

    @Override
    public WorkshopVO updateWorkshop(Long id, WorkshopRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteWorkshop(Long id) {
        throw mockWriteError();
    }

    // ---- 产线 ----
    @Override
    public List<LineVO> listLines() {
        return new ArrayList<>(LINES);
    }

    @Override
    public Optional<LineVO> getLineById(Long id) {
        return LINES.stream().filter(l -> l.lineId().equals(id)).findFirst();
    }

    @Override
    public LineVO createLine(ProductionLineRequest req) {
        throw mockWriteError();
    }

    @Override
    public LineVO updateLine(Long id, ProductionLineRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteLine(Long id) {
        throw mockWriteError();
    }

    // ---- 工位 ----
    @Override
    public List<WorkstationVO> listWorkstations() {
        return new ArrayList<>(WORKSTATIONS);
    }

    @Override
    public Optional<WorkstationVO> getWorkstationById(Long id) {
        return WORKSTATIONS.stream().filter(s -> s.stationId().equals(id)).findFirst();
    }

    @Override
    public WorkstationVO createWorkstation(WorkstationRequest req) {
        throw mockWriteError();
    }

    @Override
    public WorkstationVO updateWorkstation(Long id, WorkstationRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteWorkstation(Long id) {
        throw mockWriteError();
    }

    // ---- 班次 ----
    @Override
    public List<ShiftVO> listShifts() {
        return new ArrayList<>(SHIFTS);
    }

    @Override
    public Optional<ShiftVO> getShiftById(Long id) {
        return SHIFTS.stream().filter(s -> s.shiftId().equals(id)).findFirst();
    }

    @Override
    public ShiftVO createShift(ShiftRequest req) {
        throw mockWriteError();
    }

    @Override
    public ShiftVO updateShift(Long id, ShiftRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteShift(Long id) {
        throw mockWriteError();
    }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
