package com.yunshu.mes.quality.compat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * 检验单完结回写 WM/PRO（FINISHED 状态语义）。
 */
public final class QcFinishWriteback {

    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_PREPARE = "PREPARE";
    public static final String ACCEPT = "ACCEPT";
    public static final String QUALITY_OK = "OK";
    public static final String QUALITY_NG = "NG";
    public static final String QUALITY_NT = "NT";

    private QcFinishWriteback() {}

    public static boolean isFinished(Map<String, Object> body) {
        return STATUS_FINISHED.equals(str(body.get("status")));
    }

    public static String requirePrepareOrError(JdbcTemplate jdbc, String table, String idCol, Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status FROM " + table + " WHERE " + idCol + " = ?", id);
        if (rows.isEmpty()) {
            return "单据不存在";
        }
        Object st = rows.get(0).get("status");
        if (st != null && !STATUS_PREPARE.equals(String.valueOf(st))) {
            return "只能删除草稿状态单据";
        }
        return null;
    }

    public static String validateQualifiedQty(Map<String, Object> body) {
        BigDecimal check = dec(body.get("quantityCheck"));
        BigDecimal ok = dec(body.get("quantityQualified"));
        BigDecimal ng = dec(body.get("quantityUnqualified"));
        if (ok.compareTo(BigDecimal.ZERO) <= 0 && ng.compareTo(BigDecimal.ZERO) <= 0) {
            return "请填写合格品/不合格品数量";
        }
        if (check.compareTo(BigDecimal.ZERO) > 0 && check.compareTo(ok.add(ng)) != 0) {
            return "检测数量与合格品/不合格品总和数量不匹配";
        }
        return null;
    }

    public static void onOqcFinished(JdbcTemplate jdbc, Map<String, Object> body) {
        Long sourceLineId = longOrNull(body.get("sourceLineId"));
        Long sourceDocId = longOrNull(body.get("sourceDocId"));
        Long oqcId = longOrNull(body.get("oqcId"));
        String oqcCode = str(body.get("oqcCode"));
        String checkResult = str(body.get("checkResult"));
        if (sourceLineId == null) {
            return;
        }
        String qs = ACCEPT.equalsIgnoreCase(checkResult) ? QUALITY_OK : QUALITY_NG;
        jdbc.update(
                """
                UPDATE wm_product_sales_line
                SET quality_status = ?, oqc_id = ?, oqc_code = ?, update_time = NOW(3)
                WHERE line_id = ?
                """,
                qs, oqcId, oqcCode, sourceLineId);

        if (sourceDocId == null) {
            return;
        }
        List<Map<String, Object>> lines = jdbc.queryForList(
                "SELECT quality_status FROM wm_product_sales_line WHERE sales_id = ?", sourceDocId);
        boolean allOk = !lines.isEmpty();
        boolean anyNg = false;
        for (Map<String, Object> line : lines) {
            String s = str(line.get("quality_status"));
            if (QUALITY_NG.equals(s)) {
                anyNg = true;
                allOk = false;
            }
            if (!QUALITY_OK.equals(s)) {
                allOk = false;
            }
        }
        if (anyNg) {
            jdbc.update("UPDATE wm_product_sales SET status = 'CANCELED', update_time = NOW(3) WHERE sales_id = ?",
                    sourceDocId);
        } else if (allOk) {
            jdbc.update("UPDATE wm_product_sales SET status = 'UNSHIPPING', update_time = NOW(3) WHERE sales_id = ?",
                    sourceDocId);
        }
    }

    public static void onRqcFinished(JdbcTemplate jdbc, Map<String, Object> body) {
        Long sourceDocId = longOrNull(body.get("sourceDocId"));
        Long sourceLineId = longOrNull(body.get("sourceLineId"));
        String sourceDocType = str(body.get("sourceDocType"));
        if (sourceDocId == null || sourceLineId == null) {
            return;
        }
        BigDecimal ok = dec(body.get("quantityQualified"));
        BigDecimal ng = dec(body.get("quantityUnqualified"));

        if ("RT_ISSUE".equals(sourceDocType) || "RTISSUE".equals(sourceDocType)) {
            finishRtIssueLine(jdbc, sourceDocId, sourceLineId, ok, ng);
        } else if ("RT_SALES".equals(sourceDocType) || "RTSALSE".equals(sourceDocType) || "RT_SALSE".equals(sourceDocType)) {
            finishRtSalesLine(jdbc, sourceDocId, sourceLineId, ok, ng);
        }
    }

