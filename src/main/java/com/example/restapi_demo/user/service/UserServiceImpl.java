package com.example.restapi_demo.user.service;

import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder; // ★ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입
    @Override
    public User register(String email, String password, String passwordConfirm,
                         String nickname, String profileImageUrl) {
        if (email == null || email.isBlank()
                || password == null || passwordConfirm == null
                || !password.equals(passwordConfirm)) {
            return null;
        }


        String encoded = passwordEncoder.encode(password);

        User toSave = User.builder()
                .email(email)
                .passwordHash(encoded)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();

        return userRepository.save(toSave);
    }

    @Override
    public User findById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).orElse(null);
    }


    @Override
    public User updateProfile(Long id, String nickname, String profileImageUrl) {
        if (id == null) return null;
        return userRepository.updateProfile(id, nickname, profileImageUrl).orElse(null);
    }

    @Override
    public boolean deleteMe(Long id) {
        if (id == null) return false;
        return userRepository.deleteById(id);
    }

    @Override
    public ChangePasswordResult changePassword(Long id, String newPassword, String newPasswordConfirm) {
        List<String[]> errors = new ArrayList<>();
        if (id == null) {
            errors.add(new String[]{"id", "invalid"});
            return new ChangePasswordResult(false, errors);
        }
        if (newPassword == null || newPasswordConfirm == null || !newPassword.equals(newPasswordConfirm)) {
            errors.add(new String[]{"password", "mismatch"});
            return new ChangePasswordResult(false, errors);
        }


        String encoded = passwordEncoder.encode(newPassword);
        Optional<User> updated = userRepository.updatePassword(id, encoded);
        if (updated.isEmpty()) {
            errors.add(new String[]{"user", "not_found"});
            return new ChangePasswordResult(false, errors);
        }
        return new ChangePasswordResult(true, null);
    }

    @Override
    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return null;


        return userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash())) // ★ 수정
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByNicknameKeyword(String keyword) {
        return userRepository.findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByNickname(String nickname) {
        return userRepository.countByNickname(nickname);
    }
}
