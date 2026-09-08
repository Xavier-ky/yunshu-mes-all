package com.yunshu.mes.reporting.service;

import com.yunshu.mes.reporting.vo.ReportDefinitionVO;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 报表分析查询：报表定义清单。
 */
@Service
public class ReportingService {

    private static final String DEFINITIONS_SQL = """
            SELECT report_id, report_code, report_name, report_type, status
            FROM report_definition ORDER BY report_id
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public ReportingService(ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public List<ReportDefinitionVO> listDefinitions() {
        JdbcTemplate jdbc = jdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return List.of();
        }
        return jdbc.query(DEFINITIONS_SQL, (rs, n) -> new ReportDefinitionVO(
                rs.getLong("report_id"),
                rs.getString("report_code"),
                rs.getString("report_name"),
                rs.getString("report_type"),
                rs.getString("status")));
    }
}
