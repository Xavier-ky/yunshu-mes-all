package com.yunshu.mes.process.compat.service;

import com.yunshu.mes.inventory.compat.PageUtil;
import com.yunshu.mes.inventory.compat.WmSqlHelper;
import com.yunshu.mes.masterdata.compat.MdDocSchemas;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProProcessService {

    private final JdbcTemplate jdbc;

    public ProProcessService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "pro_process", "process_id", MdDocSchemas.PRO_PROCESS,
                MdDocSchemas.filter(params, "processCode", "processName", "enableFlag"),
                PageUtil.offset(pn, ps), ps, false);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "pro_process", MdDocSchemas.PRO_PROCESS,
                MdDocSchemas.filter(params, "processCode", "processName", "enableFlag"));
    }

    public List<Map<String, Object>> listAll() {
        return jdbc.query("SELECT * FROM pro_process ORDER BY process_id ASC",
                (rs, n) -> WmSqlHelper.getById(jdbc, "pro_process", "process_id", "processId",
                        MdDocSchemas.PRO_PROCESS, rs.getLong("process_id")));
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "pro_process", "process_id", "processId", MdDocSchemas.PRO_PROCESS, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "pro_process", MdDocSchemas.PRO_PROCESS, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("processId")));
        return WmSqlHelper.update(jdbc, "pro_process", "process_id", MdDocSchemas.PRO_PROCESS, body, id);
    }

    @Transactional
    public int delete(Long id) {
        jdbc.update("DELETE FROM pro_process_content WHERE process_id = ?", id);
        return WmSqlHelper.delete(jdbc, "pro_process", "process_id", id);
    }
}
