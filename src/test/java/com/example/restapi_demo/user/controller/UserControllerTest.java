package com.example.restapi_demo.user.controller;

import com.example.restapi_demo.auth.jwt.CustomUserPrincipal;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // Helper 메서드: CustomUserPrincipal로 Authentication 생성
    private UsernamePasswordAuthenticationToken createAuth(Long userId, String email, String nickname) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, nickname, authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    // ========== 회원가입 (인증 불필요) ==========

    @Nested
    @DisplayName("POST /api/users/signup - 회원가입")
    class Signup {

        @Test
        @DisplayName("성공 - 회원가입 성공")
        void success() throws Exception {
            // Given
            User mockUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .build();

            when(userService.register(
                    eq("test@example.com"),
                    eq("password123"),
                    eq("password123"),
                    eq("테스터"),
                    anyString()
            )).thenReturn(mockUser);

            // When & Then
            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "password_confirm": "password123",
                                    "nickname": "테스터",
                                    "profile_image": ""
                                }
                                """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("register_success"))
                    .andExpect(jsonPath("$.data.user_id").value(1));

            verify(userService).register(
                    eq("test@example.com"),
                    eq("password123"),
                    eq("password123"),
                    eq("테스터"),
                    anyString()
            );
        }

        @Test
        @DisplayName("실패 - 잘못된 요청")
        void fail_invalidRequest() throws Exception {
            // Given
            when(userService.register(any(), any(), any(), any(), any()))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "password_confirm": "different",
                                    "nickname": "테스터",
                                    "profile_image": ""
                                }
                                """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.register(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(post("/api/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "password_confirm": "password123",
                                    "nickname": "테스터",
                                    "profile_image": ""
                                }
                                """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 내 정보 조회 (인증 필요) ==========

    @Nested
    @DisplayName("GET /api/users/me - 내 정보 조회")
    class GetMe {

        @Test
        @DisplayName("성공 - 내 정보 조회 성공")
        void success() throws Exception {
            // Given
            User mockUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("테스터")
                    .profileImageUrl("image.jpg")
                    .build();

            when(userService.findById(1L)).thenReturn(mockUser);

            // When & Then
            mockMvc.perform(get("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터"));

            verify(userService).findById(1L);
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("인증이 필요합니다"));

            verify(userService, never()).findById(any());
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        void fail_userNotFound() throws Exception {
            // Given
            when(userService.findById(999L)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/users/me")
                            .with(authentication(createAuth(999L, "test@example.com", "테스터"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("user_not_found"));

            verify(userService).findById(999L);
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.findById(1L))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(get("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터"))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 내 정보 수정 (인증 필요) ==========

    @Nested
    @DisplayName("PATCH /api/users/me - 내 정보 수정")
    class UpdateMe {

        @Test
        @DisplayName("성공 - 내 정보 수정 성공")
        void success() throws Exception {
            // Given
            User updatedUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .nickname("새닉네임")
                    .profileImageUrl("new-image.jpg")
                    .build();

            when(userService.updateProfile(1L, "새닉네임", "new-image.jpg"))
                    .thenReturn(updatedUser);

            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "nickname": "새닉네임",
                                    "profile_image": "new-image.jpg"
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("update_success"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"));

            verify(userService).updateProfile(1L, "새닉네임", "new-image.jpg");
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "nickname": "새닉네임",
                                    "profile_image": "image.jpg"
                                }
                                """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("인증이 필요합니다"));

            verify(userService, never()).updateProfile(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 닉네임 공백")
        void fail_nicknameBlank() throws Exception {
            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "nickname": "",
                                    "profile_image": "image.jpg"
                                }
                                """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"))
                    .andExpect(jsonPath("$.data[0].field").value("nickname"))
                    .andExpect(jsonPath("$.data[0].reason").value("blank"));

            verify(userService, never()).updateProfile(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 닉네임 길이 초과")
        void fail_nicknameTooLong() throws Exception {
            // Given
            String longNickname = "A".repeat(21);

            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                {
                                    "nickname": "%s",
                                    "profile_image": "image.jpg"
                                }
                                """, longNickname)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"))
                    .andExpect(jsonPath("$.data[0].field").value("nickname"))
                    .andExpect(jsonPath("$.data[0].reason").value("too_long"));

            verify(userService, never()).updateProfile(any(), any(), any());
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        void fail_userNotFound() throws Exception {
            // Given
            when(userService.updateProfile(999L, "새닉네임", "image.jpg"))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .with(authentication(createAuth(999L, "test@example.com", "테스터")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "nickname": "새닉네임",
                                    "profile_image": "image.jpg"
                                }
                                """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("user_not_found"));

            verify(userService).updateProfile(999L, "새닉네임", "image.jpg");
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.updateProfile(any(), any(), any()))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(patch("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "nickname": "새닉네임",
                                    "profile_image": "image.jpg"
                                }
                                """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 회원 탈퇴 (인증 필요) ==========

    @Nested
    @DisplayName("DELETE /api/users/me - 회원 탈퇴")
    class DeleteMe {

        @Test
        @DisplayName("성공 - 회원 탈퇴 성공")
        void success() throws Exception {
            // Given
            when(userService.deleteMe(1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터"))))
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.message").value("delete_success"));

            verify(userService).deleteMe(1L);
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("인증이 필요합니다"));

            verify(userService, never()).deleteMe(any());
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        void fail_userNotFound() throws Exception {
            // Given
            when(userService.deleteMe(999L)).thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/api/users/me")
                            .with(authentication(createAuth(999L, "test@example.com", "테스터"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("user_not_found"));

            verify(userService).deleteMe(999L);
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.deleteMe(1L))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(delete("/api/users/me")
                            .with(authentication(createAuth(1L, "test@example.com", "테스터"))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 닉네임 검색 (인증 불필요) ==========

    @Nested
    @DisplayName("GET /api/users/search/nickname - 닉네임 검색")
    class SearchByNickname {

        @Test
        @DisplayName("성공 - 닉네임 검색 성공")
        void success() throws Exception {
            // Given
            User user1 = User.builder()
                    .id(1L)
                    .email("test1@example.com")
                    .nickname("테스터1")
                    .profileImageUrl("image1.jpg")
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .nickname("테스터2")
                    .profileImageUrl("image2.jpg")
                    .build();

            when(userService.findByNicknameKeyword("테스트"))
                    .thenReturn(List.of(user1, user2));

            // When & Then
            mockMvc.perform(get("/api/users/search/nickname")
                            .param("keyword", "테스트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("search_success"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].nickname").value("테스터1"));

            verify(userService).findByNicknameKeyword("테스트");
        }

        @Test
        @DisplayName("성공 - 검색 결과 없음")
        void success_noResult() throws Exception {
            // Given
            when(userService.findByNicknameKeyword("없는닉네임"))
                    .thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/users/search/nickname")
                            .param("keyword", "없는닉네임"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("search_success"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(userService).findByNicknameKeyword("없는닉네임");
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.findByNicknameKeyword(any()))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(get("/api/users/search/nickname")
                            .param("keyword", "테스트"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 이메일 존재 확인 (인증 불필요) ==========

    @Nested
    @DisplayName("GET /api/users/exists/email - 이메일 존재 확인")
    class ExistsByEmail {

        @Test
        @DisplayName("성공 - 이메일 존재")
        void exists() throws Exception {
            // Given
            when(userService.existsByEmail("test@example.com"))
                    .thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/users/exists/email")
                            .param("email", "test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("check_success"))
                    .andExpect(jsonPath("$.data.exists").value(true));

            verify(userService).existsByEmail("test@example.com");
        }

        @Test
        @DisplayName("성공 - 이메일 존재하지 않음")
        void notExists() throws Exception {
            // Given
            when(userService.existsByEmail("notexist@example.com"))
                    .thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/users/exists/email")
                            .param("email", "notexist@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("check_success"))
                    .andExpect(jsonPath("$.data.exists").value(false));

            verify(userService).existsByEmail("notexist@example.com");
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.existsByEmail(any()))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(get("/api/users/exists/email")
                            .param("email", "test@example.com"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }

    // ========== 닉네임 개수 조회 (인증 불필요) ==========

    @Nested
    @DisplayName("GET /api/users/count/nickname - 닉네임 개수 조회")
    class CountByNickname {

        @Test
        @DisplayName("성공 - 닉네임 개수 조회")
        void count() throws Exception {
            // Given
            when(userService.countByNickname("테스터"))
                    .thenReturn(5L);

            // When & Then
            mockMvc.perform(get("/api/users/count/nickname")
                            .param("nickname", "테스터"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("count_success"))
                    .andExpect(jsonPath("$.data.count").value(5));

            verify(userService).countByNickname("테스터");
        }

        @Test
        @DisplayName("실패 - 서버 에러")
        void fail_serverError() throws Exception {
            // Given
            when(userService.countByNickname(any()))
                    .thenThrow(new RuntimeException("DB Error"));

            // When & Then
            mockMvc.perform(get("/api/users/count/nickname")
                            .param("nickname", "테스터"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }
}