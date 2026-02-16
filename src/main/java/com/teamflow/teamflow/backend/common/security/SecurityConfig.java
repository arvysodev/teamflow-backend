package com.teamflow.teamflow.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamflow.teamflow.backend.auth.security.JwtAuthFilter;
import com.teamflow.teamflow.backend.auth.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        return new ProblemDetailAccessDeniedHandler(objectMapper);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
            ProblemDetailAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        http.exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().denyAll()
        );

        return http.build();
    }
}
