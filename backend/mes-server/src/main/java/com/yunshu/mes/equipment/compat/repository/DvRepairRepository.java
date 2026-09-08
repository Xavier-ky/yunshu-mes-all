package com.yunshu.mes.equipment.compat.repository;

import com.yunshu.mes.equipment.compat.DvJdbcHelper;
import com.yunshu.mes.equipment.compat.DvSchemas;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
public class DvRepairRepository {

    private static final String BASE = """
            SELECT repair_id, repair_code, repair_name,
                   machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id,
                   require_date, finish_date, confirm_date, repair_result,
                   accepted_id, accepted_name, accepted_by,
                   confirm_id, confirm_name, confirm_by,
                   source_doc_type, source_doc_id, source_doc_code, status,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM dv_repair
            """;

    private final JdbcTemplate jdbc;

    public DvRepairRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> search(Map<String, String> params, int offset, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        sql.append(" ORDER BY repair_id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), (rs, n) -> DvSchemas.mapRepair(rs), args.toArray());
    }

    public long count(Map<String, String> params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM dv_repair WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendFilters(sql, args, params);
        Long c = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return c == null ? 0 : c;
    }

    public List<Map<String, Object>> listByMachineryCode(String machineryCode) {
        if (!StringUtils.hasText(machineryCode)) {
            return List.of();
        }
        return jdbc.query(BASE + " WHERE machinery_code = ? ORDER BY repair_id DESC",
                (rs, n) -> DvSchemas.mapRepair(rs), machineryCode.trim());
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE repair_id = ?",
                (rs, n) -> DvSchemas.mapRepair(rs), id);
        return rows.stream().findFirst();
    }

    public boolean codeExists(String code, Long excludeId) {
        if (excludeId == null) {
            Long c = jdbc.queryForObject("SELECT COUNT(*) FROM dv_repair WHERE repair_code = ?",
                    Long.class, code);
            return c != null && c > 0;
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dv_repair WHERE repair_code = ? AND repair_id <> ?",
                Long.class, code, excludeId);
        return c != null && c > 0;
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dv_repair (
                      repair_code, repair_name,
                      machinery_id, machinery_code, machinery_name, machinery_brand, machinery_spec, machinery_type_id,
                      require_date, finish_date, confirm_date, repair_result,
                      accepted_id, accepted_name, accepted_by,
                      confirm_id, confirm_name, confirm_by,
                      source_doc_type, source_doc_id, source_doc_code, status, remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, DvJdbcHelper.str(body.get("repairCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("repairName")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("machineryId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryCode")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machineryBrand")));
            ps.setString(i++, DvJdbcHelper.str(body.get("machinerySpec")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("machineryTypeId")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("requireDate")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("finishDate")));
            ps.setTimestamp(i++, DvJdbcHelper.parseTs(body.get("confirmDate")));
            ps.setString(i++, DvJdbcHelper.str(body.get("repairResult")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("acceptedId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("acceptedName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("acceptedBy")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("confirmId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("confirmName")));
            ps.setString(i++, DvJdbcHelper.str(body.get("confirmBy")));
            ps.setString(i++, DvJdbcHelper.str(body.get("sourceDocType")));
            ps.setObject(i++, DvJdbcHelper.longObj(body.get("sourceDocId")));
            ps.setString(i++, DvJdbcHelper.str(body.get("sourceDocCode")));
            ps.setString(i++, DvJdbcHelper.strOr(body.get("status"), "PREPARE"));
            ps.setString(i++, DvJdbcHelper.str(body.get("remark")));
            ps.setString(i++, DvJdbcHelper.str(body.get("createBy")));
            ps.setTimestamp(i, DvJdbcHelper.now());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        return jdbc.update("""
                UPDATE dv_repair SET
                  repair_code=?, repair_name=?,
                  machinery_id=?, machinery_code=?, machinery_name=?, machinery_brand=?, machinery_spec=?, machinery_type_id=?,
                  require_date=?, finish_date=?, confirm_date=?, repair_result=?,
                  accepted_id=?, accepted_name=?, accepted_by=?,
                  confirm_id=?, confirm_name=?, confirm_by=?,
                  source_doc_type=?, source_doc_id=?, source_doc_code=?, status=?, remark=?,
                  update_by=?, update_time=NOW(3)
                WHERE repair_id=?
                """,
                DvJdbcHelper.str(body.get("repairCode")),
                DvJdbcHelper.str(body.get("repairName")),
                DvJdbcHelper.longObj(body.get("machineryId")),
                DvJdbcHelper.str(body.get("machineryCode")),
                DvJdbcHelper.str(body.get("machineryName")),
                DvJdbcHelper.str(body.get("machineryBrand")),
                DvJdbcHelper.str(body.get("machinerySpec")),
                DvJdbcHelper.longObj(body.get("machineryTypeId")),
                DvJdbcHelper.parseTs(body.get("requireDate")),
                DvJdbcHelper.parseTs(body.get("finishDate")),
                DvJdbcHelper.parseTs(body.get("confirmDate")),
                DvJdbcHelper.str(body.get("repairResult")),
                DvJdbcHelper.longObj(body.get("acceptedId")),
                DvJdbcHelper.str(body.get("acceptedName")),
                DvJdbcHelper.str(body.get("acceptedBy")),
                DvJdbcHelper.longObj(body.get("confirmId")),
                DvJdbcHelper.str(body.get("confirmName")),
                DvJdbcHelper.str(body.get("confirmBy")),
                DvJdbcHelper.str(body.get("sourceDocType")),
                DvJdbcHelper.longObj(body.get("sourceDocId")),
                DvJdbcHelper.str(body.get("sourceDocCode")),
                DvJdbcHelper.str(body.get("status")),
                DvJdbcHelper.str(body.get("remark")),
                DvJdbcHelper.str(body.get("updateBy")),
                DvJdbcHelper.longVal(body.get("repairId")));
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        String ph = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.update("DELETE FROM dv_repair WHERE repair_id IN (" + ph + ")", ids.toArray());
    }

    private void appendFilters(StringBuilder sql, List<Object> args, Map<String, String> params) {
        DvJdbcHelper.like(sql, args, "repair_code", params.get("repairCode"));
        DvJdbcHelper.like(sql, args, "repair_name", params.get("repairName"));
        DvJdbcHelper.like(sql, args, "machinery_code", params.get("machineryCode"));
        DvJdbcHelper.like(sql, args, "machinery_name", params.get("machineryName"));
        DvJdbcHelper.eq(sql, args, "status", params.get("status"));
        DvJdbcHelper.eqLong(sql, args, "machinery_id", params, "machineryId");
    }
}
