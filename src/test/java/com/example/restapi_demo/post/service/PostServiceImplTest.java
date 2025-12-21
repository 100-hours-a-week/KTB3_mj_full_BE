package com.example.restapi_demo.post.service;

import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Comment;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.repository.JpaPostEntityRepository;
import com.example.restapi_demo.post.repository.PostRepository;
import com.example.restapi_demo.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static com.example.restapi_demo.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceImplTest {

    @Mock private PostRepository repo;
    @Mock private JpaPostEntityRepository jpaRepo;
    @InjectMocks private PostServiceImpl postService;

    // ========== 좋아요 기능 ==========

    @Nested
    @DisplayName("좋아요 추가")
    class AddLike {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            when(repo.incrementLikes(1L)).thenReturn(Optional.of(5));

            // when
            Integer result = postService.addLike(1L, 100L);

            // then
            assertThat(result).isEqualTo(5);
            verify(repo).incrementLikes(1L);
        }

        @Test
        @DisplayName("게시글 없음")
        void notFound() {
            // given
            when(repo.incrementLikes(999L)).thenReturn(Optional.empty());

            // when
            Integer result = postService.addLike(999L, 100L);

            // then
            assertThat(result).isNull();
            verify(repo).incrementLikes(999L);
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class RemoveLike {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            when(repo.decrementLikes(1L)).thenReturn(Optional.of(4));

            // when
            Integer result = postService.removeLike(1L, 100L);

            // then
            assertThat(result).isEqualTo(4);
            verify(repo).decrementLikes(1L);
        }

        @Test
        @DisplayName("게시글 없음")
        void notFound() {
            // given
            when(repo.decrementLikes(999L)).thenReturn(Optional.empty());

            // when
            Integer result = postService.removeLike(999L, 100L);

            // then
            assertThat(result).isNull();
            verify(repo).decrementLikes(999L);
        }
    }

    @Nested
    @DisplayName("조회수 증가")
    class IncreaseViews {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            when(jpaRepo.increaseViews(1L)).thenReturn(1);
            when(jpaRepo.findViews(1L)).thenReturn(Optional.of(150));

            // when
            Integer result = postService.increaseViews(1L);

            // then
            assertThat(result).isEqualTo(150);
            verify(jpaRepo).increaseViews(1L);
        }

        @Test
        @DisplayName("업데이트 실패")
        void noUpdate() {
            // given
            when(jpaRepo.increaseViews(999L)).thenReturn(0);

            // when
            Integer result = postService.increaseViews(999L);

            // then
            assertThat(result).isNull();
            verify(jpaRepo).increaseViews(999L);
        }
    }

    // ========== 게시글 CRUD ==========

    @Nested
    @DisplayName("게시글 생성")
    class CreatePost {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Post post = createPost(1L, createUser(100L));
            when(repo.createPost(100L, "작성자", "제목", "내용", "image.jpg"))
                    .thenReturn(Optional.of(post));

            // when
            Post result = postService.createPost(100L, "작성자", "제목", "내용", "image.jpg");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(repo).createPost(100L, "작성자", "제목", "내용", "image.jpg");
        }

        @Test
        @DisplayName("Repository empty")
        void failure() {
            // given
            when(repo.createPost(100L, "작성자", "제목", "내용", null))
                    .thenReturn(Optional.empty());

            // when
            Post result = postService.createPost(100L, "작성자", "제목", "내용", null);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Post post = createPost(1L, createUser(100L));
            PostRepository.DetailSeed detailSeed = createMockDetailSeed(1L, 100L, "수정", "내용");

            when(repo.findById(1L)).thenReturn(Optional.of(post));
            when(repo.updatePost(1L, "수정", "내용", null))
                    .thenReturn(Optional.of(detailSeed));

            // when
            PostUpdateResponse result = postService.updatePost(1L, 100L, "수정", "내용", null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("수정");
            verify(repo).findById(1L);
            verify(repo).updatePost(1L, "수정", "내용", null);
        }

        @Test
        @DisplayName("권한 없음")
        void unauthorized() {
            // given
            Post post = createPost(1L, createUser(100L));
            when(repo.findById(1L)).thenReturn(Optional.of(post));

            // when
            PostUpdateResponse result = postService.updatePost(1L, 999L, "제목", "내용", null);

            // then
            assertThat(result).isNull();
            verify(repo, never()).updatePost(any(), any(), any(), any());
        }

        @Test
        @DisplayName("삭제된 게시글")
        void deleted() {
            // given
            Post post = createPost(1L, createUser(100L), true);
            when(repo.findById(1L)).thenReturn(Optional.of(post));

            // when
            PostUpdateResponse result = postService.updatePost(1L, 100L, "제목", "내용", null);

            // then
            assertThat(result).isNull();
            verify(repo, never()).updatePost(any(), any(), any(), any());
        }

        @Test
        @DisplayName("게시글 없음")
        void notFound() {
            // given
            when(repo.findById(999L)).thenReturn(Optional.empty());

            // when
            PostUpdateResponse result = postService.updatePost(999L, 100L, "제목", "내용", null);

            // then
            assertThat(result).isNull();
            verify(repo, never()).updatePost(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Post post = createPost(1L, createUser(100L));
            when(repo.findById(1L)).thenReturn(Optional.of(post));
            when(repo.deleteById(1L)).thenReturn(true);

            // when
            boolean result = postService.deletePost(1L, 100L);

            // then
            assertThat(result).isTrue();
            verify(repo).findById(1L);
            verify(repo).deleteById(1L);
        }

        @Test
        @DisplayName("권한 없음")
        void unauthorized() {
            // given
            Post post = createPost(1L, createUser(100L));
            when(repo.findById(1L)).thenReturn(Optional.of(post));

            // when
            boolean result = postService.deletePost(1L, 999L);

            // then
            assertThat(result).isFalse();
            verify(repo, never()).deleteById(any());
        }

        @Test
        @DisplayName("이미 삭제됨")
        void alreadyDeleted() {
            // given
            Post post = createPost(1L, createUser(100L), true);
            when(repo.findById(1L)).thenReturn(Optional.of(post));

            // when
            boolean result = postService.deletePost(1L, 100L);

            // then
            assertThat(result).isFalse();
            verify(repo, never()).deleteById(any());
        }

        @Test
        @DisplayName("게시글 없음")
        void notFound() {
            // given
            when(repo.findById(999L)).thenReturn(Optional.empty());

            // when
            boolean result = postService.deletePost(999L, 100L);

            // then
            assertThat(result).isFalse();
            verify(repo, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("게시글 조회")
    class GetPosts {

        @Test
        @DisplayName("목록 조회")
        void list() {
            // given
            Post post = createPost(1L, createUser(1L));

            Pageable pageable = PageRequest.of(
                    0, 10, Sort.by(Sort.Direction.DESC, "createdAt")
            );

            Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);

            when(jpaRepo.findAll(any(Pageable.class))).thenReturn(page);

            // when
            PostListResponse result = postService.getPosts(0, 10);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(jpaRepo).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("상세 조회")
        void detail() {
            // given
            PostRepository.DetailSeed detailSeed = createMockDetailSeed(1L, 100L);
            when(repo.findDetailById(1L)).thenReturn(Optional.of(detailSeed));

            // when
            PostDetailResponse result = postService.getPostDetail(1L, 100L);

            // then
            assertThat(result).isNotNull();
            verify(repo).findDetailById(1L);
        }

        @Test
        @DisplayName("fallback 사용")
        void fallback() {
            // given
            Post post = createPost(1L, createUser(100L));
            when(repo.findDetailById(1L)).thenReturn(Optional.empty());
            when(repo.findById(1L)).thenReturn(Optional.of(post));

            // when
            PostDetailResponse result = postService.getPostDetail(1L, 100L);

            // then
            assertThat(result).isNotNull();
            verify(repo).findDetailById(1L);
            verify(repo).findById(1L);
        }
    }

    @Nested
    @DisplayName("게시글 검색")
    class SearchPosts {

        @Test
        @DisplayName("제목 검색 - 결과 있음")
        void byTitleSuccess() {
            // given
            Post post = createPost(1L, createUser(1L));
            when(repo.findByTitleContainingIgnoreCase("테스트")).thenReturn(List.of(post));

            // when
            List<PostSummary> result = postService.searchByTitle("테스트");

            // then
            assertThat(result).hasSize(1);
            verify(repo).findByTitleContainingIgnoreCase("테스트");
        }

        @Test
        @DisplayName("제목 검색 - 결과 없음")
        void byTitleEmpty() {
            // given
            when(repo.findByTitleContainingIgnoreCase("없음")).thenReturn(List.of());

            // when
            List<PostSummary> result = postService.searchByTitle("없음");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("작성자 검색 - 결과 있음")
        void byAuthorSuccess() {
            // given
            Post post = createPost(1L, createUser(1L, "작성자"));
            when(repo.findByAuthorNickname("작성자")).thenReturn(List.of(post));

            // when
            List<PostSummary> result = postService.findByAuthorNickname("작성자");

            // then
            assertThat(result).hasSize(1);
            verify(repo).findByAuthorNickname("작성자");
        }

        @Test
        @DisplayName("작성자 검색 - 결과 없음")
        void byAuthorEmpty() {
            // given
            when(repo.findByAuthorNickname("없음")).thenReturn(List.of());

            // when
            List<PostSummary> result = postService.findByAuthorNickname("없음");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========== 댓글 CRUD ==========

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Comment comment = createComment(1L, createUser(100L, "작성자"), "댓글");
            when(repo.addComment(1L, 100L, "작성자", "댓글"))
                    .thenReturn(Optional.of(comment));

            // when
            CommentResponse result = postService.createComment(1L, 100L, "작성자", "댓글");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("댓글");
            verify(repo).addComment(1L, 100L, "작성자", "댓글");
        }

        @Test
        @DisplayName("닉네임 null")
        void nullNickname() {
            // given
            Comment comment = createComment(1L, createUser(100L, "나"), "댓글");
            when(repo.addComment(1L, 100L, "나", "댓글"))
                    .thenReturn(Optional.of(comment));

            // when
            CommentResponse result = postService.createComment(1L, 100L, null, "댓글");

            // then
            assertThat(result).isNotNull();
            verify(repo).addComment(1L, 100L, "나", "댓글");
        }

        @Test
        @DisplayName("Repository empty")
        void failure() {
            // given
            when(repo.addComment(1L, 100L, "작성자", "댓글"))
                    .thenReturn(Optional.empty());

            // when
            CommentResponse result = postService.createComment(1L, 100L, "작성자", "댓글");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            Comment comment = createComment(10L, createUser(100L), "수정됨");
            when(repo.updateComment(1L, 10L, 100L, "수정됨"))
                    .thenReturn(Optional.of(comment));

            // when
            UpdateCommentResponse result = postService.updateComment(1L, 10L, 100L, "수정됨");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("수정됨");
            verify(repo).updateComment(1L, 10L, 100L, "수정됨");
        }

        @Test
        @DisplayName("Repository empty")
        void failure() {
            // given
            when(repo.updateComment(1L, 10L, 100L, "내용"))
                    .thenReturn(Optional.empty());

            // when
            UpdateCommentResponse result = postService.updateComment(1L, 10L, 100L, "내용");

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            when(repo.deleteComment(1L, 10L, 100L)).thenReturn(true);

            // when
            boolean result = postService.deleteComment(1L, 10L, 100L);

            // then
            assertThat(result).isTrue();
            verify(repo).deleteComment(1L, 10L, 100L);
        }

        @Test
        @DisplayName("권한 없음")
        void unauthorized() {
            // given
            when(repo.deleteComment(1L, 10L, 999L)).thenReturn(false);

            // when
            boolean result = postService.deleteComment(1L, 10L, 999L);

            // then
            assertThat(result).isFalse();
            verify(repo).deleteComment(1L, 10L, 999L);
        }
    }

    @Nested
    @DisplayName("댓글 조회")
    class GetComments {

        @Test
        @DisplayName("목록 조회")
        void list() {
            // given
            Comment comment1 = createComment(1L, createUser(100L), "댓글1");
            Comment comment2 = createComment(2L, createUser(200L), "댓글2");
            when(repo.findCommentsByPostId(1L)).thenReturn(List.of(comment1, comment2));

            // when
            List<CommentResponse> result = postService.getComments(1L, 100L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getContent()).isEqualTo("댓글1");
            verify(repo).findCommentsByPostId(1L);
        }

        @Test
        @DisplayName("빈 목록")
        void empty() {
            // given
            when(repo.findCommentsByPostId(1L)).thenReturn(List.of());

            // when
            List<CommentResponse> result = postService.getComments(1L, 100L);

            // then
            assertThat(result).isEmpty();
            verify(repo).findCommentsByPostId(1L);
        }
    }
}