package com.example.restapi_demo.auth.controller;

import com.example.restapi_demo.common.api.ApiResponse;
import com.example.restapi_demo.auth.dto.LoginRequest;
import com.example.restapi_demo.auth.jwt.TokenProvider;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenProvider tokenProvider;

    @Operation(summary = "로그인", description = "JWT 토큰 발급")
    @PostMapping(value = "/login")
    public ResponseEntity<ApiResponse<Object>> login(@RequestBody LoginRequest req) {
        try {
            if (req == null || req.getEmail() == null || req.getEmail().isBlank()
                    || req.getPassword() == null || req.getPassword().isBlank()) {

                log.warn("로그인 요청 검증 실패: 이메일 또는 비밀번호가 비어있음");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("invalid_request", null));
            }

            // 1. 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            // 2. 권한 정보 추출
            String authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            log.info("로그인 성공: email={}, authorities={}", authentication.getName(), authorities);

            // 3. 사용자 정보 조회 (토큰 생성 & 응답 데이터용)
            User user = userService.findByEmail(req.getEmail());
            if (user == null) {
                log.error("로그인 후 사용자 조회 실패: email={}", req.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>("internal_server_error", null));
            }

            // 4. JWT 토큰 생성 (userId, email, nickname, authorities 포함)
            String token = tokenProvider.createToken(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    authorities
            );

            // 5. 응답 반환
            return ResponseEntity.ok(
                    new ApiResponse<>("loginSuccess", Map.of(
                            "token", token,
                            "user_id", user.getId(),
                            "email", user.getEmail(),
                            "nickname", user.getNickname()
                    ))
            );

        } catch (BadCredentialsException e) {
            log.warn("로그인 실패 (BadCredentials): email={}, message={}",
                    req != null ? req.getEmail() : "null", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("invalid_credentials", null));

        } catch (Exception e) {
            log.error("로그인 처리 중 예외 발생: email={}",
                    req != null ? req.getEmail() : "null", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout() {
        // JWT는 Stateless이므로 서버에서 할 일 없음
        // 클라이언트에서 토큰 삭제하면 됨
        return ResponseEntity.ok(new ApiResponse<>("logout_success", null));
    }
}
