package com.example.restapi_demo.post.controller;

import com.example.restapi_demo.common.api.ApiResponse;
import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.service.PostService;
import com.example.restapi_demo.user.model.User;
import com.example.restapi_demo.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    private ResponseEntity<ApiResponse<Object>> badRequest(String code) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(code, null));
    }

    private ResponseEntity<ApiResponse<Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("auth_required", null));
    }

    private ResponseEntity<ApiResponse<Object>> internalError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("internal_server_error", null));
    }

    private String currentEmailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (principal == null) return null;
        if ("anonymousUser".equals(principal)) return null;

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    private User currentUserOrNull() {
        String email = currentEmailOrNull();
        if (email == null || email.isBlank()) return null;
        return userService.findByEmail(email);
    }

    private Long currentUserIdOrNull() {
        User u = currentUserOrNull();
        return (u == null ? null : u.getId());
    }


    @Operation(summary = "게시글 목록 조회", description = "전체 게시글 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PostListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list() {
        try {
            PostListResponse data = postService.getPosts();
            return ResponseEntity.ok(new ApiResponse<>("read_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 ID를 기반으로 상세 내용을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<Object>> detail(@PathVariable Long postId) {
        try {
            Long requesterId = currentUserIdOrNull(); // 비로그인이면 null
            PostDetailResponse data = postService.getPostDetail(postId, requesterId);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("post_not_found", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("read_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글 목록을 조회합니다.")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Object>> getComments(@PathVariable Long postId) {
        try {
            Long requesterId = currentUserIdOrNull(); // 비로그인이면 null
            List<CommentResponse> comments = postService.getComments(postId, requesterId);
            return ResponseEntity.ok(new ApiResponse<>("read_success", comments));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "제목 키워드 검색", description = "제목에 keyword가 포함된 게시글을 조회합니다. 대소문자 무시.")
    @GetMapping("/search/title")
    public ResponseEntity<ApiResponse<Object>> searchByTitle(@RequestParam String keyword) {
        try {
            var results = postService.searchByTitle(keyword);
            return ResponseEntity.ok(new ApiResponse<>("read_success", results));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }

    @Operation(summary = "작성자 닉네임으로 검색", description = "특정 닉네임 작성자의 게시글을 조회합니다.")
    @GetMapping("/search/author")
    public ResponseEntity<ApiResponse<Object>> searchByAuthor(@RequestParam String nickname) {
        try {
            var results = postService.findByAuthorNickname(nickname);
            return ResponseEntity.ok(new ApiResponse<>("read_success", results));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("internal_server_error", null));
        }
    }


    @Operation(summary = "게시글 생성", description = "새로운 게시글을 작성합니다. 제목은 최대 26자.")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createPost(@RequestBody Map<String, String> req) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            if (req == null) return badRequest("invalid_request");
            String title = req.get("title");
            String content = req.get("content");
            String image = req.get("image");

            if (title == null || title.isBlank() || title.length() > 26) return badRequest("invalid_request");
            if (content == null || content.isBlank()) return badRequest("invalid_request");

            String authorName = (me.getNickname() != null && !me.getNickname().isBlank()) ? me.getNickname() : "나";

            Post newPost = postService.createPost(me.getId(), authorName, title, content, image);
            if (newPost == null) return internalError();

            PostCreateResponse data = new PostCreateResponse(
                    newPost.getId(),
                    newPost.getTitle(),
                    authorName,
                    image,
                    newPost.getCreatedAt()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>("create_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "게시글 수정", description = "제목/본문/이미지를 수정합니다.")
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Object>> updatePost(
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request
    ) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            if (request == null) return badRequest("invalid_request");
            String title = request.getTitle();
            String content = request.getContent();
            String image = request.getImage();

            if (title == null || title.isBlank() || title.length() > 26) return badRequest("invalid_request");
            if (content == null || content.isBlank()) return badRequest("invalid_request");

            PostUpdateResponse data = postService.updatePost(postId, me.getId(), title, content, image);
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("post_not_found_or_forbidden", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("update_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다. 성공 시 204 반환.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> delete(@PathVariable Long postId) {
        try {
            User me = currentUserOrNull();
            if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            boolean deleted = postService.deletePost(postId, me.getId());
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "좋아요 추가", description = "게시글에 좋아요를 추가합니다.")
    @PostMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Object>> like(@PathVariable Long postId) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            Integer newLikes = postService.addLike(postId, me.getId());
            if (newLikes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("post_not_found", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("like_added", Map.of("likes", newLikes)));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Object>> unlike(@PathVariable Long postId) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            Integer newLikes = postService.removeLike(postId, me.getId());
            if (newLikes == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("post_not_found", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("like_removed", Map.of("likes", newLikes)));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "댓글 작성", description = "게시글에 새로운 댓글을 작성합니다.")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Object>> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request
    ) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            if (request == null || request.getContent() == null || request.getContent().isBlank()) {
                return badRequest("invalid_request");
            }

            String nickname = (me.getNickname() != null && !me.getNickname().isBlank()) ? me.getNickname() : "나";

            CommentResponse data = postService.createComment(postId, me.getId(), nickname, request.getContent());
            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("post_not_found", null));
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>("create_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "댓글 수정", description = "특정 댓글의 내용을 수정합니다.")
    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Object>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentRequest request
    ) {
        try {
            User me = currentUserOrNull();
            if (me == null) return unauthorized();

            if (request == null || request.getContent() == null || request.getContent().isBlank()) {
                return badRequest("invalid_request");
            }

            UpdateCommentResponse data =
                    postService.updateComment(postId, commentId, me.getId(), request.getContent());

            if (data == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("not_found_or_forbidden", null));
            }
            return ResponseEntity.ok(new ApiResponse<>("update_success", data));
        } catch (Exception e) {
            return internalError();
        }
    }

    @Operation(summary = "댓글 삭제", description = "특정 댓글을 삭제합니다. 성공 시 204 반환.")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        try {
            User me = currentUserOrNull();
            if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            boolean ok = postService.deleteComment(postId, commentId, me.getId());
            if (!ok) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/{id}/views")
    public ResponseEntity<?> increaseViews(@PathVariable Long id) {
        Integer views = postService.increaseViews(id);
        if (views == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("data", Map.of("views", views)));
    }
}
