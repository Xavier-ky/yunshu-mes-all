package com.yunshu.mes.config;

import com.yunshu.mes.cal.compat.security.CalMutationAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CalWebMvcConfig implements WebMvcConfigurer {

    private final CalMutationAuthorizationInterceptor calMutationAuthorizationInterceptor;

    public CalWebMvcConfig(CalMutationAuthorizationInterceptor calMutationAuthorizationInterceptor) {
        this.calMutationAuthorizationInterceptor = calMutationAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(calMutationAuthorizationInterceptor)
                .addPathPatterns("/api/mes/cal/**");
    }
}
