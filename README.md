# 게시판 REST API (Backend)

JWT 기반 인증을 사용하는 게시판 백엔드 서버입니다.
회원가입/로그인, 내 정보 수정, 게시글·댓글·좋아요 기능을 제공합니다.

---

## 1. 기술 스택

* Java 21
* Spring Boot 3.5.6

  * spring-boot-starter-web
  * spring-boot-starter-data-jpa
  * spring-boot-starter-security
* MySQL + JPA(Hibernate)
* Lombok
* JWT (직접 구현한 TokenProvider)
* springdoc-openapi (Swagger UI)
* Gradle 8.x

---

## 2. 주요 기능 요약

### 2-1. 인증 / 인가

* 로그인: `POST /api/auth/login`

  * 이메일 + 비밀번호로 인증 후 **JWT 토큰 발급**
  * 응답 예: `data.token`, `data.user_id`, `data.email`, `data.nickname`

* 로그아웃: `POST /api/auth/logout`

  * 서버 상태는 유지하지 않고, 클라이언트가 토큰 삭제 (Stateless)

* 보안 설정 (`SecurityConfig`)

  * `Bearer <JWT>` 헤더를 읽는 `JwtFilter` 적용
  * 허용 경로

    * `POST /api/users/signup` (회원가입)
    * `GET  /api/users/exists/email` (이메일 중복 체크)
    * `GET  /api/users/count/nickname` (닉네임 중복 체크)
    * `POST /api/auth/login` (로그인)
    * `GET  /api/posts/**` (게시글 조회)
    * Swagger 관련 URL
  * 그 외 `/api/**` 는 모두 인증 필요

---

### 2-2. 회원(User)

* 회원가입: `POST /api/users/signup`
* 내 정보 조회: `GET /api/users/me`
* 내 정보 수정: `PATCH /api/users/me`

  * 닉네임, 프로필 이미지 변경
* 비밀번호 변경: `PATCH /api/users/me/password`

  * BCryptPasswordEncoder 사용하여 암호화 저장

---

### 2-3. 게시글(Post)

* 목록 조회: `GET /api/posts`
* 상세 조회: `GET /api/posts/{postId}`

  * 제목, 내용, 작성자, 이미지 리스트, 좋아요 수, 댓글 수, 조회수, 작성/수정 시각, `is_author`
* 생성: `POST /api/posts`

  * 제목(최대 26자), 내용 필수, 이미지 선택
* 수정: `PATCH /api/posts/{postId}`
* 삭제: `DELETE /api/posts/{postId}`

  * 실제 삭제 대신 `is_deleted = true` 로 **Soft Delete**
* 제목 검색: `GET /api/posts/search/title?keyword=...`
* 작성자 닉네임 검색: `GET /api/posts/search/author?nickname=...`
* 조회수 증가: `POST /api/posts/{id}/views`

---

### 2-4. 댓글(Comment) & 좋아요(Like)

* 댓글 목록: `GET /api/posts/{postId}/comments`

* 댓글 작성: `POST /api/posts/{postId}/comments`

* 댓글 수정: `PATCH /api/posts/{postId}/comments/{commentId}`

* 댓글 삭제: `DELETE /api/posts/{postId}/comments/{commentId}`

* 좋아요 추가: `POST /api/posts/{postId}/likes`

* 좋아요 취소: `DELETE /api/posts/{postId}/likes`

* `PostLikeId(post_id, user_id)` 복합키를 사용해 **같은 글에는 한 번만 좋아요** 가능

---

## 3. 공통 응답 형식

모든 API는 아래와 같은 공통 응답을 사용합니다.

```json
{
  "code": "read_success",
  "data": { ... }
}
```

---

## 4. 시연 영상

* YouTube 시연 영상: [https://youtu.be/mS4scIFhZLI](https://youtu.be/mS4scIFhZLI)
