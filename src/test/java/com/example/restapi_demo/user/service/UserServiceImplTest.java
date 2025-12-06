package com.example.restapi_demo.user.service;

import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("회원가입")
    class Register {

        @Test
        @DisplayName("성공")
        void success() {
            // Given - 테스트에 필요한 데이터 준비
            String email = "test@example.com";
            String password = "password123";
            String passwordConfirm = "password123";
            String nickname = "테스터";
            String profileImageUrl = "https://example.com/image.jpg";

            // passwordEncoder가 encode를 호출하면 "encodedPassword" 반환
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

            // 저장될 User 객체
            User savedUser = User.builder()
                    .id(1L)
                    .email(email)
                    .passwordHash("encodedPassword")
                    .nickname(nickname)
                    .profileImageUrl(profileImageUrl)
                    .build();

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When - 실제로 테스트할 메서드 실행
            User result = userService.register(email, password, passwordConfirm, nickname, profileImageUrl);
            //user를 다시 검증하는게 아니라 result를 검증

            // Then - 결과 검증
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getNickname()).isEqualTo(nickname);
            assertThat(result.getProfileImageUrl()).isEqualTo(profileImageUrl);

            // Mock 메서드가 제대로 호출되었는지 확인
            verify(passwordEncoder, times(1)).encode(password);
            verify(userRepository, times(1)).save(any(User.class));
        }


        @Test
        @DisplayName("실패 - 이메일 null")
        void fail_nullEmail() {
            // Given
            String email = null;
            String password = "password123";
            String passwordConfirm = "password123";
            String nickname = "test";


            // When
            User result = userService.register(email, password, passwordConfirm, nickname, null);


            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).save(any());

        }

        @Test
        @DisplayName("실패 - 이메일 공백")
        void fail_blankEmail() {
            String email = " ";
            String password = "password123";
            String passwordConfirm = "password123";
            String nickname = "test";

            //when
            User result = userService.register(
                    email,
                    password,
                    passwordConfirm,
                    nickname,
                    "");

            //then
            assertThat(result).isNull();
            verify(userRepository, never()).save(any());




        }

        @Test
        @DisplayName("실패 - 비밀번호 null")
        void fail_nullPassword() {
            //given
            String email = "test@example.com";
            String password = null;
            String passwordConfirm = "password123";
            String nickname = "test";

            //hwen
            User result = userService.register(
                    email,
                    password,
                    passwordConfirm,
                    nickname,
                    ""
            );

            //then
            assertThat(result).isNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void fail_passwordMismatch() {
            // 여기를 작성해보세요!
            //given
            String email = "test@example.com";
            String password = "qwertyuiop";
            String passwordConfirm = "password123";
            String nickname = "test";

            //when
            User result = userService.register(
                    email,
                    password,
                    passwordConfirm,
                    nickname,
                    ""
            );

            //then
            assertThat(result).isNull();
            verify(userRepository, never()).save(any());

        }

    }

    @Nested
    @DisplayName("findById 메서드")
    class FindById {

        @Test
        @DisplayName("성공 - ID로 사용자 조회")
        void success() {
            // given
            Long id = 1L;
            User mockUser = User.builder()
                    .id(id)
                    .email("test@example.com")
                    .nickname("test")
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            //when
            User result = userService.findById(id);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            verify(userRepository, times(1)).findById(id);

        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        void notFound() {
            // given
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            //when
            User result = userService.findById(userId);

            //then
            assertThat(result).isNull();
            verify(userRepository).findById(userId);

        }
    }

    @Nested
    @DisplayName("findByEmail 메서드")
    class FindByEmail {

        @Test
        @DisplayName("성공 - 이메일로 사용자 조회")
        void success() {
            // Given
            String email = "test@example.com";
            User mockUser = User.builder()
                    .id(1L)
                    .email(email)
                    .nickname("테스터")
                    .build();

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(mockUser));  // ← 여기를 채우세요!

            // When
            User result = userService.findByEmail(email);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
            verify(userRepository).findByEmail(email);  // ← 여기를 채우세요!
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void notFound() {
            // given
            String email = "notexist@example.com";  // ← 존재하지 않는 이메일
            // mockUser 생성 불필요! ✅

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            //when
            User result = userService.findByEmail(email);

            //then
            assertThat(result).isNull();
            verify(userRepository).findByEmail(email);

        }


    }


    @Nested
    @DisplayName("existsByEmail 메서드")
    class existsByEmail {

        @Test
        @DisplayName("존재하는 이메일")
        void existsByEmail() {
            //given
            String email = "test@example.com";
            when(userRepository.existsByEmail(email)).thenReturn(true);

            //when
            boolean result = userService.existsByEmail(email);

            //then
            assertThat(result).isTrue();
            verify(userRepository).existsByEmail(email);
        }

        @Test
        @DisplayName("존재하지 않는 이메일")
        void notExistsByEmail() {
            //given
            String email = "notexist@example.com";
            when(userRepository.existsByEmail(email)).thenReturn(false);

            //hen
            boolean result = userService.existsByEmail(email);

            //then
            assertThat(result).isFalse();
            verify(userRepository).existsByEmail(email);
        }
    }

    @Nested
    @DisplayName("countByNickname 메소드")
    class countByNickname {

        @Test
        @DisplayName("넥네임으로 개수 조화")
        void countByNickname() {
            String nickname = "test";
            when(userRepository.countByNickname(nickname)).thenReturn(3L);
            long result = userService.countByNickname(nickname);
            assertThat(result).isEqualTo(3L);
            verify(userRepository).countByNickname(nickname);
        }

    }

    @Nested
    @DisplayName("findByNicknameKeyword 메서드")
    class FindByNicknameKeyword {

        @Test
        @DisplayName("닉네임 키워드로 검색")
        void search() {
            // Given
            String keyword = "테스트";

            User user1 = User.builder()
                    .id(1L)
                    .email("test1@example.com")
                    .nickname("테스트유저1")
                    .build();

            User user2 = User.builder()
                    .id(2L)
                    .email("test2@example.com")
                    .nickname("테스트유저2")
                    .build();

            when(userRepository.findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword))  // ✅ 실제 메서드명!
                    .thenReturn(List.of(user1, user2));

            // When
            List<User> result = userService.findByNicknameKeyword(keyword);

            // Then
            assertThat(result).hasSize(2); // ← 여기를 채우세요!
            assertThat(result.get(0).getNickname()).contains("테스트");  // ← 여기를 채우세요!
            verify(userRepository).findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword);  // ✅ 수정!
        }

        @Test
        @DisplayName("검색 결과 없음")
        void noResult() {
            // given
            String keyword = "test";
            when(userRepository.findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword))
                    .thenReturn(List.of());

            //when
            List<User> result = userService.findByNicknameKeyword(keyword);

            //then
            assertThat(result).isEmpty();
            verify(userRepository).findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword);


        }
    }

    @Nested
    @DisplayName("프로필 업데이트 메소드")
    class UpdateProfile {

        @Test
        @DisplayName("성공 - 닉네임 수정")
        void updateNickname() {
            //given
            Long userId = 1L;
            String newNickname = "닉변한 닉네임";

            User mockUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname("기존닉네임")
                    .profileImageUrl("image.jpg")
                    .build();

            User updatedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname(newNickname)
                    .profileImageUrl("image.jpg")
                    .build();

            when(userRepository.updateProfile(userId, newNickname, null))
                    .thenReturn(Optional.of(updatedUser));

            //when
            User result = userService.updateProfile(userId,newNickname,null);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getNickname()).isEqualTo(newNickname);
            verify(userRepository).updateProfile(userId, newNickname, null);

        }

        @Test
        @DisplayName("성공 - 프로필 이미지만 수정")
        void updateProfileImage() {
            //given
            Long userId = 1L;
            String newImageUrl = "image.jpg";

            User updatedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname("테스터")
                    .profileImageUrl(newImageUrl)
                    .build();

            when(userRepository.updateProfile(userId,  null, newImageUrl))
            .thenReturn(Optional.of(updatedUser));

            //when
            User result = userService.updateProfile(userId, null, newImageUrl);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getProfileImageUrl()).isEqualTo(newImageUrl);
            verify(userRepository).updateProfile(userId,  null, newImageUrl);

        }

        @Test
        @DisplayName("성공 - 닉네임과 프로필 이미지 모두 수정")
        void updateBoth() {
            // Given
            Long userId = 1L;
            String newNickname = "새닉네임";
            String newImageUrl = "new-image.jpg";

            User updatedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .nickname(newNickname)
                    .profileImageUrl(newImageUrl)
                    .build();

            when(userRepository.updateProfile(userId, newNickname, newImageUrl))
                    .thenReturn(Optional.of(updatedUser));

            // When
            User result = userService.updateProfile(userId, newNickname, newImageUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getNickname()).isEqualTo(newNickname);
            assertThat(result.getProfileImageUrl()).isEqualTo(newImageUrl);
            verify(userRepository).updateProfile(userId, newNickname, newImageUrl);
        }// 두번호출말고 한번에 바꾸기

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void notFound() {
            // given
            Long userId = 999L;
            String newNickname = "새닉네임";
            String newImageUrl = "new-image.jpg";

            when(userRepository.updateProfile(userId, newNickname, newImageUrl))
                    .thenReturn(Optional.empty());

            //when
            User result = userService.updateProfile(userId, newNickname, newImageUrl);

            //then
            assertThat(result).isNull();
            verify(userRepository).updateProfile(userId, newNickname, newImageUrl);


        }

    }

    @Nested
    @DisplayName("authenticate 메서드")
    class Authenticate {

        @Test
        @DisplayName("성공 - 로그인 성공")
        void success() {
            // Given
            String email = "test@example.com";
            String password = "password123";
            String encodedPassword = "encodedPassword123";

            User mockUser = User.builder()
                    .id(1L)
                    .email(email)
                    .passwordHash(encodedPassword)
                    .nickname("테스터")
                    .build();

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(password, encodedPassword))
                    .thenReturn(true);

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(email);
            verify(userRepository).findByEmail(email);
            verify(passwordEncoder).matches(password, encodedPassword);
        }

        @Test
        @DisplayName("실패 - 이메일 null")
        void fail_nullEmail() {
            // Given
            String email = null;
            String password = "password123";

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).findByEmail(any());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("실패 - 이메일 공백")
        void fail_blankEmail() {
            // Given
            String email = "  ";
            String password = "password123";

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).findByEmail(any());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 null")
        void fail_nullPassword() {
            // Given
            String email = "test@example.com";
            String password = null;

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).findByEmail(any());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 공백")
        void fail_blankPassword() {
            // Given
            String email = "test@example.com";
            String password = "  ";

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).findByEmail(any());
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void fail_emailNotFound() {
            // Given
            String email = "notexist@example.com";
            String password = "password123";

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.empty());

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository).findByEmail(email);
            verify(passwordEncoder, never()).matches(any(), any());  // 비밀번호 검증 안 함!
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void fail_wrongPassword() {
            // Given
            String email = "test@example.com";
            String password = "wrongpassword";
            String encodedPassword = "encodedPassword123";

            User mockUser = User.builder()
                    .id(1L)
                    .email(email)
                    .passwordHash(encodedPassword)
                    .nickname("테스터")
                    .build();

            when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(password, encodedPassword))
                    .thenReturn(false);  // ← 불일치!

            // When
            User result = userService.authenticate(email, password);

            // Then
            assertThat(result).isNull();
            verify(userRepository).findByEmail(email);
            verify(passwordEncoder).matches(password, encodedPassword);
        }
    }

    @Nested
    @DisplayName("changePassword 메서드")
    class ChangePassword {

        @Test
        @DisplayName("성공 - 비밀번호 변경 성공")
        void success() {
            // Given
            Long userId = 1L;
            String newPassword = "newPassword123";
            String newPasswordConfirm = "newPassword123";
            String encodedPassword = "encodedNewPassword";

            User updatedUser = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .passwordHash(encodedPassword)
                    .build();

            when(passwordEncoder.encode(newPassword))
                    .thenReturn(encodedPassword);

            when(userRepository.updatePassword(userId, encodedPassword))
                    .thenReturn(Optional.of(updatedUser));

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isTrue();   // ← getter 아님!
            assertThat(result.errors).isNull();    // ← getter 아님!
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).updatePassword(userId, encodedPassword);
        }

        @Test
        @DisplayName("실패 - ID null")
        void fail_nullId() {
            // Given
            Long userId = null;
            String newPassword = "newPassword123";
            String newPasswordConfirm = "newPassword123";

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isFalse();
            assertThat(result.errors).isNotEmpty();
            assertThat(result.errors.get(0)[0]).isEqualTo("id");
            assertThat(result.errors.get(0)[1]).isEqualTo("invalid");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).updatePassword(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 null")
        void fail_nullPassword() {
            // Given
            Long userId = 1L;
            String newPassword = null;
            String newPasswordConfirm = "newPassword123";

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isFalse();
            assertThat(result.errors).isNotEmpty();
            assertThat(result.errors.get(0)[0]).isEqualTo("password");
            assertThat(result.errors.get(0)[1]).isEqualTo("mismatch");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).updatePassword(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 확인 null")
        void fail_nullPasswordConfirm() {
            // Given
            Long userId = 1L;
            String newPassword = "newPassword123";
            String newPasswordConfirm = null;

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isFalse();
            assertThat(result.errors).isNotEmpty();
            assertThat(result.errors.get(0)[0]).isEqualTo("password");
            assertThat(result.errors.get(0)[1]).isEqualTo("mismatch");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).updatePassword(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void fail_passwordMismatch() {
            // Given
            Long userId = 1L;
            String newPassword = "newPassword123";
            String newPasswordConfirm = "differentPassword";

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isFalse();
            assertThat(result.errors).isNotEmpty();
            assertThat(result.errors.get(0)[0]).isEqualTo("password");
            assertThat(result.errors.get(0)[1]).isEqualTo("mismatch");

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).updatePassword(any(), any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() {
            // Given
            Long userId = 999L;
            String newPassword = "newPassword123";
            String newPasswordConfirm = "newPassword123";
            String encodedPassword = "encodedNewPassword";

            when(passwordEncoder.encode(newPassword))
                    .thenReturn(encodedPassword);

            when(userRepository.updatePassword(userId, encodedPassword))
                    .thenReturn(Optional.empty());  // ← 사용자 없음!

            // When
            var result = userService.changePassword(userId, newPassword, newPasswordConfirm);

            // Then
            assertThat(result.success).isFalse();
            assertThat(result.errors).isNotEmpty();
            assertThat(result.errors.get(0)[0]).isEqualTo("user");
            assertThat(result.errors.get(0)[1]).isEqualTo("not_found");

            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).updatePassword(userId, encodedPassword);
        }
    }

    @Nested
    @DisplayName("deleteMe 메서드")
    class DeleteMe {

        @Test
        @DisplayName("성공 - 회원 탈퇴 성공")
        void success() {
            // Given
            Long userId = 1L;

            when(userRepository.deleteById(userId))
                    .thenReturn(true);

            // When
            boolean result = userService.deleteMe(userId);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("실패 - ID null")
        void fail_nullId() {
            // Given
            Long userId = null;

            // When
            boolean result = userService.deleteMe(userId);

            // Then
            assertThat(result).isFalse();
            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() {
            // Given
            Long userId = 999L;

            when(userRepository.deleteById(userId))
                    .thenReturn(false);  // ← 사용자 없음

            // When
            boolean result = userService.deleteMe(userId);

            // Then
            assertThat(result).isFalse();
            verify(userRepository).deleteById(userId);
        }
    }

}