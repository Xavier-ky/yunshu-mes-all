package com.yunshu.mes.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentLlmProperties.class)
public class LlmConfig {
}
