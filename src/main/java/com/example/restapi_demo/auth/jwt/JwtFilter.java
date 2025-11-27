package com.example.restapi_demo.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ==========================
        // 1. 필터 진입 로그
        // ==========================
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        System.out.println("\n========== [JwtFilter] 요청 진입 ==========");
        System.out.println("요청 URI    : " + uri);
        System.out.println("HTTP 메서드 : " + method);
        System.out.println("Auth 헤더   : " + authHeader);
        System.out.println("=========================================");

        // 2. 헤더에서 JWT 토큰 추출
        String jwt = resolveToken(request);
        System.out.println("[JwtFilter] resolveToken 결과(jwt): " + jwt);

        try {
            // 3. 토큰 유효성 검사
            if (StringUtils.hasText(jwt)) {
                System.out.println("[JwtFilter] 토큰 문자열 존재 → 유효성 검사 시작");

                boolean valid = tokenProvider.validateToken(jwt);
                System.out.println("[JwtFilter] tokenProvider.validateToken(jwt) = " + valid);

                if (valid) {
                    // 4. 토큰이 유효하면 Authentication 객체를 가져와서 SecurityContext에 저장
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("✅ JWT 인증 성공: " + authentication.getName());
                } else {
                    System.out.println("❌ JWT 토큰이 유효하지 않음 (validateToken=false)");
                }
            } else {
                System.out.println("[JwtFilter] JWT 토큰이 비어있음(Authorization 헤더 없음 또는 형식 불일치)");
            }
        } catch (Exception e) {
            System.out.println("❗ [JwtFilter] 토큰 처리 중 예외 발생: " + e.getClass().getName());
            System.out.println("메시지: " + e.getMessage());
            e.printStackTrace();
            // 예외가 발생하더라도 필터 체인은 계속 흘려보냄
        }

        // 5. 다음 필터로 진행
        System.out.println("[JwtFilter] 다음 필터로 체인 진행\n");
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
