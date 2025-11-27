// src/main/java/com/example/restapi_demo/post/repository/JpaPostEntityRepository.java
package com.example.restapi_demo.post.repository;

import com.example.restapi_demo.post.model.Comment;
import com.example.restapi_demo.post.model.Post;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaPostEntityRepository extends JpaRepository<Post, Long> {

    @Query("""
       select distinct p
       from Post p
       join fetch p.author a
       left join fetch p.images imgs
       where (p.isDeleted = false or p.isDeleted is null)
       order by p.id desc
       """)
    List<Post> findAllWithAuthorAndImages();

    @Query("""
       select distinct p
       from Post p
       join fetch p.author a
       left join fetch p.images imgs
       where p.id = :postId
         and (p.isDeleted = false or p.isDeleted is null)
       """)
    Optional<Post> findDetailWithAuthorAndImages(@Param("postId") Long postId);

    @Query("""
       select c
       from Comment c
       join fetch c.author ca
       where c.post.id = :postId
       order by c.createdAt asc, c.id asc
       """)
    List<Comment> findCommentsByPostId(@Param("postId") Long postId);

    List<Post> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String keyword);
    List<Post> findByAuthor_NicknameAndIsDeletedFalse(String nickname);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.views = coalesce(p.views, 0) + 1 where p.id = :postId")
    int increaseViews(@Param("postId") Long postId);


    @Query("select p.views from Post p where p.id = :postId")
    Optional<Integer> findViews(@Param("postId") Long postId);
}
