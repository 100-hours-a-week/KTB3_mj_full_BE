package com.example.restapi_demo.user.repository;

import com.example.restapi_demo.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUserEntityRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByNicknameContainingIgnoreCaseOrderByIdDesc(String keyword);
    boolean existsByEmail(String email);
    long countByNickname(String nickname);
}
