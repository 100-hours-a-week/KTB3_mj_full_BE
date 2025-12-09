package com.example.restapi_demo.post.model;

import com.example.restapi_demo.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostTest {

    @PersistenceContext
    EntityManager em;

    /**
     * 단방향 ManyToOne(Post -> User) 매핑이 제대로 동작하는지 검증
     * - Post.author 에 User 가 잘 들어가는지
     * - 다시 조회했을 때 author 정보가 재구성되는지
     */
    @Test
    void unidirectionalManyToOneTest() {
        // 1) 사용자 저장
        User user = User.builder()
                .email("tester@adapterz.kr")
                .passwordHash("123aS!")
                .nickname("tester")
                .build();
        em.persist(user);

        // 2) 게시글 저장
        Post post = Post.builder()
                .author(user)
                .title("공지 글")
                .content("내용")
                .build();
        em.persist(post);
        em.flush();
        em.clear(); // 1차 캐시 비우기 → 실제 DB에서 다시 조회하기 위함

        // 3) 재조회
        Post findPost = em.find(Post.class, post.getId());

        // ✅ 단순 출력 대신 assert 로 검증
        assertThat(findPost).isNotNull();
        assertThat(findPost.getId()).isEqualTo(post.getId());
        assertThat(findPost.getTitle()).isEqualTo("공지 글");
        assertThat(findPost.getAuthor()).isNotNull();
        assertThat(findPost.getAuthor().getNickname()).isEqualTo("tester");
    }
}
