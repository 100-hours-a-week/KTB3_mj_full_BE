package com.example.restapi_demo.post.model;

import com.example.restapi_demo.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_likes",
        indexes = @Index(name = "idx_post_likes_user", columnList = "user_id"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id",
            foreignKey = @ForeignKey(name = "fk_post_likes_post"))
    private Post post;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "fk_post_likes_user"))
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }


    public static PostLike of(Post post, User user) {
        return PostLike.builder()
                .id(new PostLikeId(post.getId(), user.getId()))
                .post(post)
                .user(user)
                .build();
    }
}
