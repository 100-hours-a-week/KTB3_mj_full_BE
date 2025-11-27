package com.example.restapi_demo.auth.service;

import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // email 기반으로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("사용자를 찾을 수 없습니다. email=" + email));

        // UserRole.USER / ADMIN → "USER"/"ADMIN"
        String roleName = user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())          // username = email
                .password(user.getPasswordHash())       // 비밀번호 해시
                .roles(roleName)                        // → "ROLE_USER" / "ROLE_ADMIN"
                .build();
    }
}
