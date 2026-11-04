# 9주차 정리 · 게시판 CRUD와 본인 확인

자바웹프로그래밍(2) 9주차 강의 PDF(28쪽) 기준 정리. 연습문제(조회수, 내가 쓴 글)는 다음 단계에서 진행하므로 여기서는 제외했다.

## 1. 오늘의 핵심 한 줄

> 게시판 = **CRUD** + **작성자 연결** + **본인 확인**. 버튼을 숨기는 것만으로는 막을 수 없고, **서버가 "이 글의 작성자가 맞나?"를 직접 확인**해야 한다.

## 2. 왜 게시판에 보안이 필요한가

- 위협 : 주소의 번호만 바꿔 남의 글을 수정·삭제 (`/board/2/edit`) → 7주차 OWASP **A01 접근 제어 실패**와 같은 유형
- 6주차 : 관리자 메뉴만 숨겼더니 주소로 들어갔다 → 9주차 : 수정 버튼만 숨기면 주소로 들어간다

| 주차 | 기준 | 질문 | 실패 예 |
|---|---|---|---|
| 5주차 | 로그인 | 누구세요? | 비로그인 → 로그인 화면 |
| 6주차 | 역할(ROLE) | 관리자인가요? | USER → 관리자 페이지 403 |
| 9주차 | 주인(작성자) | 이 글을 쓴 사람인가? | 남의 글 수정 → 403 |

## 3. 실습 순서

1. 제공 화면 복사 : `templates/board/`(목록·상세·글쓰기/수정 공용), `templates/error/404.html`, `static/css/board.css`, `index.html`에 "게시판" 메뉴
   - 직접 꾸민 학생은 **게시판 메뉴 추가 부분만** 반영 (index.html 전체를 덮어쓰지 않기)
2. 글 엔티티(작성자 연결) → `Post.java`
3. DTO·리포지토리 → `PostForm.java`, `PostRepository.java`
4. 글쓰기·목록 → `PostService`, `BoardController`, `SecurityConfig`
5. 상세 보기 + 404
6. 수정·삭제 (문제 : 남의 글도 고쳐진다!)
7. 본인 확인 (`checkWriter`, `isAdmin`)

### 화면 ↔ 컨트롤러 약속

| 화면 | 주소 | 컨트롤러가 넘겨줄 값 |
|---|---|---|
| `board/list.html` | `GET /board` | `posts` (글 목록, "전체 N개"도 이 크기) |
| `board/detail.html` | `GET /board/{id}` | `post` (글 1개) |
| `board/form.html` | `GET /board/write`, `/board/{id}/edit` | `postForm`(입력값), `postId`(수정일 때), `error` |

## 4. CRUD와 JPA, 주소

| 기능 | SQL | JPA | 화면 주소 (오늘) | 7주차 REST라면 |
|---|---|---|---|---|
| 쓰기 (Create) | INSERT | `save()` | `POST /board/write` | `POST /api/posts` |
| 목록 (Read) | SELECT | `findAll()` | `GET /board` | `GET /api/posts` |
| 상세 (Read) | SELECT … WHERE id | `findById()` | `GET /board/3` | `GET /api/posts/3` |
| 수정 (Update) | UPDATE | `save()` | `POST /board/3/edit` | `PATCH /api/posts/3` |
| 삭제 (Delete) | DELETE | `delete()` | `POST /board/3/delete` | `DELETE /api/posts/3` |

- **HTML 폼은 GET·POST만** 보낼 수 있다 → PATCH·DELETE 대신 주소 끝에 할 일(`/edit`, `/delete`)을 붙인다
- 목록·상세 : 누구나(GET) / 쓰기·수정·삭제 : 로그인한 사람(폼 → POST)

## 5. 글 엔티티와 작성자 연결 (`@ManyToOne`)

```java
@Entity
@Table(name = "posts")
@Data
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY)              // 글 N : 회원 1
    @JoinColumn(name = "member_id", nullable = false) // FK 컬럼 이름
    private Member author;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;                 // 수정한 적 없으면 null
    @Column(nullable = false)
    private int viewCount = 0;                       // 연습문제에서 사용
}
```

| 시점 | 읽는 법 | 어노테이션 | 붙는 위치 |
|---|---|---|---|
| 회원에서 보면 | 회원 1명 → 글 여러 개 (1 : N) | `@OneToMany` | Member |
| 글에서 보면 | 글 여러 개 → 회원 1명 (N : 1) | `@ManyToOne` | Post |

- 게시글(Post) 기준으로 읽으므로 **N : 1 = `@ManyToOne`**

**작성자를 왜 문자열이 아니라 객체로?**

| 문제 | `String writer = "student1"` | `Member author + @ManyToOne` |
|---|---|---|
| 작성자 이름 보기 | 회원을 다시 찾아야 함 | `post.author.name` 한 번에 |
| 회원 정보가 바뀌면 | 글 쪽 값과 어긋남 | 항상 최신 회원 정보 |
| 없는 회원 이름 입력 | 막을 수 없음 | DB의 FK가 막아 줌 |
| 회원 삭제 시 | 주인 없는 글이 남음 | FK 때문에 오류 → 처리 필요(10주차) |

