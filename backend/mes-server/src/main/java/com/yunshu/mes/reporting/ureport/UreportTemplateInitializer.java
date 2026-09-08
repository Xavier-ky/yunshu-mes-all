package com.yunshu.mes.reporting.ureport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * 启动时将 classpath:ureport-templates/*.ureport.xml 同步到 ureport_file_tbl。
 * <ul>
 *   <li>文件名不存在 → 安装</li>
 *   <li>存在但 content_ 为空 → 从 classpath 补写</li>
 *   <li>存在且 content 非空 → 仅 force-template-upgrade=true 时覆盖</li>
 * </ul>
 */
@Component
public class UreportTemplateInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UreportTemplateInitializer.class);
    private static final String PATTERN = "classpath:ureport-templates/*.ureport.xml";

    private final JdbcTemplate jdbc;
    private final boolean forceTemplateUpgrade;

    public UreportTemplateInitializer(
            JdbcTemplate jdbc,
            @Value("${mes.ureport.force-template-upgrade:false}") boolean forceTemplateUpgrade) {
        this.jdbc = jdbc;
        this.forceTemplateUpgrade = forceTemplateUpgrade;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PATTERN);
        if (resources.length == 0) {
            log.warn("No ureport templates found at {}", PATTERN);
            return;
        }
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || filename.isBlank()) {
                continue;
            }
            ContentState state = contentState(filename);
            if (state == ContentState.MISSING) {
                String content = readUtf8(resource);
                install(filename, content, now);
                log.info("UReport template installed: {}", filename);
                continue;
            }
            if (state == ContentState.EMPTY || forceTemplateUpgrade) {
                String content = readUtf8(resource);
                upgrade(filename, content, now);
                log.info(
                        forceTemplateUpgrade
                                ? "UReport template upgraded: {}"
                                : "UReport template filled empty content: {}",
                        filename);
                continue;
            }
            log.info("UReport template exists: {}", filename);
        }
    }

    private ContentState contentState(String name) {
        ListRow row = jdbc.query(
                        "SELECT content_ FROM ureport_file_tbl WHERE name_ = ? LIMIT 1",
                        rs -> {
                            if (!rs.next()) {
                                return null;
                            }
                            return new ListRow(rs.getString(1));
                        },
                        name);
        if (row == null) {
            return ContentState.MISSING;
        }
        if (!StringUtils.hasText(row.content())) {
            return ContentState.EMPTY;
        }
        return ContentState.PRESENT;
    }

    private void install(String name, String content, Timestamp now) {
        jdbc.update(
                "INSERT INTO ureport_file_tbl (name_, content_, create_time_, update_time_) VALUES (?,?,?,?)",
                name, content, now, now);
    }

    private void upgrade(String name, String content, Timestamp now) {
        jdbc.update(
                "UPDATE ureport_file_tbl SET content_ = ?, update_time_ = ? WHERE name_ = ?",
                content, now, name);
    }

    private static String readUtf8(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private enum ContentState {
        MISSING,
        EMPTY,
        PRESENT
    }

    private record ListRow(String content) {}
}
