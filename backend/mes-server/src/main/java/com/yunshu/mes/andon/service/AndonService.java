package com.yunshu.mes.andon.service;

import com.yunshu.mes.andon.dto.AndonReasonRequest;
import com.yunshu.mes.andon.dto.AndonTypeRequest;
import com.yunshu.mes.andon.vo.AndonEventVO;
import com.yunshu.mes.andon.vo.AndonReasonVO;
import com.yunshu.mes.andon.vo.AndonTypeVO;
import java.util.List;
import java.util.Optional;

/**
 * 安灯域服务接口：安灯类型、异常原因、安灯事件。
 */
public interface AndonService {

    // ---- 安灯类型 ----
    List<AndonTypeVO> listTypes();

    Optional<AndonTypeVO> getTypeById(Long id);

    AndonTypeVO createType(AndonTypeRequest req);

    AndonTypeVO updateType(Long id, AndonTypeRequest req);

    void deleteType(Long id);

    // ---- 安灯原因 ----
    List<AndonReasonVO> listReasons();

    Optional<AndonReasonVO> getReasonById(Long id);

    AndonReasonVO createReason(AndonReasonRequest req);

    AndonReasonVO updateReason(Long id, AndonReasonRequest req);

    void deleteReason(Long id);

    // ---- 安灯事件 ----
    List<AndonEventVO> listEvents();
}
