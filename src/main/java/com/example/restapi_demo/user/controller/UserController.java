package com.example.restapi_demo.user.controller;

import com.example.restapi_demo.common.api.ApiResponse;
import com.example.restapi_demo.user.dto.FieldErrorDTO;
import com.example.restapi_demo.user.dto.PasswordChangeRequest;
import com.example.restapi_demo.user.dto.UpdateUserRequest;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    private String currentEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        if ("anonymousUser".equals(principal)) return null;

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    private User currentUserOrNull() {
        String email = currentEmailOrNull();
        if (email == null || email.isBlank()) return null;
        return userService.findByEmail(email);
    }

    @Operation(
            summary = "회원가입",
            description = "이메일/비밀번호/닉네임으로 회원을 등록합니다. 성공 시 data에 user_id 반환."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "register_success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "invalid_request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "internal_server_error")
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Object>> signup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "회원가입 요청 바디",
                    content = @Content(schema = @Schema(implementation = SignupBody.class))
            )
            @RequestBody Map<String, String> req
    ) {
        try {
            String email            = req.get("email");
            String password         = req.get("password");
            String passwordConfirm  = req.get("password_confirm");
            String nickname         = req.get("nickname");
            String profileImageUrl  = req.get("profile_image");

            User user = userService.register(email, password, passwordConfirm, nickname, profileImageUrl);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>("invalid_request", null));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>("register_success", Map.of("user_id", user.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인 사용자의 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "read_success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "auth_required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "user_not_found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "internal_server_error")
    })
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> me(HttpServletRequest httpRequest) {
        try {
            System.out.println("\n========== GET /api/users/me 호출 ==========");

            // 1. 세션 확인
            HttpSession session = httpRequest.getSession(false);
            System.out.println("[세션 정보]");
            System.out.println("세션 존재: " + (session != null));
            if (session != null) {
                System.out.println("세션 ID: " + session.getId());
                Object secContext = session.getAttribute("SPRING_SECURITY_CONTEXT");
                System.out.println("세션의 SecurityContext: " + secContext);
            }

            // 2. SecurityContextHolder 확인
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("\n[Authentication 정보]");
            System.out.println("auth 객체: " + auth);
            if (auth != null) {
                System.out.println("isAuthenticated: " + auth.isAuthenticated());
                System.out.println("Principal: " + auth.getPrincipal());
            }

            // 3. 쿠키 확인
            Cookie[] cookies = httpRequest.getCookies();
            System.out.println("\n[Cookie 정보]");
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    System.out.println("Cookie: " + cookie.getName() + " = " + cookie.getValue());
                }
            } else {
                System.out.println("⚠️ 쿠키 없음!");
            }

            System.out.println("==========================================\n");

            User u = currentUserOrNull();
            if (u == null) {
                System.out.println("❌ currentUserOrNull() 반환 null - 401 반환");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>("auth_required", null));
            }

            System.out.println("✅ 인증 성공 - 사용자: " + u.getEmail());

            Map<String, Object> data = new HashMap<>();
            data.put("id", u.getId());
            data.put("email", u.getEmail());
            data.put("nickname", u.getNickname());
            data.put("profile_image", u.getProfileImageUrl() != null ? u.getProfileImageUrl() : "");

            return ResponseEntity.ok(new ApiResponse<>("read_success", data));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(summary = "내 정보 수정", description = "닉네임(필수, 최대 20자)과 프로필 이미지(선택)를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "update_success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "invalid_request",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FieldErrorDTO.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "auth_required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "user_not_found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "internal_server_error")
    })
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Object>> updateMe(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = UpdateUserRequest.class))
            )
            @RequestBody UpdateUserRequest req,
            HttpServletRequest httpRequest
    ) {
        try {
            System.out.println("\n========== PATCH /api/users/me 호출 ==========");
            System.out.println("요청 바디 - nickname: " + req.getNickname() + ", profile_image: " + req.getProfile_image());

            User exist = currentUserOrNull();
            if (exist == null) {
                System.out.println("❌ currentUserOrNull() 반환 null - 401 반환");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>("auth_required", null));
            }

            System.out.println("✅ 인증 성공 - 사용자: " + exist.getEmail());

            List<FieldErrorDTO> errors = new ArrayList<>();
            String nickname = req.getNickname();
            String profileImageUrl = req.getProfile_image();

            if (nickname == null || nickname.isBlank()) {
                errors.add(new FieldErrorDTO("nickname", "blank"));
            } else if (nickname.length() > 20) {
                errors.add(new FieldErrorDTO("nickname", "too_long"));
            }
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("invalid_request", errors));
            }

            User updated = userService.updateProfile(exist.getId(), nickname, profileImageUrl);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("user_not_found", null));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", updated.getId());
            data.put("nickname", updated.getNickname());
            data.put("profile_image", updated.getProfileImageUrl());
            data.put("updated_at", LocalDateTime.now());

            System.out.println("✅ 회원정보 수정 완료");
            return ResponseEntity.ok(new ApiResponse<>("update_success", data));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(summary = "회원 탈퇴", description = "성공 시 204(No Content) 반환.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "delete_success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "auth_required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "user_not_found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "internal_server_error")
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Object>> deleteMe(HttpServletRequest httpRequest) {
        try {
            User exist = currentUserOrNull();
            if (exist == null) {
                System.out.println("❌ currentUserOrNull() 반환 null - 401 반환");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>("auth_required", null));
            }

            boolean deleted = userService.deleteMe(exist.getId());
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("user_not_found", null));
            }

            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("delete_success", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "8~20자, 대/소문자/숫자/특수문자 각 1개 이상 포함하는 새 비밀번호로 변경합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "password_changed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "invalid_request",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = FieldErrorDTO.class)))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "auth_required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "user_not_found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "internal_server_error")
    })
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Object>> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = PasswordChangeRequest.class))
            )
            @RequestBody PasswordChangeRequest req,
            HttpServletRequest httpRequest
    ) {
        try {
            User exist = currentUserOrNull();
            if (exist == null) {
                System.out.println("❌ currentUserOrNull() 반환 null - 401 반환");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse<>("auth_required", null));
            }

            var result = userService.changePassword(exist.getId(), req.getNew_password(), req.getNew_password_confirm());
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("user_not_found", null));
            }
            if (!result.success) {
                List<FieldErrorDTO> errors = new ArrayList<>();
                for (String[] e : result.errors) errors.add(new FieldErrorDTO(e[0], e[1]));
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("invalid_request", errors));
            }

            return ResponseEntity.ok(new ApiResponse<>("password_changed", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @GetMapping("/search/nickname")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> searchByNickname(
            @Parameter(description = "검색 키워드", example = "test")
            @RequestParam String keyword
    ) {
        try {
            var users = userService.findByNicknameKeyword(keyword);
            var data = users.stream().map(u -> Map.of(
                    "id", u.getId(),
                    "email", u.getEmail(),
                    "nickname", u.getNickname(),
                    "profile_image", u.getProfileImageUrl()
            )).toList();
            return ResponseEntity.ok(new ApiResponse<>("search_success", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @GetMapping("/exists/email")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> existsByEmail(
            @Parameter(description = "확인할 이메일", example = "test@example.com")
            @RequestParam String email
    ) {
        try {
            boolean exists = userService.existsByEmail(email);
            return ResponseEntity.ok(new ApiResponse<>("check_success", Map.of("exists", exists)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @GetMapping("/count/nickname")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Object>> countByNickname(
            @Parameter(description = "조회할 닉네임", example = "startup")
            @RequestParam String nickname
    ) {
        try {
            long count = userService.countByNickname(nickname);
            return ResponseEntity.ok(new ApiResponse<>("count_success", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Schema(description = "회원가입 요청 바디(문서용)")
    static class SignupBody {
        @Schema(description = "이메일", example = "test@startupcode.kr")
        public String email;
        @Schema(description = "비밀번호", example = "Abcd1234!")
        public String password;
        @Schema(description = "비밀번호 확인", example = "Abcd1234!")
        public String password_confirm;
        @Schema(description = "닉네임", example = "startup")
        public String nickname;
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/avatar.jpg")
        public String profile_image;
    }
}