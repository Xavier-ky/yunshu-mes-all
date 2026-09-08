package com.yunshu.mes.andon.service.impl;

import com.yunshu.mes.andon.dto.AndonReasonRequest;
import com.yunshu.mes.andon.dto.AndonTypeRequest;
import com.yunshu.mes.andon.repository.AndonEventRepository;
import com.yunshu.mes.andon.repository.AndonReasonRepository;
import com.yunshu.mes.andon.repository.AndonTypeRepository;
import com.yunshu.mes.andon.service.AndonMockDataService;
import com.yunshu.mes.andon.service.AndonService;
import com.yunshu.mes.andon.vo.AndonEventVO;
import com.yunshu.mes.andon.vo.AndonReasonVO;
import com.yunshu.mes.andon.vo.AndonTypeVO;
import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 安灯域服务门面：优先 JDBC 仓储，异常时回退 Mock。
 */
@Service
@Primary
public class AndonServiceImpl implements AndonService {

    private static final Logger log = LoggerFactory.getLogger(AndonServiceImpl.class);

    private final AndonTypeRepository typeRepo;
    private final AndonReasonRepository reasonRepo;
    private final AndonEventRepository eventRepo;
    private final AndonMockDataService mock;

    public AndonServiceImpl(AndonTypeRepository typeRepo, AndonReasonRepository reasonRepo,
                            AndonEventRepository eventRepo, AndonMockDataService mock) {
        this.typeRepo = typeRepo;
        this.reasonRepo = reasonRepo;
        this.eventRepo = eventRepo;
        this.mock = mock;
    }

    // ==================== 安灯类型 ====================

    @Override
    public List<AndonTypeVO> listTypes() {
        try { return typeRepo.findAll(); }
        catch (DataAccessException e) { log.warn("安灯类型列表回退 Mock：{}", e.getMessage()); return mock.listTypes(); }
    }

    @Override
    public Optional<AndonTypeVO> getTypeById(Long id) {
        try { return typeRepo.findById(id).or(() -> mock.getTypeById(id)); }
        catch (DataAccessException e) { log.warn("安灯类型详情回退 Mock：{}", e.getMessage()); return mock.getTypeById(id); }
    }

    @Override
    public AndonTypeVO createType(AndonTypeRequest req) {
        try {
            Long id = typeRepo.insert(req.typeCode(), req.typeName(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建安灯类型失败");
            return typeRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建安灯类型后回查失败"));
        } catch (DataAccessException e) { log.warn("创建安灯类型回退 Mock：{}", e.getMessage()); return mock.createType(req); }
    }

    @Override
    public AndonTypeVO updateType(Long id, AndonTypeRequest req) {
        try {
            typeRepo.update(id, req.typeCode(), req.typeName(), req.status());
            return typeRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "安灯类型不存在"));
        } catch (DataAccessException e) { log.warn("更新安灯类型回退 Mock：{}", e.getMessage()); return mock.updateType(id, req); }
    }

    @Override
    public void deleteType(Long id) {
        try { typeRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除安灯类型回退 Mock：{}", e.getMessage()); mock.deleteType(id); }
    }

    // ==================== 安灯原因 ====================

    @Override
    public List<AndonReasonVO> listReasons() {
        try { return reasonRepo.findAll(); }
        catch (DataAccessException e) { log.warn("安灯原因列表回退 Mock：{}", e.getMessage()); return mock.listReasons(); }
    }

    @Override
    public Optional<AndonReasonVO> getReasonById(Long id) {
        try { return reasonRepo.findById(id).or(() -> mock.getReasonById(id)); }
        catch (DataAccessException e) { log.warn("安灯原因详情回退 Mock：{}", e.getMessage()); return mock.getReasonById(id); }
    }

    @Override
    public AndonReasonVO createReason(AndonReasonRequest req) {
        try {
            Long id = reasonRepo.insert(req.reasonCode(), req.reasonName(), req.reasonCategory(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建安灯原因失败");
            return reasonRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建安灯原因后回查失败"));
        } catch (DataAccessException e) { log.warn("创建安灯原因回退 Mock：{}", e.getMessage()); return mock.createReason(req); }
    }

    @Override
    public AndonReasonVO updateReason(Long id, AndonReasonRequest req) {
        try {
            reasonRepo.update(id, req.reasonCode(), req.reasonName(), req.reasonCategory(), req.status());
            return reasonRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "安灯原因不存在"));
        } catch (DataAccessException e) { log.warn("更新安灯原因回退 Mock：{}", e.getMessage()); return mock.updateReason(id, req); }
    }

    @Override
    public void deleteReason(Long id) {
        try { reasonRepo.delete(id); }
        catch (DataAccessException e) { log.warn("删除安灯原因回退 Mock：{}", e.getMessage()); mock.deleteReason(id); }
    }

    // ==================== 安灯事件 ====================

    @Override
    public List<AndonEventVO> listEvents() {
        try { return eventRepo.findAll(); }
        catch (DataAccessException e) { log.warn("安灯事件列表回退 Mock：{}", e.getMessage()); return mock.listEvents(); }
    }
}
