package com.yunshu.mes.factory.service;

import com.yunshu.mes.factory.dto.ProductionLineRequest;
import com.yunshu.mes.factory.dto.ShiftRequest;
import com.yunshu.mes.factory.dto.WorkshopRequest;
import com.yunshu.mes.factory.dto.WorkstationRequest;
import com.yunshu.mes.factory.vo.LineVO;
import com.yunshu.mes.factory.vo.ShiftVO;
import com.yunshu.mes.factory.vo.WorkshopVO;
import com.yunshu.mes.factory.vo.WorkstationVO;
import java.util.List;
import java.util.Optional;

/**
 * 工厂资源域服务接口：车间、产线、工位、班次。
 */
public interface FactoryService {

    // ---- 车间 ----
    List<WorkshopVO> listWorkshops();

    Optional<WorkshopVO> getWorkshopById(Long id);

    WorkshopVO createWorkshop(WorkshopRequest req);

    WorkshopVO updateWorkshop(Long id, WorkshopRequest req);

    void deleteWorkshop(Long id);

    // ---- 产线 ----
    List<LineVO> listLines();

    Optional<LineVO> getLineById(Long id);

    LineVO createLine(ProductionLineRequest req);

    LineVO updateLine(Long id, ProductionLineRequest req);

    void deleteLine(Long id);

    // ---- 工位 ----
    List<WorkstationVO> listWorkstations();

    Optional<WorkstationVO> getWorkstationById(Long id);

    WorkstationVO createWorkstation(WorkstationRequest req);

    WorkstationVO updateWorkstation(Long id, WorkstationRequest req);

    void deleteWorkstation(Long id);

    // ---- 班次 ----
    List<ShiftVO> listShifts();

    Optional<ShiftVO> getShiftById(Long id);

    ShiftVO createShift(ShiftRequest req);

    ShiftVO updateShift(Long id, ShiftRequest req);

    void deleteShift(Long id);
}
