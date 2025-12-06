package com.example.restapi_demo.support;

import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.model.Comment;
import com.example.restapi_demo.post.repository.PostRepository;
import com.example.restapi_demo.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class TestFixtures {

    // ========== User 생성 ==========

    public static User createUser(Long id, String nickname) {
        return User.builder()
                .id(id)
                .email(nickname + "@test.com")
                .nickname(nickname)
                .build();
    }

    public static User createUser(Long id) {
        return createUser(id, "testUser" + id);
    }

    // ========== Post 생성 ==========

    public static Post createPost(Long id, User author, boolean isDeleted) {
        return Post.builder()
                .id(id)
                .title("테스트 제목 " + id)
                .content("테스트 내용 " + id)
                .author(author)
                .isDeleted(isDeleted)
                .build();
    }

    public static Post createPost(Long id, User author) {
        return createPost(id, author, false);
    }

    // ========== Comment 생성 ==========

    public static Comment createComment(Long id, Post post, User author, String content) {
        return Comment.builder()
                .id(id)
                .post(post)
                .author(author)
                .content(content)
                .isDeleted(false)
                .build();
    }

    public static Comment createComment(Long id, User author, String content) {
        return Comment.builder()
                .id(id)
                .author(author)
                .content(content)
                .isDeleted(false)
                .build();
    }

    public static Comment createComment(Long id, User author) {
        return createComment(id, author, "테스트 댓글 " + id);
    }

    // ========== Mock 객체 생성 ==========

    public static PostRepository.DetailSeed createMockDetailSeed(
            Long postId,
            Long authorId,
            String title,
            String content
    ) {
        PostRepository.DetailSeed detailSeed = mock(PostRepository.DetailSeed.class);

        lenient().when(detailSeed.getPostId()).thenReturn(postId);
        lenient().when(detailSeed.getTitle()).thenReturn(title);
        lenient().when(detailSeed.getAuthorName()).thenReturn("작성자");
        lenient().when(detailSeed.getContent()).thenReturn(content);
        lenient().when(detailSeed.getImages()).thenReturn(List.of());
        lenient().when(detailSeed.getLikesCount()).thenReturn(0);
        lenient().when(detailSeed.getViews()).thenReturn(0);
        lenient().when(detailSeed.getCommentsCount()).thenReturn(0);
        lenient().when(detailSeed.getAuthorId()).thenReturn(authorId);
        lenient().when(detailSeed.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(detailSeed.getUpdatedAt()).thenReturn(LocalDateTime.now());

        return detailSeed;
    }

    public static PostRepository.DetailSeed createMockDetailSeed(Long postId, Long authorId) {
        return createMockDetailSeed(postId, authorId, "테스트 제목", "테스트 내용");
    }
}