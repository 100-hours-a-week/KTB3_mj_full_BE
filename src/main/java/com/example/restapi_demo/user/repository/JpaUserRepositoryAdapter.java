package com.example.restapi_demo.user.repository;

import com.example.restapi_demo.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserEntityRepository jpa;

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email)
                .filter(User::getIsActive);
    }

    @Override
    public User save(User u) {

        return jpa.save(u);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id)
                .filter(User::getIsActive);
    }

    @Override
    public Optional<User> updateProfile(Long id, String nickname, String profileImageUrl) {
        return jpa.findById(id)
                .filter(User::getIsActive)
                .map(u -> {
                    if (nickname != null) u.setNickname(nickname);
                    if (profileImageUrl != null) u.setProfileImageUrl(profileImageUrl);
                    return jpa.save(u);
                });
    }

    @Override
    public boolean deleteById(Long id) {
        return jpa.findById(id).map(u -> {
            u.setIsActive(false);
            jpa.save(u);
            return true;
        }).orElse(false);
    }

    @Override
    public Optional<User> updatePassword(Long id, String newPasswordHash) {
        return jpa.findById(id)
                .filter(User::getIsActive)
                .map(u -> {
                    u.setPasswordHash(newPasswordHash);
                    return jpa.save(u);
                });
    }

    @Override
    public List<User> findByNicknameContainingIgnoreCaseOrderByIdDesc(String keyword) {
        return jpa.findByNicknameContainingIgnoreCaseOrderByIdDesc(keyword)
                .stream()
                .filter(User::getIsActive)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public long countByNickname(String nickname) {
        return jpa.countByNickname(nickname);
    }
}