    private static void finishRtIssueLine(JdbcTemplate jdbc, Long rtId, Long lineId, BigDecimal ok, BigDecimal ng) {
        if (ng.compareTo(BigDecimal.ZERO) == 0) {
            jdbc.update("UPDATE wm_rt_issue_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_OK, lineId);
        } else if (ok.compareTo(BigDecimal.ZERO) == 0) {
            jdbc.update("UPDATE wm_rt_issue_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_NG, lineId);
        } else {
            jdbc.update(
                    """
                    INSERT INTO wm_rt_issue_line (
                      rt_id, material_stock_id, item_id, item_code, item_name, specification,
                      unit_of_measure, unit_name, quantity_rt, batch_id, batch_code,
                      ipqc_id, ipqc_code, qc_flag, quality_status, remark, create_time)
                    SELECT rt_id, material_stock_id, item_id, item_code, item_name, specification,
                           unit_of_measure, unit_name, ?, batch_id, batch_code,
                           ipqc_id, ipqc_code, qc_flag, ?, remark, NOW(3)
                    FROM wm_rt_issue_line WHERE line_id = ?
                    """,
                    ng, QUALITY_NG, lineId);
            jdbc.update(
                    "UPDATE wm_rt_issue_line SET quality_status = ?, quantity_rt = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_OK, ok, lineId);
        }
        Integer pending = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM wm_rt_issue_line
                WHERE rt_id = ?
                  AND (quality_status IS NULL OR quality_status = '' OR quality_status IN ('WAIT','NT'))
                """,
                Integer.class, rtId);
        if (pending != null && pending == 0) {
            jdbc.update("UPDATE wm_rt_issue SET status = 'UNSTOCK', update_time = NOW(3) WHERE rt_id = ?", rtId);
        }
    }

    private static void finishRtSalesLine(JdbcTemplate jdbc, Long rtId, Long lineId, BigDecimal ok, BigDecimal ng) {
        if (ng.compareTo(BigDecimal.ZERO) == 0) {
            jdbc.update("UPDATE wm_rt_sales_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_OK, lineId);
        } else if (ok.compareTo(BigDecimal.ZERO) == 0) {
            jdbc.update("UPDATE wm_rt_sales_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_NG, lineId);
        } else {
            jdbc.update(
                    """
                    INSERT INTO wm_rt_sales_line (
                      rt_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
                      batch_id, batch_code, quantity_rted, quality_status, remark, create_time)
                    SELECT rt_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
                           batch_id, batch_code, ?, ?, remark, NOW(3)
                    FROM wm_rt_sales_line WHERE line_id = ?
                    """,
                    ng, QUALITY_NG, lineId);
            jdbc.update(
                    "UPDATE wm_rt_sales_line SET quality_status = ?, quantity_rted = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_OK, ok, lineId);
        }
        Integer pending = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM wm_rt_sales_line
                WHERE rt_id = ?
                  AND (quality_status IS NULL OR quality_status = '' OR quality_status IN ('WAIT','NT'))
                """,
                Integer.class, rtId);
        if (pending != null && pending == 0) {
            jdbc.update("UPDATE wm_rt_sales SET status = 'UNSTOCK', update_time = NOW(3) WHERE rt_id = ?", rtId);
        }
    }

    /**
     * IPQC 完结：产出行质量拆分 + 报工待检清零。
     * 虚拟仓入库（executeProductProduce）暂未移植，仅写产出明细数量。
     */
    public static void onIpqcFinished(JdbcTemplate jdbc, Map<String, Object> body) {
        String sourceDocType = str(body.get("sourceDocType"));
        Long sourceDocId = longOrNull(body.get("sourceDocId"));
        Long sourceLineId = longOrNull(body.get("sourceLineId"));
        BigDecimal ok = dec(body.get("quantityQualified"));
        BigDecimal ng = dec(body.get("quantityUnqualified"));
        BigDecimal check = dec(body.get("quantityCheck"));

        if ("FEEDBACK".equals(sourceDocType) && sourceDocId != null) {
            jdbc.update(
                    """
                    UPDATE pro_feedback
                    SET quantity_uncheck = 0,
                        quantity_qualified = COALESCE(?, quantity_qualified),
                        quantity_unquanlified = COALESCE(?, quantity_unquanlified),
                        update_time = NOW(3)
                    WHERE record_id = ?
                    """,
                    ok, ng, sourceDocId);
            // 尝试按 feedback 关联产出单拆分
            List<Map<String, Object>> produces = jdbc.queryForList(
                    "SELECT record_id FROM wm_product_produce WHERE feedback_id = ? ORDER BY record_id DESC LIMIT 1",
                    sourceDocId);
            if (!produces.isEmpty()) {
                Long produceId = ((Number) produces.get(0).get("record_id")).longValue();
                Long lineId = sourceLineId;
                if (lineId == null) {
                    List<Map<String, Object>> lines = jdbc.queryForList(
                            """
                            SELECT line_id FROM wm_product_produce_line
                            WHERE record_id = ?
                              AND (quality_status IS NULL OR quality_status IN ('','NT','WAIT'))
                            ORDER BY line_id LIMIT 1
                            """,
                            produceId);
                    if (!lines.isEmpty()) {
                        lineId = ((Number) lines.get(0).get("line_id")).longValue();
                    }
                }
                if (lineId != null) {
                    splitProduceLine(jdbc, produceId, lineId, ok, ng);
                }
            }
            return;
        }

        // sourceDocId=产出单, sourceLineId=产出行
        if (("PRODUCT_PRODUCE".equals(sourceDocType) || "PRODUCE".equals(sourceDocType)
                || sourceDocType == null || sourceDocType.isEmpty())
                && sourceDocId != null && sourceLineId != null) {
            splitProduceLine(jdbc, sourceDocId, sourceLineId, ok, ng);
            List<Map<String, Object>> headers = jdbc.queryForList(
                    "SELECT feedback_id FROM wm_product_produce WHERE record_id = ?", sourceDocId);
            if (!headers.isEmpty() && headers.get(0).get("feedback_id") != null) {
                Long feedbackId = ((Number) headers.get(0).get("feedback_id")).longValue();
                jdbc.update(
                        "UPDATE pro_feedback SET quantity_uncheck = 0, update_time = NOW(3) WHERE record_id = ?",
                        feedbackId);
            }
        } else if (sourceDocId != null && check.compareTo(BigDecimal.ZERO) >= 0) {
            // 兼容旧 pending：仅清零报工待检
            jdbc.update(
                    "UPDATE pro_feedback SET quantity_uncheck = 0, update_time = NOW(3) WHERE record_id = ?",
                    sourceDocId);
        }
    }

    private static void splitProduceLine(JdbcTemplate jdbc, Long produceId, Long lineId, BigDecimal ok, BigDecimal ng) {
        if (ng.compareTo(BigDecimal.ZERO) > 0 && ok.compareTo(BigDecimal.ZERO) > 0) {
            jdbc.update(
                    """
                    INSERT INTO wm_product_produce_line (
                      record_id, material_stock_id, item_id, item_code, item_name, specification,
                      unit_of_measure, unit_name, quantity_produce, batch_id, batch_code,
                      quality_status, remark, create_by, create_time)
                    SELECT record_id, material_stock_id, item_id, item_code, item_name, specification,
                           unit_of_measure, unit_name, ?, batch_id, batch_code,
                           ?, remark, 'system', NOW(3)
                    FROM wm_product_produce_line WHERE line_id = ?
                    """,
                    ng, QUALITY_NG, lineId);
            jdbc.update(
                    """
                    UPDATE wm_product_produce_line
                    SET quantity_produce = ?, quality_status = ?, update_time = NOW(3)
                    WHERE line_id = ?
                    """,
                    ok, QUALITY_OK, lineId);
        } else if (ng.compareTo(BigDecimal.ZERO) > 0) {
            jdbc.update(
                    "UPDATE wm_product_produce_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_NG, lineId);
        } else {
            jdbc.update(
                    "UPDATE wm_product_produce_line SET quality_status = ?, update_time = NOW(3) WHERE line_id = ?",
                    QUALITY_OK, lineId);
        }
        // 明细（无仓位列，仅记数量）
        jdbc.update(
                """
                INSERT INTO wm_product_produce_detail (
                  line_id, record_id, item_id, item_code, item_name, specification,
                  unit_of_measure, unit_name, quantity, batch_id, batch_code, remark, create_by, update_by, create_time)
                SELECT line_id, record_id, item_id, item_code, item_name, specification,
                       unit_of_measure, unit_name, quantity_produce, batch_id, batch_code, '', 'system', 'system', NOW(3)
                FROM wm_product_produce_line WHERE line_id = ?
                  AND NOT EXISTS (
                    SELECT 1 FROM wm_product_produce_detail d WHERE d.line_id = wm_product_produce_line.line_id
                  )
                """,
                lineId);
        Integer pending = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM wm_product_produce_line
                WHERE record_id = ?
                  AND (quality_status IS NULL OR quality_status IN ('','NT','WAIT'))
                """,
                Integer.class, produceId);
        if (pending != null && pending == 0) {
            jdbc.update("UPDATE wm_product_produce SET status = 'FINISHED', update_time = NOW(3) WHERE record_id = ?",
                    produceId);
        }
    }

    public static void onIqcFinished(JdbcTemplate jdbc, Map<String, Object> body) {
        String sourceDocType = str(body.get("sourceDocType"));
        Long sourceDocId = longOrNull(body.get("sourceDocId"));
        Long sourceLineId = longOrNull(body.get("sourceLineId"));
        Long iqcId = longOrNull(body.get("iqcId"));
        String iqcCode = str(body.get("iqcCode"));
        BigDecimal ok = dec(body.get("quantityQualified"));
        BigDecimal ng = dec(body.get("quantityUnqualified"));
        if (sourceDocId == null || sourceLineId == null) {
            return;
        }
        if ("ARRIVAL_NOTICE".equals(sourceDocType)) {
            jdbc.update(
                    """
                    UPDATE wm_arrival_notice_line
                    SET iqc_id = ?, iqc_code = ?, quantity_quanlified = ?, update_time = NOW(3)
                    WHERE line_id = ?
                    """,
                    iqcId, iqcCode, ok, sourceLineId);
            Integer left = jdbc.queryForObject(
                    """
                    SELECT COUNT(*) FROM wm_arrival_notice_line
                    WHERE notice_id = ? AND iqc_check = 'Y' AND iqc_id IS NULL
                    """,
                    Integer.class, sourceDocId);
            if (left != null && left == 0) {
                jdbc.update("UPDATE wm_arrival_notice SET status = 'APPROVED', update_time = NOW(3) WHERE notice_id = ?",
                        sourceDocId);
            }
        } else if ("OUTSOURCE_RECPT".equals(sourceDocType)) {
            if (ok.compareTo(BigDecimal.ZERO) > 0) {
                jdbc.update(
                        """
                        UPDATE wm_outsource_recpt_line
                        SET iqc_id = ?, iqc_code = ?, quantity_recived = ?, quality_status = 'OK', update_time = NOW(3)
                        WHERE line_id = ?
                        """,
                        iqcId, iqcCode, ok, sourceLineId);
            }
            if (ng.compareTo(BigDecimal.ZERO) > 0) {
                if (ok.compareTo(BigDecimal.ZERO) > 0) {
                    jdbc.update(
                            """
                            INSERT INTO wm_outsource_recpt_line (
                              recpt_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
                              quantity_recived, batch_id, batch_code, produce_date, lot_number, expire_date,
                              quality_status, iqc_check, iqc_id, iqc_code, remark, create_time)
                            SELECT recpt_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name,
                                   ?, batch_id, batch_code, produce_date, lot_number, expire_date,
                                   'NG', iqc_check, ?, ?, remark, NOW(3)
                            FROM wm_outsource_recpt_line WHERE line_id = ?
                            """,
                            ng, iqcId, iqcCode, sourceLineId);
                } else {
                    jdbc.update(
                            """
                            UPDATE wm_outsource_recpt_line
                            SET iqc_id = ?, iqc_code = ?, quality_status = 'NG', update_time = NOW(3)
                            WHERE line_id = ?
                            """,
                            iqcId, iqcCode, sourceLineId);
                }
            }
            jdbc.update("UPDATE wm_outsource_recpt SET status = 'UNSTOCK', update_time = NOW(3) WHERE recpt_id = ?",
                    sourceDocId);
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static Long longOrNull(Object o) {
        if (o == null || !StringUtils.hasText(String.valueOf(o))) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal dec(Object o) {
        if (o == null || !StringUtils.hasText(String.valueOf(o))) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(o));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
