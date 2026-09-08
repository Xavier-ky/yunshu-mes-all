package com.yunshu.mes.production.compat.repository;

import com.yunshu.mes.production.compat.ProAndonSchemas;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProAndonConfigRepository {

    private static final String BASE = """
            SELECT config_id, andon_reason, andon_level,
                   handler_role_id, handler_role_name,
                   handler_user_id, handler_user_name, handler_nick_name,
                   remark, attr1, attr2, attr3, attr4,
                   create_by, create_time, update_by, update_time
            FROM pro_andon_config
            """;

    private final JdbcTemplate jdbc;

    public ProAndonConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.query(BASE + " ORDER BY config_id", (rs, n) -> ProAndonSchemas.mapConfig(rs));
    }

    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> rows = jdbc.query(BASE + " WHERE config_id = ?",
                (rs, n) -> ProAndonSchemas.mapConfig(rs), id);
        return rows.stream().findFirst();
    }

    public Long insert(Map<String, Object> body) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO pro_andon_config (
                      andon_reason, andon_level,
                      handler_role_id, handler_role_name,
                      handler_user_id, handler_user_name, handler_nick_name,
                      remark, create_by, create_time
                    ) VALUES (?,?,?,?,?,?,?,?,?,NOW(3))
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, str(body.get("andonReason")));
            ps.setString(i++, str(body.get("andonLevel")));
            ps.setObject(i++, body.get("handlerRoleId"));
            ps.setString(i++, str(body.get("handlerRoleName")));
            ps.setObject(i++, body.get("handlerUserId"));
            ps.setString(i++, str(body.get("handlerUserName")));
            ps.setString(i++, str(body.get("handlerNickName")));
            ps.setString(i++, str(body.get("remark")));
            ps.setString(i, str(body.get("createBy")));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int update(Map<String, Object> body) {
        return jdbc.update("""
                UPDATE pro_andon_config SET
                  andon_reason=?, andon_level=?,
                  handler_role_id=?, handler_role_name=?,
                  handler_user_id=?, handler_user_name=?, handler_nick_name=?,
                  remark=?, update_by=?, update_time=NOW(3)
                WHERE config_id=?
                """,
                str(body.get("andonReason")), str(body.get("andonLevel")),
                body.get("handlerRoleId"), str(body.get("handlerRoleName")),
                body.get("handlerUserId"), str(body.get("handlerUserName")), str(body.get("handlerNickName")),
                str(body.get("remark")), str(body.get("updateBy")), body.get("configId"));
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM pro_andon_config WHERE config_id = ?", id);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
