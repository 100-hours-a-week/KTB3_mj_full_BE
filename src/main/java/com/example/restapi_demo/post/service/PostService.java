package com.example.restapi_demo.post.service;

import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Post;

import java.util.List;

public interface PostService {
    PostListResponse getPosts(int page, int size);

    PostDetailResponse getPostDetail(Long postId, Long requestUserId);

    boolean deletePost(Long postId, Long requesterId);

    Integer addLike(Long postId, Long requesterId);
    Integer removeLike(Long postId, Long requesterId);

    List<CommentResponse> getComments(Long postId, Long requestUserId);

    CommentResponse createComment(Long postId, Long requesterId, String requesterNickname, String content);
    UpdateCommentResponse updateComment(Long postId, Long commentId, Long requesterId, String content);
    boolean deleteComment(Long postId, Long commentId, Long requesterId);

    PostUpdateResponse updatePost(Long postId, Long requesterId, String title, String content, String image);

    Post createPost(Long authorId, String authorName, String title, String content, String image);

    List<PostSummary> searchByTitle(String keyword);
    List<PostSummary> findByAuthorNickname(String nickname);

    Integer increaseViews(Long postId);
}
