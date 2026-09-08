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
public class ProProcessContentService {

    private final JdbcTemplate jdbc;

    public ProProcessContentService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> list(Map<String, String> params) {
        int pn = PageUtil.pageNum(params);
        int ps = PageUtil.pageSize(params);
        return WmSqlHelper.list(jdbc, "pro_process_content", "content_id", MdDocSchemas.PRO_PROCESS_CONTENT,
                MdDocSchemas.filter(params, "processId"),
                PageUtil.offset(pn, ps), ps);
    }

    public long count(Map<String, String> params) {
        return WmSqlHelper.count(jdbc, "pro_process_content", MdDocSchemas.PRO_PROCESS_CONTENT,
                MdDocSchemas.filter(params, "processId"));
    }

    public Map<String, Object> getById(Long id) {
        return WmSqlHelper.getById(jdbc, "pro_process_content", "content_id", "contentId",
                MdDocSchemas.PRO_PROCESS_CONTENT, id);
    }

    @Transactional
    public Long create(Map<String, Object> body) {
        return WmSqlHelper.insert(jdbc, "pro_process_content", MdDocSchemas.PRO_PROCESS_CONTENT, body);
    }

    @Transactional
    public int update(Map<String, Object> body) {
        Long id = Long.parseLong(String.valueOf(body.get("contentId")));
        return WmSqlHelper.update(jdbc, "pro_process_content", "content_id", MdDocSchemas.PRO_PROCESS_CONTENT, body, id);
    }

    @Transactional
    public int delete(Long id) {
        return WmSqlHelper.delete(jdbc, "pro_process_content", "content_id", id);
    }
}
