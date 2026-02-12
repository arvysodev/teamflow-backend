package com.teamflow.teamflow.backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return (PageableHandlerMethodArgumentResolver resolver) -> {
            resolver.setMaxPageSize(50);
            resolver.setOneIndexedParameters(false);
        };
    }
}
