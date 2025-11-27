package com.example.restapi_demo.post.repository;

import com.example.restapi_demo.post.model.Comment;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.model.PostImage;
import com.example.restapi_demo.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaPostRepositoryAdapter implements PostRepository {

    private final JpaPostEntityRepository postJpa;
    private final JpaCommentEntityRepository commentJpa;

    @Override
    @Transactional(readOnly = true)
    public List<Post> findAll() {
        return postJpa.findAllWithAuthorAndImages();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        return postJpa.findById(id);
    }

    @Override
    public Post save(Post post) {
        return postJpa.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findCommentsByPostId(Long postId) {
        return commentJpa.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findByTitleContainingIgnoreCase(String keyword) {
        return postJpa.findByTitleContainingIgnoreCaseAndIsDeletedFalse(keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> findByAuthorNickname(String nickname) {
        return postJpa.findByAuthor_NicknameAndIsDeletedFalse(nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetailSeed> findDetailById(Long postId) {
        return postJpa.findDetailWithAuthorAndImages(postId).map(p -> {
            Long authorId = (p.getAuthor() != null) ? p.getAuthor().getId() : null;
            String authorName = (p.getAuthor() != null) ? p.getAuthor().getNickname() : null;

            List<String> images = (p.getImages() == null) ? List.of()
                    : p.getImages().stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSortOrder() == null ? 0 : a.getSortOrder(),
                            b.getSortOrder() == null ? 0 : b.getSortOrder()))
                    .map(PostImage::getUrl)
                    .collect(Collectors.toList());

            return new DetailSeed(
                    p.getId(),
                    authorId,
                    p.getTitle(),
                    authorName,
                    p.getContent(),
                    images,
                    p.getLikesCount(),
                    p.getViews(),
                    p.getCommentsCount(),
                    p.getCreatedAt(),
                    p.getUpdatedAt()
            );
        });
    }

    @Override
    public boolean deleteById(Long postId) {
        return postJpa.findById(postId).map(p -> {
            p.setIsDeleted(true);
            postJpa.save(p);
            return true;
        }).orElse(false);
    }

    @Override
    public Optional<Integer> incrementLikes(Long postId) {
        return postJpa.findById(postId).map(p -> {
            p.setLikesCount((p.getLikesCount() == null ? 0 : p.getLikesCount()) + 1);
            postJpa.save(p);
            return p.getLikesCount();
        });
    }

    @Override
    public Optional<Integer> decrementLikes(Long postId) {
        return postJpa.findById(postId).map(p -> {
            int now = Math.max(0, (p.getLikesCount() == null ? 0 : p.getLikesCount()) - 1);
            p.setLikesCount(now);
            postJpa.save(p);
            return p.getLikesCount();
        });
    }

    @Override
    public Optional<Comment> addComment(Long postId, Long authorId, String authorName, String content) {
        return postJpa.findById(postId).map(p -> {
            User author = User.builder()
                    .id(authorId)
                    .nickname(authorName)
                    .build();

            Comment c = Comment.builder()
                    .post(p)
                    .author(author)
                    .content(content)
                    .build();

            Comment saved = commentJpa.save(c);

            p.setCommentsCount((p.getCommentsCount() == null ? 0 : p.getCommentsCount()) + 1);
            postJpa.save(p);

            return saved;
        });
    }

    @Override
    public Optional<Comment> updateComment(Long postId, Long commentId, Long requesterId, String newContent) {
        return commentJpa.findById(commentId)
                .filter(c ->
                        Objects.equals(c.getPost().getId(), postId) &&
                                c.getAuthor() != null &&
                                Objects.equals(c.getAuthor().getId(), requesterId))
                .map(c -> {
                    c.setContent(newContent);
                    c.setUpdatedAt(LocalDateTime.now());
                    return commentJpa.save(c);
                });
    }

    @Override
    public boolean deleteComment(Long postId, Long commentId, Long requesterId) {
        return commentJpa.findById(commentId)
                .filter(c ->
                        Objects.equals(c.getPost().getId(), postId) &&
                                c.getAuthor() != null &&
                                Objects.equals(c.getAuthor().getId(), requesterId))
                .map(c -> {
                    commentJpa.delete(c);
                    Post p = c.getPost();
                    int now = Math.max(0, (p.getCommentsCount() == null ? 0 : p.getCommentsCount()) - 1);
                    p.setCommentsCount(now);
                    postJpa.save(p);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public Optional<DetailSeed> updatePost(Long postId, String newTitle, String newContent, String newImage) {
        return postJpa.findById(postId).map(p -> {
            if (newTitle != null) p.setTitle(newTitle);
            if (newContent != null) p.setContent(newContent);

            if (newImage != null) {
                if (p.getImages() == null || p.getImages().isEmpty()) {
                    PostImage img = PostImage.builder()
                            .post(p)
                            .url(newImage)
                            .sortOrder(0)
                            .build();
                    p.getImages().add(img);
                } else {
                    PostImage first = p.getImages().stream()
                            .sorted((a, b) -> Integer.compare(
                                    a.getSortOrder() == null ? 0 : a.getSortOrder(),
                                    b.getSortOrder() == null ? 0 : b.getSortOrder()))
                            .findFirst()
                            .orElse(null);
                    if (first != null) first.setUrl(newImage);
                }
            }

            p.setUpdatedAt(LocalDateTime.now());
            Post saved = postJpa.save(p);

            Long authorId = (saved.getAuthor() != null) ? saved.getAuthor().getId() : null;
            String authorName = (saved.getAuthor() != null) ? saved.getAuthor().getNickname() : null;

            List<String> images = (saved.getImages() == null) ? List.of()
                    : saved.getImages().stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSortOrder() == null ? 0 : a.getSortOrder(),
                            b.getSortOrder() == null ? 0 : b.getSortOrder()))
                    .map(PostImage::getUrl)
                    .collect(Collectors.toList());

            return new DetailSeed(
                    saved.getId(),
                    authorId,
                    saved.getTitle(),
                    authorName,
                    saved.getContent(),
                    images,
                    saved.getLikesCount(),
                    saved.getViews(),
                    saved.getCommentsCount(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );
        });
    }

    @Override
    public Optional<Post> createPost(Long authorId, String authorName, String title, String content, String image) {
        if (title == null || content == null) return Optional.empty();

        User author = User.builder()
                .id(authorId)
                .nickname(authorName)
                .build();

        Post p = Post.builder()
                .author(author)
                .title(title)
                .content(content)
                .views(0)
                .likesCount(0)
                .commentsCount(0)
                .isDeleted(false)
                .build();

        if (image != null) {
            PostImage img = PostImage.builder()
                    .post(p)
                    .url(image)
                    .sortOrder(0)
                    .build();
            p.getImages().add(img);
        }

        return Optional.of(postJpa.save(p));
    }
}