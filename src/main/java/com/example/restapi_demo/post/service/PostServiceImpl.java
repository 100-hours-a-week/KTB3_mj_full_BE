package com.example.restapi_demo.post.service;

import com.example.restapi_demo.post.dto.*;
import com.example.restapi_demo.post.model.Post;
import com.example.restapi_demo.post.repository.JpaPostEntityRepository;
import com.example.restapi_demo.post.repository.PostRepository;
import com.example.restapi_demo.post.repository.PostRepository.DetailSeed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository repo;
    private final JpaPostEntityRepository jpaRepo;

    // ✅ 두 리포지토리 모두 생성자에서 주입
    public PostServiceImpl(PostRepository repo, JpaPostEntityRepository jpaRepo) {
        this.repo = repo;
        this.jpaRepo = jpaRepo;
    }

    @Override
    @Transactional(readOnly = true) // ✅ 읽기 트랜잭션
    public PostListResponse getPosts() {
        // ✅ 목록도 JPA에서 최신값으로 읽기 (조회수 증가 반영됨)
        List<Post> all = jpaRepo.findAllWithAuthorAndImages();

        List<PostSummary> content = all.stream()
                .map(p -> new PostSummary(
                        p.getId(),
                        p.getTitle(),
                        p.getAuthor() != null ? p.getAuthor().getNickname() : null,
                        // null 방지
                        Optional.ofNullable(p.getLikesCount()).orElse(0),
                        Optional.ofNullable(p.getCommentsCount()).orElse(0),
                        Optional.ofNullable(p.getViews()).orElse(0),
                        p.getCreatedAt()
                ))
                .collect(Collectors.toList());

        int page = 0;
        int size = 10;
        long totalElements = content.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PostListResponse(content, page, size, totalElements, totalPages);
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
        System.out.println("=== getPostDetail 호출 ===");
        System.out.println("postId: " + postId);
        System.out.println("requestUserId: " + requestUserId);


        List<Post> allPosts = repo.findAll();
        System.out.println("📊 전체 Post 개수: " + allPosts.size());
        System.out.println("📋 존재하는 Post ID 목록:");
        allPosts.forEach(p -> {
            System.out.println("  - ID: " + p.getId() +
                    ", Title: " + p.getTitle() +
                    ", isDeleted: " + p.getIsDeleted());
        });

        try {
            Optional<DetailSeed> detailOpt = repo.findDetailById(postId);
            System.out.println("findDetailById 결과 존재 여부: " + detailOpt.isPresent());

            return detailOpt
                    .map(d -> {
                        System.out.println("프로젝션 사용 - title: " + d.getTitle());
                        System.out.println("authorName: " + d.getAuthorName());
                        System.out.println("authorId: " + d.getAuthorId());
                        return new PostDetailResponse(
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
                        );
                    })
                    .orElseGet(() -> {
                        System.out.println("프로젝션 비어있음 - fallback 사용");
                        return fallbackDetail(postId, requestUserId);
                    });
        } catch (Exception e) {
            System.out.println("예외 발생: " + e.getMessage());
            e.printStackTrace();
            return fallbackDetail(postId, requestUserId);
        }
    }

    private PostDetailResponse fallbackDetail(Long postId, Long requestUserId) {
        System.out.println("=== fallbackDetail 호출 ===");

        Optional<Post> postOpt = repo.findById(postId);
        System.out.println("findById 결과 존재 여부: " + postOpt.isPresent());

        if (postOpt.isPresent()) {
            Post p = postOpt.get();
            System.out.println("Post 발견 - id: " + p.getId());
            System.out.println("title: " + p.getTitle());
            System.out.println("isDeleted: " + p.getIsDeleted());
            System.out.println("필터 통과 여부: " + !Boolean.TRUE.equals(p.getIsDeleted()));

            if (p.getAuthor() != null) {
                System.out.println("작성자 정보 - id: " + p.getAuthor().getId() + ", nickname: " + p.getAuthor().getNickname());
            } else {
                System.out.println("작성자 정보 없음 (author is null)");
            }
        } else {
            System.out.println("Post를 찾을 수 없음!");
        }

        return postOpt
                .filter(p -> {
                    boolean pass = !Boolean.TRUE.equals(p.getIsDeleted());
                    System.out.println("필터 결과: " + pass);
                    return pass;
                })
                .map(p -> {
                    System.out.println("PostDetailResponse 생성 중...");
                    return new PostDetailResponse(
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
                    );
                })
                .orElseGet(() -> {
                    System.out.println("최종 결과: null 반환");
                    return null;
                });
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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