package com.example.restapi_demo.user.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    /**
     * @GeneratedValue 전략이 정상적으로 동작하는지 확인
     * - persist + flush 이후 id가 null이 아니어야 함
     */
    @Test
    void idTest() {
        User user = User.builder()
                .email("tester@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("Adapterz")
                .build();

        em.persist(user);
        em.flush(); // INSERT 즉시 실행

        assertThat(user.getId()).isNotNull(); // @GeneratedValue 확인
    }

    /**
     * @PrePersist 콜백이 createdAt / updatedAt / isActive 를 세팅하는지 확인
     */
    @Test
    void createdUpdatedAtTest_prePersistWorks() {
        User user = User.builder()
                .email("time@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("TimeUser")
                .build();

        em.persist(user);
        em.flush();

        // @PrePersist 로 자동 세팅되는지 확인
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getIsActive()).isTrue(); // 기본값 true
    }

    /**
     * soft delete 로직: isActive 값을 false 로 변경하면 soft delete 로 동작하는지 확인
     */
    @Test
    void softDelete_setsIsActiveFalse() {
        User user = User.builder()
                .email("delete@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("ToDelete")
                .build();

        em.persist(user);
        em.flush();

        user.setIsActive(false);
        em.flush();

        assertThat(user.getIsActive()).isFalse();
    }

    /**
     * users 테이블 컬럼에 id 컬럼이 존재하는지 확인
     * - 스키마 매핑 검증 용도
     */
    @Test
    void user_id_column_is_mapped() {
        User user = User.builder()
                .email("col@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("ColUser")
                .build();
        em.persist(user);
        em.flush();

        var columns = jdbc.query(
                "SHOW COLUMNS FROM users",
                (rs, i) -> rs.getString("Field")
        );

        assertThat(columns).contains("id");
    }
}
