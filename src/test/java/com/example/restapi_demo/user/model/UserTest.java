package com.example.restapi_demo.user.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserTest {

    @PersistenceContext
    EntityManager em;

    @Test
    @Rollback(false)
    void idTest() {
        // ✅ passwordHash 로 맞춤
        User user = User.builder()
                .email("tester@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("Adapterz")
                .build();

        em.persist(user);
        em.flush(); // INSERT 즉시 실행

        assertThat(user.getId()).isNotNull(); // @GeneratedValue 확인
    }

    @Test
    @Rollback(false)
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
        assertThat(user.getIsActive()).isTrue(); // ✅ isActive 기본 true
    }

    @Test
    @Rollback(false)
    void softDelete_setsIsActiveFalse() {
        User user = User.builder()
                .email("delete@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("ToDelete")
                .build();

        em.persist(user);
        em.flush();

        // ✅ soft delete 로직: isActive = false
        user.setIsActive(false);
        em.flush();

        assertThat(user.getIsActive()).isFalse();
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @Rollback(false)
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

        // ✅ 현재 스키마는 id 컬럼 사용
        assertThat(columns).contains("id");
    }
}
