package com.yunshu.mes.production.compat.repository;

import com.yunshu.mes.reporting.compat.AnalyticsJdbcHelper;
import com.yunshu.mes.system.compat.SysCompatHelper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ProcardCompatRepository {

    private static final String CARD_BASE = """
            SELECT card_id, card_code, workorder_id, workorder_code, workorder_name, batch_code,
                   item_code, item_name, specification, unit_of_measure, quantity_transfered, status, remark, create_time
            FROM pro_card
            """;

    private final JdbcTemplate jdbc;

    public ProcardCompatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> searchCards(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(CARD_BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("cardCode"))) {
            sql.append(" AND card_code LIKE ?");
            args.add("%" + params.get("cardCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("workorderCode"))) {
            sql.append(" AND workorder_code LIKE ?");
            args.add("%" + params.get("workorderCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("batchCode"))) {
            sql.append(" AND batch_code LIKE ?");
            args.add("%" + params.get("batchCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemCode"))) {
            sql.append(" AND item_code LIKE ?");
            args.add("%" + params.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemName"))) {
            sql.append(" AND item_name LIKE ?");
            args.add("%" + params.get("itemName").trim() + "%");
        }
        sql.append(" ORDER BY card_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::procardRow, args.toArray());
    }

    public long countCards(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pro_card WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("cardCode"))) {
            sql.append(" AND card_code LIKE ?");
            args.add("%" + params.get("cardCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("workorderCode"))) {
            sql.append(" AND workorder_code LIKE ?");
            args.add("%" + params.get("workorderCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("batchCode"))) {
            sql.append(" AND batch_code LIKE ?");
            args.add("%" + params.get("batchCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemCode"))) {
            sql.append(" AND item_code LIKE ?");
            args.add("%" + params.get("itemCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("itemName"))) {
            sql.append(" AND item_name LIKE ?");
            args.add("%" + params.get("itemName").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public Optional<Map<String, Object>> findCardById(Long cardId) {
        List<Map<String, Object>> rows = jdbc.query(CARD_BASE + " WHERE card_id = ?",
                AnalyticsJdbcHelper::procardRow, cardId);
        return rows.stream().findFirst();
    }

    public Long insertCard(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO pro_card (card_code, workorder_id, workorder_code, workorder_name, batch_code,
                      item_code, item_name, specification, unit_of_measure, quantity_transfered, status, remark, create_by, create_time)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, SysCompatHelper.str(body.get("cardCode")));
            ps.setObject(i++, SysCompatHelper.longObj(body.get("workorderId")));
            ps.setString(i++, SysCompatHelper.str(body.get("workorderCode")));
            ps.setString(i++, SysCompatHelper.str(body.get("workorderName")));
            ps.setString(i++, SysCompatHelper.str(body.get("batchCode")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("itemCode"), "ITEM"));
            ps.setString(i++, SysCompatHelper.strOr(body.get("itemName"), "产品"));
            ps.setString(i++, SysCompatHelper.str(body.get("specification")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("unitOfMeasure"), "PCS"));
            ps.setObject(i++, AnalyticsJdbcHelper.doubleObj(body.get("quantityTransfered")));
            ps.setString(i++, SysCompatHelper.strOr(body.get("status"), "PROCESSING"));
            ps.setString(i++, SysCompatHelper.str(body.get("remark")));
            ps.setString(i++, SysCompatHelper.currentUsername());
            ps.setTimestamp(i, now);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateCard(Map<String, Object> body) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("""
                UPDATE pro_card SET card_code=?, workorder_code=?, workorder_name=?, batch_code=?,
                  item_code=?, item_name=?, quantity_transfered=?, status=?, remark=?, update_by=?, update_time=?
                WHERE card_id=?
                """,
                SysCompatHelper.str(body.get("cardCode")),
                SysCompatHelper.str(body.get("workorderCode")),
                SysCompatHelper.str(body.get("workorderName")),
                SysCompatHelper.str(body.get("batchCode")),
                SysCompatHelper.str(body.get("itemCode")),
                SysCompatHelper.str(body.get("itemName")),
                AnalyticsJdbcHelper.doubleObj(body.get("quantityTransfered")),
                SysCompatHelper.str(body.get("status")),
                SysCompatHelper.str(body.get("remark")),
                SysCompatHelper.currentUsername(),
                now,
                SysCompatHelper.longVal(body.get("cardId")));
    }

    public int deleteCards(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        for (Long id : ids) {
            jdbc.update("DELETE FROM pro_card_process WHERE card_id = ?", id);
        }
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        return jdbc.update("DELETE FROM pro_card WHERE card_id IN (" + placeholders + ")", ids.toArray());
    }

    public List<Map<String, Object>> listCardProcess(Long cardId) {
        return jdbc.query("""
                SELECT record_id, card_id, card_code, seq_num, process_code, process_name,
                       input_time, output_time, quantity_input, quantity_output, workstation_name, user_name
                FROM pro_card_process WHERE card_id = ? ORDER BY seq_num
                """, AnalyticsJdbcHelper::cardProcessRow, cardId);
    }

    public List<Map<String, Object>> listSnProcess(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT record_id, sn_id, sn_code, seq_num, process_code, process_name,
                       input_time, output_time, workstation_name
                FROM pro_sn_process WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("snCode"))) {
            sql.append(" AND sn_code LIKE ?");
            args.add("%" + params.get("snCode").trim() + "%");
        }
        if (StringUtils.hasText(params.get("snId"))) {
            sql.append(" AND sn_id = ?");
            args.add(Long.parseLong(params.get("snId").trim()));
        }
        sql.append(" ORDER BY record_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), AnalyticsJdbcHelper::snProcessRow, args.toArray());
    }

    public long countSnProcess(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pro_sn_process WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(params.get("snCode"))) {
            sql.append(" AND sn_code LIKE ?");
            args.add("%" + params.get("snCode").trim() + "%");
        }
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }
}
