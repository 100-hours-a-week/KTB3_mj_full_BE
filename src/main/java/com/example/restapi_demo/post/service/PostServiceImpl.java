package com.example.restapi_demo.post.service;

import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.repository.JpaPostEntityRepository;
import com.example.restapi_demo.post.repository.PostRepository;
import com.example.restapi_demo.post.repository.PostRepository.DetailSeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository repo;
    private final JpaPostEntityRepository jpaRepo;

    public PostServiceImpl(PostRepository repo, JpaPostEntityRepository jpaRepo) {
        this.repo = repo;
        this.jpaRepo = jpaRepo;
    }


    @Override
    @Transactional(readOnly = true)
    public PostListResponse getPosts(int page, int size) {
        // page, size에 대한 방어 코드 (음수 들어오는 것 방지)
        int pageIndex = Math.max(page, 0);
        int pageSize = (size <= 0) ? 10 : size;

        // 최신 글 기준 정렬
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // ★ JPA 페이징 조회
        Page<Post> postPage = jpaRepo.findAll(pageable);

        List<PostSummary> content = postPage.getContent().stream()
                .map(p -> new PostSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getAuthor() != null ? p.getAuthor().getNickname() : null,
                        Optional.ofNullable(p.getLikesCount()).orElse(0),
                        Optional.ofNullable(p.getCommentsCount()).orElse(0),
                        Optional.ofNullable(p.getViews()).orElse(0),
                        p.getCreatedAt()
                ))
                .toList();

        return new PostListResponse(
                content,
                postPage.getNumber(),          // 현재 페이지 번호
                postPage.getSize(),            // 페이지 크기
                postPage.getTotalElements(),   // 전체 게시글 수
                postPage.getTotalPages()       // 전체 페이지 수
        );
    }


    @Override
    public List<CommentResponse> getComments(Long postId, Long requestUserId) {
        return repo.findCommentsByPostId(postId).stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        c.getAuthor() != null ? c.getAuthor().getNickname() : "작성자",
                        c.getContent(),
                        c.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public PostDetailResponse getPostDetail(Long postId, Long requestUserId) {
        try {
            Optional<DetailSeed> detailOpt = repo.findDetailById(postId);

            return detailOpt
                    .map(d -> new PostDetailResponse(
                            d.getPostId(),
                            s(d.getTitle()),
                            s(d.getAuthorName()),
                            s(d.getContent()),
                            d.getImages() != null ? d.getImages() : List.of(),
                            nz(d.getLikesCount()),
                            nz(d.getViews()),
                            nz(d.getCommentsCount()),
                            requestUserId != null && requestUserId.equals(d.getAuthorId()),
                            d.getCreatedAt(),
                            d.getUpdatedAt()
                    ))
                    .orElseGet(() -> fallbackDetail(postId, requestUserId));
        } catch (Exception e) {
            return fallbackDetail(postId, requestUserId);
        }
    }

    private PostDetailResponse fallbackDetail(Long postId, Long requestUserId) {
        return repo.findById(postId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(p -> new PostDetailResponse(
                        p.getId(),
                        s(p.getTitle()),
                        p.getAuthor() != null ? s(p.getAuthor().getNickname()) : "작성자",
                        s(p.getContent()),
                        p.getImages() != null
                                ? p.getImages().stream().map(pi -> s(pi.getUrl())).toList()
                                : List.of(),
                        nz(p.getLikesCount()),
                        nz(p.getViews()),
                        nz(p.getCommentsCount()),
                        requestUserId != null && p.getAuthor() != null
                                && requestUserId.equals(p.getAuthor().getId()),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                ))
                .orElse(null);
    }

    private String s(String v) { return v == null ? "" : v; }
    private int nz(Integer v) { return v == null ? 0 : v; }

    @Override
    @Transactional
    public boolean deletePost(Long postId, Long requesterId) {
        return repo.findById(postId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .filter(p -> p.getAuthor() != null && requesterId != null && requesterId.equals(p.getAuthor().getId()))
                .map(p -> repo.deleteById(postId))
                .orElse(false);
    }

    @Override
    public Integer addLike(Long postId, Long requesterId) {
        return repo.incrementLikes(postId).orElse(null);
    }

    @Override
    public Integer removeLike(Long postId, Long requesterId) {
        return repo.decrementLikes(postId).orElse(null);
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long postId, Long requesterId, String requesterNickname, String content) {
        String authorName = (requesterNickname == null || requesterNickname.isBlank()) ? "나" : requesterNickname;

        return repo.addComment(postId, requesterId, authorName, content)
                .map(c -> new CommentResponse(
                        c.getId(),
                        (c.getAuthor() != null ? c.getAuthor().getNickname() : authorName),
                        c.getContent(),
                        c.getCreatedAt()
                ))
                .orElse(null);
    }

    @Override
    public UpdateCommentResponse updateComment(Long postId, Long commentId, Long requesterId, String content) {
        return repo.updateComment(postId, commentId, requesterId, content)
                .map(c -> new UpdateCommentResponse(c.getId(), c.getContent()))
                .orElse(null);
    }

    @Override
    public boolean deleteComment(Long postId, Long commentId, Long requesterId) {
        return repo.deleteComment(postId, commentId, requesterId);
    }

    @Override
    @Transactional
    public PostUpdateResponse updatePost(Long postId, Long requesterId, String title, String content, String image) {
        boolean allowed = repo.findById(postId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .filter(p -> p.getAuthor() != null && requesterId != null && requesterId.equals(p.getAuthor().getId()))
                .isPresent();

        if (!allowed) return null;

        return repo.updatePost(postId, title, content, image)
                .map((DetailSeed d) -> new PostUpdateResponse(
                        d.getPostId(),
                        d.getTitle(),
                        d.getContent(),
                        (d.getImages() == null || d.getImages().isEmpty()) ? null : d.getImages().get(0),
                        d.getUpdatedAt()
                ))
                .orElse(null);
    }

    @Override
    public Post createPost(Long authorId, String authorName, String title, String content, String image) {
        return repo.createPost(authorId, authorName, title, content, image).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostSummary> searchByTitle(String keyword) {
        return repo.findByTitleContainingIgnoreCase(keyword).stream()
                .map(p -> new PostSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getAuthor() != null ? p.getAuthor().getNickname() : null,
                        p.getLikesCount(),
                        p.getCommentsCount(),
                        p.getViews(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostSummary> findByAuthorNickname(String nickname) {
        return repo.findByAuthorNickname(nickname).stream()
                .map(p -> new PostSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getAuthor() != null ? p.getAuthor().getNickname() : null,
                        p.getLikesCount(),
                        p.getCommentsCount(),
                        p.getViews(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public Integer increaseViews(Long postId) {
        int updated = jpaRepo.increaseViews(postId);
        if (updated == 0) return null;
        return jpaRepo.findViews(postId).orElse(null);
    }
}