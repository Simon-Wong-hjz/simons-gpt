package com.simwong.simonsgpt.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import static com.simwong.simonsgpt.utils.StringUtil.camelToSnake;

@Configuration
public class ServiceConfiguration {
    /**
     * Cross Origin Configurations
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(source);
    }

    @Bean
    public NamingStrategy namingStrategy() {
        return new NamingStrategy() {
            @NotNull
            @Override
            public String getColumnName(@NotNull RelationalPersistentProperty property) {
                return camelToSnake(property.getName());
            }
        };
    }
}