**DB 테이블 매핑**

| 자바 필드 | DB 컬럼 | 타입 |
|---|---|---|
| id | id | bigint (자동 증가, PK) |
| title | title | varchar(100) |
| content | content | TEXT |
| author | member_id | bigint (FK → member.id) |
| createdAt / updatedAt | created_at / updated_at | datetime |
| viewCount | view_count | int |

- **외래 키(FK) = DB가 지키는 약속** : `member_id`는 반드시 존재하는 회원 번호
- **`FetchType.LAZY`** : 연결된 객체를 바로 읽지 않고 필요할 때 가져온다 (불필요한 조회 감소)

## 6. DTO · 리포지토리

```java
@Data
public class PostForm {   // 화면 → 컨트롤러
    private String title;    // input name="title"
    private String content;  // textarea name="content"
    // 작성자는 받지 않는다 → 로그인한 사람으로 서버가 정한다
}

@Repository
public interface PostRepository extends JpaRepository<Post, Long> { }
// save, findAll, findById, delete 는 이미 있다
```

| 구분 | MemberForm (5주차) | PostForm (9주차) |
|---|---|---|
| 받는 값 | 아이디, 비밀번호, 확인, 이름 | 제목, 내용 |
| 받지 않는 값 | role (권한) | author (작성자) |
| 막는 것 | 권한 조작 | 작성자 바꿔치기 (남의 이름으로 글쓰기) |

## 7. 글쓰기 · 목록

**글쓰기 순서 : ① 검사 → ② 작성자 찾기 → ③ 저장**

```java
public List<Post> findAll() {                     // 최신 글이 위로
    return postRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
}

public Post write(PostForm form, String username) {
    validate(form);                                // ① 빈 값·100자 검사
    Member author = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    Post post = new Post();
    post.setTitle(form.getTitle().trim());
    post.setContent(form.getContent());
    post.setAuthor(author);                        // ② 작성자는 서버가 정한다
    post.setCreatedAt(LocalDateTime.now());
    return postRepository.save(post);              // ③ INSERT
}
```

- 컨트롤러 : `principal.getName()` = 로그인한 아이디 → 작성자. 성공하면 `redirect:/board/{id}`, `IllegalArgumentException`이면 `error`를 담아 같은 폼 다시 표시
- `SecurityConfig`

```java
.requestMatchers("/board", "/board/{id:\\d+}").permitAll() // 목록·상세 : 누구나
```

- `{id:\d+}` = 숫자만 → `/board/write`는 포함 안 됨 (로그인 필요)
- `/board/**`로 넓게 허용하면 글쓰기까지 누구나 열려 버린다

## 8. 상세 보기와 404

```java
public Post findById(Long id) {
    return postRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "글이 존재하지 않습니다."));
}
```

- `ResponseStatusException(NOT_FOUND)` → 스프링 부트가 `templates/error/404.html`을 자동으로 보여 줌 (6주차 `error/403.html`과 같은 방식)
- 상세 화면은 `th:text`로 내용 출력 → HTML 태그가 글자로 보인다 (XSS 방지)

## 9. 수정 · 삭제

- JPA에는 별도의 "수정 메서드"가 없다 → 조회한 엔티티 값을 바꾸고 `save()` (변경 감지, Dirty Checking)

| 상황 | post의 id | `save()`가 하는 일 |
|---|---|---|
| 글쓰기 (write) | 없음 (null) | INSERT |
| 수정 (edit) | 있음 | UPDATE |

```java
public Post edit(Long id, PostForm form) {
    Post post = findForEdit(id);
    validate(form);
    post.setTitle(form.getTitle().trim());
    post.setContent(form.getContent());
    post.setUpdatedAt(LocalDateTime.now());  // 수정 시간 기록
    return postRepository.save(post);        // UPDATE
}
```

**글쓰기·수정 화면 하나로 쓰기 (form.html)**

| 구분 | 글쓰기 | 수정 |
|---|---|---|
| postId | 없음 | 글 번호 |
| 폼 전송 주소 | `POST /board/write` | `POST /board/{id}/edit` |
| 입력칸 | 빈 칸 | 기존 제목·내용 채움 |

- 삭제 후 `redirect:/board?deleted` → 목록에 "글이 삭제되었습니다."

**문제 발견 (단계 5 테스트)** : student3이 student1의 글에서 버튼은 안 보이지만, 주소창에 `/board/2/edit`을 치면 수정 화면이 열리고 저장까지 된다 → 버튼은 숨겼지만 **서버는 확인하지 않았다**.

## 10. 본인 확인 (핵심)

