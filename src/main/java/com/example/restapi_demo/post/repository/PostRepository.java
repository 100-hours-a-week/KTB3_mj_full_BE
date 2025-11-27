package com.example.restapi_demo.post.repository;

import com.example.restapi_demo.post.model.Comment;
import com.example.restapi_demo.post.model.Post;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository {


    List<Post> findAll();
    Post save(Post post);

    List<Post> findByTitleContainingIgnoreCase(String keyword);
    List<Post> findByAuthorNickname(String nickname);


    Optional<DetailSeed> findDetailById(Long postId);
    Optional<Post> findById(Long id);


    boolean deleteById(Long postId);

    List<Comment> findCommentsByPostId(Long postId);



    Optional<Integer> incrementLikes(Long postId);
    Optional<Integer> decrementLikes(Long postId);


    Optional<Comment> addComment(Long postId, Long authorId, String authorName, String content);
    Optional<Comment> updateComment(Long postId, Long commentId, Long requesterId, String newContent);
    boolean deleteComment(Long postId, Long commentId, Long requesterId);


    Optional<DetailSeed> updatePost(Long postId, String newTitle, String newContent, String newImage);
    Optional<Post> createPost(Long authorId, String authorName, String title, String content, String image);


    class DetailSeed {
        private Long postId;
        private Long authorId;
        private String title;
        private String authorName;
        private String content;
        @Setter
        private List<String> images;
        private int likesCount;       // <- 필드명 정리
        private int views;
        private int commentsCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public DetailSeed(
                Long postId,
                Long authorId,
                String title,
                String authorName,
                String content,
                List<String> images,
                int likesCount,
                int views,
                int commentsCount,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
            this.postId = postId;
            this.authorId = authorId;
            this.title = title;
            this.authorName = authorName;
            this.content = content;
            this.images = images;
            this.likesCount = likesCount;
            this.views = views;
            this.commentsCount = commentsCount;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public Long getPostId() { return postId; }
        public Long getAuthorId() { return authorId; }
        public String getTitle() { return title; }
        public String getAuthorName() { return authorName; }
        public String getContent() { return content; }
        public List<String> getImages() { return images; }
        public int getLikesCount() { return likesCount; }   // <- 서비스/응답과 동일 명명
        public int getViews() { return views; }
        public int getCommentsCount() { return commentsCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }

        public void setTitle(String title) { this.title = title; }
        public void setContent(String content) { this.content = content; }

        public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
        public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
