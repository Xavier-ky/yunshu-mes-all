package com.yunshu.mes.reporting.ureport;

import com.bstek.ureport.provider.report.ReportFile;
import com.bstek.ureport.provider.report.ReportProvider;
import com.yunshu.mes.reporting.compat.repository.UreportCompatRepository;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * UReport 报表文件 MySQL 存储，与 /api/ureportM CRUD 共用 ureport_file_tbl。
 */
@Component
public class MySQLReportProvider implements ReportProvider {

    private static final String NAME = "数据库服务器";
    private static final String PREFIX = "mysql:";

    private final UreportCompatRepository repository;

    public MySQLReportProvider(UreportCompatRepository repository) {
        this.repository = repository;
    }

    @Override
    public InputStream loadReport(String file) {
        String name = stripPrefix(file);
        String content = repository.findContentByName(name)
                .orElseThrow(() -> new IllegalArgumentException("报表不存在: " + file));
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void deleteReport(String file) {
        repository.deleteByName(stripPrefix(file));
    }

    @Override
    public List<ReportFile> getReportFiles() {
        return repository.listReportFiles().stream()
                .map(row -> new ReportFile(row.name(), new Date(row.updateTime())))
                .toList();
    }

    @Override
    public void saveReport(String file, String content) {
        repository.saveByName(stripPrefix(file), content);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean disabled() {
        return false;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    private static String stripPrefix(String file) {
        if (file != null && file.startsWith(PREFIX)) {
            return file.substring(PREFIX.length());
        }
        return file;
    }
}
