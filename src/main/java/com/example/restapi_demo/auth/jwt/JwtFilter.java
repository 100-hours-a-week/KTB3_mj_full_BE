package com.example.restapi_demo.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
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

        String uri = request.getRequestURI();
        String method = request.getMethod();

        String jwt = resolveToken(request);

        // 1. 토큰이 아예 없는 경우 → 그냥 다음 필터로 진행 (익명 사용자로 취급)
        if (!StringUtils.hasText(jwt)) {
            log.debug("[JwtFilter] 토큰 없음 → uri={}, method={}", uri, method);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 토큰이 있는 경우 → 유효성 검사 후 잘못되면 여기서 바로 401 응답
        try {
            log.debug("[JwtFilter] 토큰 감지 → 유효성 검사 시작. uri={}, method={}", uri, method);

            boolean valid = tokenProvider.validateToken(jwt);

            if (!valid) {
                log.warn("[JwtFilter] 유효하지 않은 JWT 토큰. uri={}, method={}", uri, method);
                writeUnauthorized(response, "invalid_token");
                return; // 더 이상 필터 체인 진행 X
            }

            // 3. 유효한 토큰이면 Authentication 생성해서 SecurityContext에 저장
            Authentication authentication = tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[JwtFilter] JWT 인증 성공. user={}, uri={}", authentication.getName(), uri);

            // 4. 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 예상치 못한 예외 발생 시에도 401로 응답
            log.error("[JwtFilter] 토큰 처리 중 예외 발생. uri={}, method={}", uri, method, e);
            writeUnauthorized(response, "invalid_token");
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 401 Unauthorized 응답 헬퍼
     * - 이미 응답이 commit된 경우에는 아무 것도 하지 않음
     * - 간단한 JSON 바디로 에러 코드 내려줌
     */
    private void writeUnauthorized(HttpServletResponse response, String code) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 프로젝트에서 쓰는 ApiResponse 형식에 맞추고 싶으면 이 부분 나중에 변경 가능
        String body = String.format("{\"code\":\"%s\",\"data\":null}", code);
        response.getWriter().write(body);
    }
}
