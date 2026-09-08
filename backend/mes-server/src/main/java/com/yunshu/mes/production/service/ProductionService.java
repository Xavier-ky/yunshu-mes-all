package com.yunshu.mes.production.service;

import com.yunshu.mes.production.dto.ProductSnRequest;
import com.yunshu.mes.production.dto.ProductionCompletionRequest;
import com.yunshu.mes.production.dto.ProductionReportRequest;
import com.yunshu.mes.production.vo.ProductSnVO;
import com.yunshu.mes.production.vo.ProductionCompletionVO;
import com.yunshu.mes.production.vo.ProductionReportVO;
import java.util.List;
import java.util.Optional;

/**
 * 生产执行域服务接口：产品 SN、生产报工、完工单。
 */
public interface ProductionService {

    // ---- 产品 SN ----
    List<ProductSnVO> listProductSns();

    Optional<ProductSnVO> getProductSnById(Long id);

    ProductSnVO createProductSn(ProductSnRequest req);

    ProductSnVO updateProductSn(Long id, ProductSnRequest req);

    void deleteProductSn(Long id);

    // ---- 生产报工 ----
    List<ProductionReportVO> listReports();

    Optional<ProductionReportVO> getReportById(Long id);

    ProductionReportVO createReport(ProductionReportRequest req);

    ProductionReportVO updateReport(Long id, ProductionReportRequest req);

    void deleteReport(Long id);

    // ---- 完工单 ----
    List<ProductionCompletionVO> listCompletions();

    Optional<ProductionCompletionVO> getCompletionById(Long id);

    ProductionCompletionVO createCompletion(ProductionCompletionRequest req);

    ProductionCompletionVO updateCompletion(Long id, ProductionCompletionRequest req);

    void deleteCompletion(Long id);
}
