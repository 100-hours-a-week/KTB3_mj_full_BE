package com.example.restapi_demo.auth.controller;

import com.example.restapi_demo.common.api.ApiResponse;
import com.example.restapi_demo.auth.dto.LoginRequest;
import com.example.restapi_demo.auth.jwt.TokenProvider;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

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
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("invalid_request", null));
            }

            System.out.println("\n========== 로그인 시도 ==========");
            System.out.println("이메일: " + req.getEmail());
            System.out.println("비밀번호 길이: " + req.getPassword().length());
            System.out.println("================================\n");

            // 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            // 권한 정보 추출
            String authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            System.out.println("\n========== 로그인 성공 ==========");
            System.out.println("인증된 사용자: " + authentication.getName());
            System.out.println("권한: " + authorities);

            // JWT 토큰 생성
            String token = tokenProvider.createToken(req.getEmail(), authorities);
            System.out.println("JWT 토큰 생성됨: " + token.substring(0, 20) + "...");
            System.out.println("================================\n");

            // 사용자 정보 조회
            User user = userService.findByEmail(req.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>("internal_server_error", null));
            }

            return ResponseEntity.ok(
                    new ApiResponse<>("loginSuccess", Map.of(
                            "token", token,
                            "user_id", user.getId(),
                            "email", user.getEmail(),
                            "nickname", user.getNickname()
                    ))
            );

        } catch (BadCredentialsException e) {
            System.out.println("\n========== 로그인 실패 (BadCredentials) ==========");
            System.out.println("이메일: " + req.getEmail());
            System.out.println("원인: " + e.getMessage());
            System.out.println("=================================================\n");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>("invalid_credentials", null));
        } catch (Exception e) {
            System.out.println("\n========== 로그인 예외 발생 ==========");
            System.out.println("예외 타입: " + e.getClass().getName());
            System.out.println("메시지: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=====================================\n");

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