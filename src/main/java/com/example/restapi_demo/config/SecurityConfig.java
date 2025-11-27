package com.example.restapi_demo.config;

import com.example.restapi_demo.auth.handler.JwtAccessDeniedHandler;
import com.example.restapi_demo.auth.handler.JwtAuthenticationEntryPoint;
import com.example.restapi_demo.auth.jwt.JwtFilter;
import com.example.restapi_demo.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정
                .cors(Customizer.withDefaults())

                // CSRF 비활성화 (JWT는 헤더로 전송하므로 CSRF 공격 불가)
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 폼 로그인, HTTP Basic 인증 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)  // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403
                )

                // 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ★★★ 회원가입 관련 API 모두 허용 ★★★
                        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/exists/email").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/count/nickname").permitAll()

                        // 로그인
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // 게시글 조회
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()

                        // Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 나머지 /api/** 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll()
                )

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}