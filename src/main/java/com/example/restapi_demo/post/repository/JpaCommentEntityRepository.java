package com.example.restapi_demo.post.repository;

import com.example.restapi_demo.post.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaCommentEntityRepository extends JpaRepository<Comment, Long> {

    @Query("""
        select c
        from Comment c
        join fetch c.author a
        where c.post.id = :postId
        order by c.createdAt asc, c.id asc
    """)
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
}
