package com.yunshu.mes.production.service.impl;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.production.dto.ProductSnRequest;
import com.yunshu.mes.production.dto.ProductionCompletionRequest;
import com.yunshu.mes.production.dto.ProductionReportRequest;
import com.yunshu.mes.production.repository.ProductSnRepository;
import com.yunshu.mes.production.repository.ProductionReportRepository;
import com.yunshu.mes.production.service.ProductionMockDataService;
import com.yunshu.mes.production.service.ProductionService;
import com.yunshu.mes.production.vo.ProductSnVO;
import com.yunshu.mes.production.vo.ProductionCompletionVO;
import com.yunshu.mes.production.vo.ProductionReportVO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ProductionServiceImpl implements ProductionService {

    private static final Logger log = LoggerFactory.getLogger(ProductionServiceImpl.class);

    private final ProductSnRepository snRepo;
    private final ProductionReportRepository reportRepo;
    private final ProductionMockDataService mock;

    public ProductionServiceImpl(ProductSnRepository snRepo, ProductionReportRepository reportRepo,
                                  ProductionMockDataService mock) {
        this.snRepo = snRepo;
        this.reportRepo = reportRepo;
        this.mock = mock;
    }

    // ---- 产品 SN ----
    @Override public List<ProductSnVO> listProductSns() {
        try { return snRepo.findAll(); } catch (DataAccessException e) { log.warn("SN列表回退Mock：{}", e.getMessage()); return mock.listProductSns(); }
    }
    @Override public Optional<ProductSnVO> getProductSnById(Long id) {
        try { return snRepo.findById(id).or(() -> mock.getProductSnById(id)); } catch (DataAccessException e) { log.warn("SN详情回退Mock：{}", e.getMessage()); return mock.getProductSnById(id); }
    }
    @Override public ProductSnVO createProductSn(ProductSnRequest req) {
        try {
            Long id = snRepo.insert(req.snCode(), req.productId(), req.workOrderId(), req.status());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建SN失败");
            return snRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建SN后回查失败"));
        } catch (DataAccessException e) { log.warn("创建SN回退Mock：{}", e.getMessage()); return mock.createProductSn(req); }
    }
    @Override public ProductSnVO updateProductSn(Long id, ProductSnRequest req) {
        try { snRepo.update(id, req.snCode(), req.productId(), req.workOrderId(), req.status()); return snRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SN不存在")); }
        catch (DataAccessException e) { log.warn("更新SN回退Mock：{}", e.getMessage()); return mock.updateProductSn(id, req); }
    }
    @Override public void deleteProductSn(Long id) {
        try { snRepo.delete(id); } catch (DataAccessException e) { log.warn("删除SN回退Mock：{}", e.getMessage()); mock.deleteProductSn(id); }
    }

    // ---- 生产报工 ----
    @Override public List<ProductionReportVO> listReports() {
        try { return reportRepo.findAll(); } catch (DataAccessException e) { log.warn("报工列表回退Mock：{}", e.getMessage()); return mock.listReports(); }
    }
    @Override public Optional<ProductionReportVO> getReportById(Long id) {
        try { return reportRepo.findById(id).or(() -> mock.getReportById(id)); } catch (DataAccessException e) { log.warn("报工详情回退Mock：{}", e.getMessage()); return mock.getReportById(id); }
    }
    @Override public ProductionReportVO createReport(ProductionReportRequest req) {
        try {
            Long id = reportRepo.insert(req.reportNo(), req.workOrderId(), req.stepId(), req.operatorId(), req.reportType(), req.goodQty(), req.defectQty(), req.remark());
            if (id == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建报工失败");
            return reportRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "创建报工后回查失败"));
        } catch (DataAccessException e) { log.warn("创建报工回退Mock：{}", e.getMessage()); return mock.createReport(req); }
    }
    @Override public ProductionReportVO updateReport(Long id, ProductionReportRequest req) {
        try { reportRepo.update(id, req.reportNo(), req.workOrderId(), req.stepId(), req.reportType(), req.goodQty(), req.defectQty(), req.remark()); return reportRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报工不存在")); }
        catch (DataAccessException e) { log.warn("更新报工回退Mock：{}", e.getMessage()); return mock.updateReport(id, req); }
    }
    @Override public void deleteReport(Long id) {
        try { reportRepo.delete(id); } catch (DataAccessException e) { log.warn("删除报工回退Mock：{}", e.getMessage()); mock.deleteReport(id); }
    }

    // ---- 完工单（已退役 native production_completion，改走 pro_feedback / wm_product_recpt）----
    @Override public List<ProductionCompletionVO> listCompletions() { return List.of(); }
    @Override public Optional<ProductionCompletionVO> getCompletionById(Long id) { return Optional.empty(); }
    @Override public ProductionCompletionVO createCompletion(ProductionCompletionRequest req) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "完工单 API 已退役，请使用 pro_feedback / wm_product_recpt");
    }
    @Override public ProductionCompletionVO updateCompletion(Long id, ProductionCompletionRequest req) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "完工单 API 已退役，请使用 pro_feedback / wm_product_recpt");
    }
    @Override public void deleteCompletion(Long id) {}
}
