package com.example.restapi_demo.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT에서 꺼낸 사용자 정보를 담아두는 Principal 객체.
 * - 매 요청마다 DB를 다시 조회하지 않고, 여기 있는 id/email/nickname을 사용한다.
 */
@Getter
@RequiredArgsConstructor
public class CustomUserPrincipal {

    private final Long id;   // User PK
    private final String email;
    private final String nickname;

    // 권한 목록 (ex: ROLE_USER, ROLE_ADMIN ...)
    private final Collection<? extends GrantedAuthority> authorities;
}
