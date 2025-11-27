package com.example.restapi_demo.post.model;

import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PostTest {

    @PersistenceContext
    EntityManager em;

    @Test
    @Rollback(false)
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
        em.clear();

        // 3) 재조회
        Post findPost = em.find(Post.class, post.getId());
        System.out.println("postId: " + findPost.getId());
        System.out.println("title : " + findPost.getTitle());
        System.out.println("author: " + findPost.getAuthor().getNickname());
    }
}
