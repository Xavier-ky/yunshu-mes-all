package com.yunshu.mes.reporting.ureport;

import com.bstek.ureport.console.UReportServlet;
import com.bstek.ureport.definition.datasource.BuildinDatasource;
import jakarta.servlet.Servlet;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:ureport-context.xml")
public class UReportConfig implements BuildinDatasource {

    private final DataSource dataSource;

    public UReportConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public ServletRegistrationBean<Servlet> ureportServlet() {
        return new ServletRegistrationBean<>(new UReportServlet(), "/ureport/*");
    }

    @Override
    public String name() {
        return "内置数据源";
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("UReport 获取数据源连接失败", e);
        }
    }
}
