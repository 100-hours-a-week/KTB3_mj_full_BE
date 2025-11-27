package com.example.restapi_demo.post.model;

import com.example.restapi_demo.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PostLikeId implements Serializable {
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "user_id")
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostLikeId that)) return false;
        return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(postId, userId);
    }
}