- 서버가 확인할 것 : **로그인한 사람 = 글쓴이?** 다르면 `AccessDeniedException` → 403 화면
- 관리자 규칙 : **수정은 본인만**(대신 고치면 안 됨), **삭제는 본인 또는 관리자**(규칙 위반 글 정리)
- URL 규칙으로 못 하는 이유 : `/board/2/edit`의 주인은 **DB를 봐야 안다** → 서비스 코드에서 확인

```java
public Post findForEdit(Long id) {          // 수정 화면·수정 처리 모두 여기를 지난다
    Post post = findById(id);
    checkWriter(post);                      // ★ 본인만
    return post;
}

public void delete(Long id) {
    Post post = findById(id);
    if (!isAdmin()) checkWriter(post);      // ★ 관리자는 누구 글이든 삭제 가능
    postRepository.delete(post);
}

private void checkWriter(Post post) {
    String loginUsername = SecurityContextHolder.getContext().getAuthentication().getName();
    if (!post.getAuthor().getUsername().equals(loginUsername)) {
        throw new AccessDeniedException("본인이 쓴 글만 수정·삭제할 수 있습니다.");
    }
}

private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
}
```

- `edit()`은 `findForEdit()`을 부르므로 **자동으로 본인 확인**된다
- **이중 확인** = 화면(버튼 숨김, 1차 안내) + 서비스(본인 확인, 진짜 검사)

| 구분 | 6주차 관리자 페이지 | 9주차 게시글 |
|---|---|---|
| 기준 | 역할 (ROLE_ADMIN) | 주인 (글쓴이) |
| 확인 위치 | URL 규칙 + `@PreAuthorize` | 서비스 코드 (글마다 주인이 다름) |
| 화면 | 관리자 메뉴 숨김 | 수정·삭제 버튼 숨김 |
| 실패 시 | 403 | 403 |

## 11. 테스트 표 (직접 확인 + `BoardTests.java` 자동 테스트)

| # | 계정 | 하는 일 | 기대 결과 |
|---|---|---|---|
| 1 | 로그인 안 함 | 메인 → 게시판 | 목록, 글쓰기 버튼 없음 |
| 2 | 로그인 안 함 | 주소창 `/board/write` | 로그인 화면 |
| 3 | student1 | 글쓰기 → 등록 | 상세 화면 |
| 4 | student1 | 제목 비우고 등록 | "제목과 내용을 모두 입력하세요." |
| 5 | 누구나 | `/board/99999` | 404 화면 |
| 6 | student1 | 내 글 수정 | 수정됨, "수정 시간" 표시 |
| 7 | student1 | 내 글 삭제 | 목록 + "글이 삭제되었습니다." |
| 8 | student3 | student1 글 열기 | 수정·삭제 버튼 없음 |
| 9 | student3 | 주소창 `/board/2/edit` | **403** |
| 10 | admin | student3 글 열기 → 관리자 삭제 | 삭제됨 |
| 11 | admin | student1 글 `/edit` | **403** (수정은 본인만) |

## 12. 오류 해결표

| 증상 | 원인 → 해결 |
|---|---|
| 게시판 목록이 로그인 화면으로 | `/board` permitAll 누락 |
| 글쓰기 저장 시 403 | 폼에 `th:action` 대신 `action` 사용 → CSRF 토큰 없음 (5주차) |
| `/board/write`가 누구에게나 열림 | `/board/**`로 너무 넓게 허용 → `{id:\d+}` 사용 |
| 글 제목 클릭 시 404 | 상세 미구현, 또는 없는 번호 |
| 남의 글이 수정됨 | `checkWriter()` 누락 |
| 글 쓴 회원을 관리자가 삭제하면 500 | 외래 키 때문 → 10주차에 해결, 지금은 그 회원의 글을 먼저 삭제 |
| (내 경우) 메인 화면 링크·회원목록 메뉴 숨김이 사라짐 | 제공 `index.html` 전체로 덮어씀 → 기존 파일에 게시판 메뉴 부분만 추가 |

## 13. 시험 대비 체크

- CRUD 각각의 SQL, JPA 메서드, 오늘의 주소를 말할 수 있는가?
- HTML 폼으로 수정·삭제를 POST로 하는 이유는? (폼은 GET·POST만)
- `@ManyToOne`인 이유와 `@JoinColumn(name = "member_id")`의 의미는?
- `FetchType.LAZY`란?
- PostForm에 author를 받지 않는 이유는? (작성자 바꿔치기 방지)
- `{id:\d+}`를 쓰는 이유는?
- `save()`가 INSERT/UPDATE를 구분하는 기준은? (id 유무)
- 버튼 숨김만으로 안 되는 이유와 본인 확인 위치는? (서비스, DB의 주인 확인)
- 수정과 삭제의 관리자 규칙 차이는?
- 깃에는 코드만 올라가고, 오늘 쓴 글(데이터)은 DB에 저장된다

---

다음 : 9주차 연습문제(조회수, 내가 쓴 글 `/board/my`) → 10주차 페이징 + 검색 + 댓글
