package com.example.restapi_demo.post.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


@Schema(description = "댓글 작성 응답 DTO (댓글 생성 시 반환되는 데이터 구조)")
public class CommentResponse {
    @Schema(description = "댓글 ID", example = "101")
    private Long comment_id;
    @Schema(description = "댓글 작성자명", example = "박성현")
    private String author;
    @Schema(description = "댓글 내용", example = "댓글입니다")
    private String content;
    @Schema(description = "댓글 작성 시각", example = "2025-10-19 3시")
    private LocalDateTime created_at;

    public CommentResponse(Long comment_id, String author, String content, LocalDateTime created_at) {
        this.comment_id = comment_id;
        this.author = author;
        this.content = content;
        this.created_at = created_at;
    }

    public Long getComment_id() { return comment_id; }
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getCreated_at() { return created_at; }
}
