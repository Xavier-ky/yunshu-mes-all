package com.yunshu.mes.andon.service;

import com.yunshu.mes.andon.dto.AndonReasonRequest;
import com.yunshu.mes.andon.dto.AndonTypeRequest;
import com.yunshu.mes.andon.vo.AndonReasonVO;
import com.yunshu.mes.andon.vo.AndonEventVO;
import com.yunshu.mes.andon.vo.AndonTypeVO;
import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * 安灯域 Mock 实现，与 seed SQL 保持一致。写操作在无库时不可用。
 */
@Service
public class AndonMockDataService implements AndonService {

    private static final List<AndonTypeVO> TYPES = new ArrayList<>(List.of(
            new AndonTypeVO(1L, "AT-001", "设备异常", "ENABLED"),
            new AndonTypeVO(2L, "AT-002", "质量异常", "ENABLED"),
            new AndonTypeVO(3L, "AT-003", "物料短缺", "ENABLED"),
            new AndonTypeVO(4L, "AT-004", "安全问题", "ENABLED")
    ));

    private static final List<AndonReasonVO> REASONS = new ArrayList<>(List.of(
            new AndonReasonVO(1L, "AR-001", "电机过热", "设备异常", "ENABLED"),
            new AndonReasonVO(2L, "AR-002", "传送带卡料", "设备异常", "ENABLED"),
            new AndonReasonVO(3L, "AR-003", "尺寸超差", "质量异常", "ENABLED"),
            new AndonReasonVO(4L, "AR-004", "外观缺陷", "质量异常", "ENABLED"),
            new AndonReasonVO(5L, "AR-005", "原料断供", "物料短缺", "ENABLED"),
            new AndonReasonVO(6L, "AR-006", "人员受伤", "安全问题", "ENABLED")
    ));

    private static final List<AndonEventVO> EVENTS = new ArrayList<>();

    private final AtomicLong typeNextId = new AtomicLong(10);
    private final AtomicLong reasonNextId = new AtomicLong(10);

    // ---- 安灯类型 ----
    @Override
    public List<AndonTypeVO> listTypes() {
        return new ArrayList<>(TYPES);
    }

    @Override
    public Optional<AndonTypeVO> getTypeById(Long id) {
        return TYPES.stream().filter(t -> t.typeId().equals(id)).findFirst();
    }

    @Override
    public AndonTypeVO createType(AndonTypeRequest req) {
        throw mockWriteError();
    }

    @Override
    public AndonTypeVO updateType(Long id, AndonTypeRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteType(Long id) {
        throw mockWriteError();
    }

    // ---- 安灯原因 ----
    @Override
    public List<AndonReasonVO> listReasons() {
        return new ArrayList<>(REASONS);
    }

    @Override
    public Optional<AndonReasonVO> getReasonById(Long id) {
        return REASONS.stream().filter(r -> r.reasonId().equals(id)).findFirst();
    }

    @Override
    public AndonReasonVO createReason(AndonReasonRequest req) {
        throw mockWriteError();
    }

    @Override
    public AndonReasonVO updateReason(Long id, AndonReasonRequest req) {
        throw mockWriteError();
    }

    @Override
    public void deleteReason(Long id) {
        throw mockWriteError();
    }

    // ---- 安灯事件 ----
    @Override
    public List<AndonEventVO> listEvents() {
        return new ArrayList<>(EVENTS);
    }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                "写操作需连接 MySQL 且 fan_mes 库已执行迁移脚本，当前为 Mock 模式");
    }
}
