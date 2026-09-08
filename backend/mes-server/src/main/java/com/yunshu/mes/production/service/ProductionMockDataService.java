package com.yunshu.mes.production.service;

import com.yunshu.mes.common.exception.BusinessException;
import com.yunshu.mes.common.exception.ErrorCode;
import com.yunshu.mes.production.dto.ProductSnRequest;
import com.yunshu.mes.production.dto.ProductionCompletionRequest;
import com.yunshu.mes.production.dto.ProductionReportRequest;
import com.yunshu.mes.production.vo.ProductSnVO;
import com.yunshu.mes.production.vo.ProductionCompletionVO;
import com.yunshu.mes.production.vo.ProductionReportVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 生产执行域 Mock 实现，与 seed SQL 保持一致。
 */
@Service
public class ProductionMockDataService implements ProductionService {

    private static final List<ProductSnVO> PRODUCT_SNS = new ArrayList<>(List.of(
            new ProductSnVO(1L, "FAN-20240101-0001", "台扇 FS-40", "WO-20240101-001", "CREATED"),
            new ProductSnVO(2L, "FAN-20240101-0002", "台扇 FS-40", "WO-20240101-001", "PRODUCING"),
            new ProductSnVO(3L, "FAN-20240102-0001", "落地扇 FS-50", "WO-20240102-001", "COMPLETED")
    ));

    private static final List<ProductionReportVO> REPORTS = new ArrayList<>(List.of(
            new ProductionReportVO(1L, "RPT-001", "WO-20240101-001", "底座装配", "NORMAL", "100", "0", "2024-01-15 08:30"),
            new ProductionReportVO(2L, "RPT-002", "WO-20240101-001", "电机安装", "NORMAL", "98", "2", "2024-01-15 09:45")
    ));

    private static final List<ProductionCompletionVO> COMPLETIONS = new ArrayList<>(List.of(
            new ProductionCompletionVO(1L, "CMP-001", "WO-20240101-001", "500", "3", "2024-01-15 18:00", "CREATED")
    ));

    // ---- 产品 SN ----
    @Override public List<ProductSnVO> listProductSns() { return new ArrayList<>(PRODUCT_SNS); }
    @Override public Optional<ProductSnVO> getProductSnById(Long id) { return PRODUCT_SNS.stream().filter(s -> s.snId().equals(id)).findFirst(); }
    @Override public ProductSnVO createProductSn(ProductSnRequest req) { throw mockWriteError(); }
    @Override public ProductSnVO updateProductSn(Long id, ProductSnRequest req) { throw mockWriteError(); }
    @Override public void deleteProductSn(Long id) { throw mockWriteError(); }

    // ---- 生产报工 ----
    @Override public List<ProductionReportVO> listReports() { return new ArrayList<>(REPORTS); }
    @Override public Optional<ProductionReportVO> getReportById(Long id) { return REPORTS.stream().filter(r -> r.reportId().equals(id)).findFirst(); }
    @Override public ProductionReportVO createReport(ProductionReportRequest req) { throw mockWriteError(); }
    @Override public ProductionReportVO updateReport(Long id, ProductionReportRequest req) { throw mockWriteError(); }
    @Override public void deleteReport(Long id) { throw mockWriteError(); }

    // ---- 完工单 ----
    @Override public List<ProductionCompletionVO> listCompletions() { return new ArrayList<>(COMPLETIONS); }
    @Override public Optional<ProductionCompletionVO> getCompletionById(Long id) { return COMPLETIONS.stream().filter(c -> c.completionId().equals(id)).findFirst(); }
    @Override public ProductionCompletionVO createCompletion(ProductionCompletionRequest req) { throw mockWriteError(); }
    @Override public ProductionCompletionVO updateCompletion(Long id, ProductionCompletionRequest req) { throw mockWriteError(); }
    @Override public void deleteCompletion(Long id) { throw mockWriteError(); }

    private BusinessException mockWriteError() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "写操作需连接 MySQL，当前为 Mock 模式");
    }
}
