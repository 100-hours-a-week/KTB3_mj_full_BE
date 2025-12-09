package com.example.restapi_demo.post.controller;

import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.service.PostService;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PostController 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("게시글 목록 조회")
    class GetPosts {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            // Given
            PostSummary postSummary = new PostSummary(
                    1L,
                    "테스트 제목",
                    "작성자",
                    0,
                    0,
                    0,
                    LocalDateTime.now()
            );
            PostListResponse response = new PostListResponse(
                    List.of(postSummary),
                    0,
                    10,
                    1L,
                    1
            );

            // 🔻 여기 수정
            when(postService.getPosts(0, 10)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/posts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].postId").value(1))
                    .andExpect(jsonPath("$.data.content[0].title").value("테스트 제목"))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10));
        }

        @Test
        @DisplayName("서버 오류")
        void serverError() throws Exception {
            // Given
            // 🔻 여기도 수정
            when(postService.getPosts(0, 10))
                    .thenThrow(new RuntimeException("서버 오류"));

            // When & Then
            mockMvc.perform(get("/api/posts"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("internal_server_error"));
        }
    }


    @Nested
    @DisplayName("게시글 상세 조회")
    class GetPostDetail {

        @Test
        @DisplayName("성공 - 비로그인 사용자")
        void success_withoutAuth() throws Exception {
            // Given
            PostDetailResponse response = new PostDetailResponse(
                    1L,
                    "제목",
                    "작성자",
                    "내용",
                    List.of("https://example.com/image.jpg"),
                    10,
                    100,
                    5,
                    false,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            when(postService.getPostDetail(1L, null)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/posts/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data.post_id").value(1))
                    .andExpect(jsonPath("$.data.title").value("제목"))
                    .andExpect(jsonPath("$.data.content").value("내용"))
                    .andExpect(jsonPath("$.data.likes").value(10))
                    .andExpect(jsonPath("$.data.views").value(100))
                    .andExpect(jsonPath("$.data.comments_count").value(5));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글")
        void fail_notFound() throws Exception {
            // Given
            when(postService.getPostDetail(999L, null)).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/posts/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("post_not_found"));
        }
    }

    @Nested
    @DisplayName("게시글 생성")
    class CreatePost {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            mockUser.setEmail("test@example.com");
            mockUser.setNickname("테스터");
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            Post mockPost = new Post();
            mockPost.setId(1L);
            mockPost.setTitle("새 게시글");
            mockPost.setCreatedAt(LocalDateTime.now());
            when(postService.createPost(eq(1L), eq("테스터"), eq("새 게시글"), eq("내용"), isNull()))
                    .thenReturn(mockPost);

            String requestBody = """
                {
                    "title": "새 게시글",
                    "content": "내용"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("create_success"))
                    .andExpect(jsonPath("$.data.post_id").value(1))
                    .andExpect(jsonPath("$.data.title").value("새 게시글"));
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // Given
            String requestBody = """
                {
                    "title": "새 게시글",
                    "content": "내용"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 제목 누락")
        void fail_missingTitle() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            String requestBody = """
                {
                    "content": "내용"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 제목 길이 초과 (27자)")
        void fail_titleTooLong() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser); // 이 부분 추가!

            // 정확히 28자로 만들기
            String requestBody = """
        {
            "title": "12345678901234567890123456789",
            "content": "내용"
        }
        """;

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }


        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 내용 누락")
        void fail_missingContent() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            String requestBody = """
                {
                    "title": "제목"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }
    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            PostUpdateResponse response = new PostUpdateResponse(
                    1L,
                    "수정된 제목",
                    "수정된 내용",
                    null,
                    LocalDateTime.now()
            );
            when(postService.updatePost(eq(1L), eq(1L), eq("수정된 제목"), eq("수정된 내용"), isNull()))
                    .thenReturn(response);

            String requestBody = """
                {
                    "title": "수정된 제목",
                    "content": "수정된 내용"
                }
                """;

            // When & Then
            mockMvc.perform(patch("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("update_success"))
                    .andExpect(jsonPath("$.data.post_id").value(1))
                    .andExpect(jsonPath("$.data.title").value("수정된 제목"));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 권한 없음 또는 게시글 없음")
        void fail_notFoundOrForbidden() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.updatePost(eq(999L), eq(1L), any(), any(), any()))
                    .thenReturn(null);

            String requestBody = """
                {
                    "title": "수정",
                    "content": "내용"
                }
                """;

            // When & Then
            mockMvc.perform(patch("/api/posts/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("post_not_found_or_forbidden"));
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // Given
            String requestBody = """
                {
                    "title": "수정",
                    "content": "내용"
                }
                """;

            // When & Then
            mockMvc.perform(patch("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.deletePost(1L, 1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/api/posts/1"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void fail_unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/posts/1"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 게시글 없음 또는 권한 없음")
        void fail_notFound() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.deletePost(999L, 1L)).thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/api/posts/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("게시글 검색")
    class SearchPosts {

        @Test
        @DisplayName("제목으로 검색 성공")
        void searchByTitle_success() throws Exception {
            // Given
            List<PostSummary> results = List.of(
                    new PostSummary(1L, "테스트 제목", "작성자", 0, 0, 0, LocalDateTime.now())
            );
            when(postService.searchByTitle("테스트")).thenReturn(results);

            // When & Then
            mockMvc.perform(get("/api/posts/search/title")
                            .param("keyword", "테스트"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].title").value("테스트 제목"));
        }

        @Test
        @DisplayName("작성자로 검색 성공")
        void searchByAuthor_success() throws Exception {
            // Given
            List<PostSummary> results = List.of(
                    new PostSummary(1L, "제목", "홍길동", 0, 0, 0, LocalDateTime.now())
            );
            when(postService.findByAuthorNickname("홍길동")).thenReturn(results);

            // When & Then
            mockMvc.perform(get("/api/posts/search/author")
                            .param("nickname", "홍길동"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].author").value("홍길동"));
        }
    }

    @Nested
    @DisplayName("좋아요 기능")
    class LikePost {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("좋아요 추가 성공")
        void addLike_success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.addLike(1L, 1L)).thenReturn(11);

            // When & Then
            mockMvc.perform(post("/api/posts/1/likes"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("like_added"))
                    .andExpect(jsonPath("$.data.likes").value(11));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("좋아요 취소 성공")
        void removeLike_success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.removeLike(1L, 1L)).thenReturn(9);

            // When & Then
            mockMvc.perform(delete("/api/posts/1/likes"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("like_removed"))
                    .andExpect(jsonPath("$.data.likes").value(9));
        }

        @Test
        @DisplayName("좋아요 추가 실패 - 인증 없음")
        void addLike_fail_unauthorized() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/posts/1/likes"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("좋아요 추가 실패 - 게시글 없음")
        void addLike_fail_notFound() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.addLike(999L, 1L)).thenReturn(null);

            // When & Then
            mockMvc.perform(post("/api/posts/999/likes"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("post_not_found"));
        }
    }

    @Nested
    @DisplayName("댓글 조회")
    class GetComments {

        @Test
        @DisplayName("댓글 목록 조회 성공")
        void getComments_success() throws Exception {
            // Given
            List<CommentResponse> comments = List.of(
                    new CommentResponse(1L, "작성자1", "댓글1", LocalDateTime.now()),
                    new CommentResponse(2L, "작성자2", "댓글2", LocalDateTime.now())
            );
            when(postService.getComments(1L, null)).thenReturn(comments);

            // When & Then
            mockMvc.perform(get("/api/posts/1/comments"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("read_success"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateComment {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void createComment_success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            mockUser.setNickname("테스터");
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            CommentResponse response = new CommentResponse(
                    1L,
                    "테스터",
                    "댓글 내용",
                    LocalDateTime.now()
            );
            when(postService.createComment(1L, 1L, "테스터", "댓글 내용"))
                    .thenReturn(response);

            String requestBody = """
            {
              "content": "댓글 내용"
            }
            """;

            // When & Then
            mockMvc.perform(post("/api/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("create_success"))
                    .andExpect(jsonPath("$.data.content").value("댓글 내용"));
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void createComment_unauthorized() throws Exception {
            String requestBody = """
            {
              "content": "댓글 내용"
            }
            """;

            mockMvc.perform(post("/api/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 내용 없음")
        void createComment_invalid() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            String requestBody = """
            {
              "content": ""
            }
            """;

            mockMvc.perform(post("/api/posts/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 게시글 없음")
        void createComment_postNotFound() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            mockUser.setNickname("테스터");
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            when(postService.createComment(999L, 1L, "테스터", "댓글 내용"))
                    .thenReturn(null);

            String requestBody = """
            {
              "content": "댓글 내용"
            }
            """;

            mockMvc.perform(post("/api/posts/999/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("post_not_found"));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void updateComment_success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            UpdateCommentResponse response = new UpdateCommentResponse(1L, "수정된 댓글");
            when(postService.updateComment(1L, 1L, 1L, "수정된 댓글"))
                    .thenReturn(response);

            String requestBody = """
            {
              "content": "수정된 댓글"
            }
            """;

            mockMvc.perform(patch("/api/posts/1/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("update_success"))
                    .andExpect(jsonPath("$.data.content").value("수정된 댓글"));
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void updateComment_unauthorized() throws Exception {
            String requestBody = """
            {
              "content": "수정된 댓글"
            }
            """;

            mockMvc.perform(patch("/api/posts/1/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 내용 없음")
        void updateComment_invalid() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            String requestBody = """
            {
              "content": ""
            }
            """;

            mockMvc.perform(patch("/api/posts/1/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 댓글 없음 또는 권한 없음")
        void updateComment_notFoundOrForbidden() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            when(postService.updateComment(1L, 999L, 1L, "수정된 댓글"))
                    .thenReturn(null);

            String requestBody = """
            {
              "content": "수정된 댓글"
            }
            """;

            mockMvc.perform(patch("/api/posts/1/comments/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("not_found_or_forbidden"));
        }
    }


    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공")
        void deleteComment_success() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.deleteComment(1L, 1L, 1L)).thenReturn(true);

            mockMvc.perform(delete("/api/posts/1/comments/1"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패 - 인증 없음")
        void deleteComment_unauthorized() throws Exception {
            mockMvc.perform(delete("/api/posts/1/comments/1"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 댓글 없음 또는 권한 없음")
        void deleteComment_notFound() throws Exception {
            // Given
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);
            when(postService.deleteComment(1L, 999L, 1L)).thenReturn(false);

            mockMvc.perform(delete("/api/posts/1/comments/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("조회수 증가")
    class IncreaseViews {

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("성공 - 조회수 증가")
        void increaseViews_success() throws Exception {
            // Given: 서비스가 150을 돌려준다고 가정
            when(postService.increaseViews(1L)).thenReturn(150);

            // When & Then
            mockMvc.perform(post("/api/posts/1/views"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.views").value(150));
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 게시글 없음")
        void increaseViews_notFound() throws Exception {
            // Given: 서비스가 null을 리턴 (없는 게시글)
            when(postService.increaseViews(999L)).thenReturn(null);

            // When & Then
            mockMvc.perform(post("/api/posts/999/views"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "test@example.com")
        @DisplayName("실패 - 제목 길이 초과")
        void updatePost_titleTooLong() throws Exception {
            User mockUser = new User();
            mockUser.setId(1L);
            when(userService.findByEmail("test@example.com")).thenReturn(mockUser);

            String requestBody = """
    {
      "title": "12345678901234567890123456789",
      "content": "내용"
    }
    """;

            mockMvc.perform(patch("/api/posts/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("invalid_request"));
        }


    }


}