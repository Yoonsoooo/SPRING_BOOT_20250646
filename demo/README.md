# SPRING_BOOT_20250646

자바웹프로그래밍(2) 수업 실습 프로젝트입니다.

- GitHub 저장소: https://github.com/Yoonsoooo/SPRING_BOOT_20250646

## 2주차 - 개발환경 설정 및 테스트

- JDK 25와 Spring Boot 4.1.1 설정
- `DemoApplication` 실행 및 8080 포트 확인
- `/hello`, `/hello2` URL 매핑 구현
- Thymeleaf Model 속성 5개 출력
- [DemoController.java](src/main/java/com/example/demo/controller/DemoController.java)
- [hello.html](src/main/resources/templates/hello.html) / [hello2.html](src/main/resources/templates/hello2.html)

## 3주차 - 포트폴리오 프론트

- TemplateMo 578 First Portfolio의 600줄대 원본 전체 구조를 유지하여 Spring Boot 구조로 배치
- CSS, JavaScript, 이미지, 폰트를 `static` 폴더로 분리
- Thymeleaf 자원 경로 적용
- 한글 네비게이션과 개인 학습 소개 작성
- 웹, AI, 보안, 게임 기술 카드와 상세 페이지 작성
- PC/모바일 반응형 레이아웃 적용
- [포트폴리오 index.html](src/main/resources/templates/index.html)
- [웹](src/main/resources/public/detailed_web.html) / [AI](src/main/resources/public/detailed_ai.html) / [보안](src/main/resources/public/detailed_security.html) / [게임](src/main/resources/public/detailed_game.html) 상세 페이지

## 4주차 - 데이터베이스 연동 및 테스트

4주차 데이터베이스 수정 완료

- `pom.xml`에서 Spring Data JPA, MySQL 드라이버(`mysql-connector-j`) 주석 해제
- `application.properties`에 MySQL(`localhost:3306/spring`) 접속 정보 추가, DB 계정은 git에 올라가지 않는 `application-secret.properties`로 분리(5주차에 변경)
- 계층별 패키지 구조로 변경: `controller` / `model.domain` / `model.repository` / `model.service`
- 엔티티 [TestDB.java](src/main/java/com/example/demo/model/domain/TestDB.java) → 리포지토리 [TestRepository.java](src/main/java/com/example/demo/model/repository/TestRepository.java) → 서비스 [TestService.java](src/main/java/com/example/demo/model/service/TestService.java) → 컨트롤러 `/testdb`
- 연습문제: 엔티티에 나이(`age`), 성별(`gender`) 컬럼 추가, INSERT 후 [testdb.html](src/main/resources/templates/testdb.html)에서 표로 출력
- 데이터 입력 SQL: [sql/week4_testdb.sql](sql/week4_testdb.sql)
- 테스트는 H2 메모리 DB로 실행되어 MySQL 없이도 `mvnw test` 가능

### 로컬 DB 준비

1. MySQL 8 설치 (Server only, root / spring 계정 생성)
2. `create database spring;`
3. `src/main/resources/application-secret.properties` 파일을 만들고 DB 계정 2줄 작성

   ```properties
   spring.datasource.username=root
   spring.datasource.password=root비밀번호
   remember-me.key=아무거나_긴_비밀키
   ```
4. 서버 실행 → `testdb` 테이블 자동 생성 확인 → `sql/week4_testdb.sql`의 INSERT 실행
5. `http://localhost:8080/testdb` 접속

## 5주차 - 로그인/로그아웃 및 암호화

5주차 로그인/로그아웃, 암호화 완료

- `pom.xml`에 Spring Security, `thymeleaf-extras-springsecurity6` 의존성 추가
- 교수님 제공 [login.html](src/main/resources/templates/login.html), [signup.html](src/main/resources/templates/signup.html) 적용
- 직접 꾸민 포트폴리오라서 `index.html`은 전체 교체 대신 `index_네비게이션_교체부분.html`의 ①~④만 반영
  - `xmlns:sec` 선언, 로그인 사용자 표시 스타일, 모바일/PC 로그인·로그아웃 버튼, 메뉴에 `회원목록` 추가
- 새로 작성한 자바 파일 6개
  - [SecurityConfig.java](src/main/java/com/example/demo/config/SecurityConfig.java) : BCrypt 암호화 빈, URL 접근 규칙, 폼 로그인, 로그아웃
  - [MemberController.java](src/main/java/com/example/demo/controller/MemberController.java) : `/login`, `/signup` 화면과 회원가입 처리
  - [Member.java](src/main/java/com/example/demo/model/domain/Member.java) : `member` 테이블 엔티티(아이디 중복 불가, 권한 USER)
  - [MemberForm.java](src/main/java/com/example/demo/model/dto/MemberForm.java) : 회원가입 화면 데이터 DTO
  - [MemberRepository.java](src/main/java/com/example/demo/model/repository/MemberRepository.java) : `findByUsername`, `existsByUsername`
  - [MemberService.java](src/main/java/com/example/demo/model/service/MemberService.java) : 가입 시 비밀번호 BCrypt 암호화, `UserDetailsService`로 로그인 회원 조회
- 누구나 접근 : 메인, 로그인, 회원가입, 공개 실습/상세 페이지, CSS·JS·이미지
- 로그인 필요 : `/testdb`(회원목록) → 비로그인 시 로그인 화면으로 이동, 로그인 후 원래 페이지로 복귀
- 로그아웃은 CSRF 보호 때문에 POST 폼으로만 동작하고, 세션과 `JSESSIONID` 쿠키를 삭제
- DB 비밀번호가 GitHub에 노출되지 않도록 `application-secret.properties`로 분리하고 `.gitignore`에 등록
- 테스트 : 가입 시 비밀번호가 `$2a$10$...` 60자 해시로 저장되는지, 로그인 성공/실패, 로그아웃, CSRF 차단 확인 ([MemberSecurityTests.java](src/test/java/com/example/demo/MemberSecurityTests.java))

### 5주차 연습문제 - 로그인 기능 개선하기

- ① 로그인 상태 유지(remember-me)
  - `SecurityConfig` 로그아웃 설정 뒤에 `.rememberMe()` 추가 : `key(비밀키)`, `tokenValiditySeconds(7일)`
  - `login.html`의 `로그인 상태 유지` 체크박스(`name="remember-me"`)를 체크하고 로그인하면 7일짜리 `remember-me` 쿠키 발급
  - 브라우저를 닫아 `JSESSIONID`가 사라져도 `remember-me` 쿠키로 자동 로그인
  - 비밀키는 공개 저장소에 노출되지 않도록 `application-secret.properties`의 `remember-me.key`에 저장
- ② 비밀번호 확인 검증
  - `MemberService.signup()`에서 `password`와 `passwordConfirm` 비교
  - 다르면 "비밀번호가 일치하지 않습니다." 출력, 입력한 아이디·이름은 유지, DB에는 저장하지 않음

## 6주차 - 권한 관리 및 관리자 페이지

6주차 권한(ROLE) 접근 제어, 관리자 페이지 분리 완료

- 교수님 제공 화면 적용 : [mypage.html](src/main/resources/templates/mypage.html), [admin/members.html](src/main/resources/templates/admin/members.html), [error/403.html](src/main/resources/templates/error/403.html)
- 직접 꾸민 포트폴리오라서 `index.html`은 `index_네비게이션_교체부분.html`의 ①~③만 반영
  - 권한 배지 스타일, `내 정보` 메뉴(로그인 사용자), 빨간 `관리자` 메뉴(ADMIN만), 아이디 옆 권한 배지
- 자바 파일 4개
  - [AdminController.java](src/main/java/com/example/demo/controller/AdminController.java) (신규) : `/admin/members` 회원 목록, 권한 변경, 회원 삭제 (`@RequestMapping("/admin")`, PRG 패턴)
  - [SecurityConfig.java](src/main/java/com/example/demo/config/SecurityConfig.java) : `/admin/**` → `hasRole("ADMIN")`, `@EnableMethodSecurity`
  - [MemberController.java](src/main/java/com/example/demo/controller/MemberController.java) : `/mypage` (`Principal`로 로그인한 내 정보만 조회 → IDOR 차단)
  - [MemberService.java](src/main/java/com/example/demo/model/service/MemberService.java) : `findByUsername`, `findAll`, `changeRole`(ROLES 화이트리스트), `deleteMember`, `@PreAuthorize("hasRole('ADMIN')")`
- 관리자 계정 : `/signup`에서 `admin / 1234` 가입 → 제공 SQL(`UPDATE member SET role = 'ADMIN' WHERE username = 'admin'`)로 권한 부여 → 다시 로그인 (실습용 계정)
- 접근 제어 3단계 : ① 화면 `sec:authorize`(숨기기만) ② URL `requestMatchers`(1차 잠금) ③ 메서드 `@PreAuthorize`(2차 잠금)
- 결과 : 비로그인 `/admin/members` → 302 로그인 화면, USER → 403 접근 거부 화면, ADMIN → 200 회원 관리
- 테스트 : [AdminSecurityTests.java](src/test/java/com/example/demo/AdminSecurityTests.java) - 302/403/200, USER의 권한 상승 차단, 서비스 직접 호출도 `@PreAuthorize`로 차단(이중 잠금), 허용되지 않는 권한 거부, 메뉴·배지 표시

### 6주차 연습문제 - 권한 관리 개선하기

- ① 관리자 본인 보호
  - `MemberService.changeRole()`, `deleteMember()`에서 `SecurityContextHolder`로 로그인 아이디를 꺼내 대상이 본인이면 거부
  - 관리자가 자기 자신을 USER로 바꾸거나 삭제하면 "자기 자신의 권한 변경·삭제는 할 수 없습니다." 출력 → 관리자 0명 방지
- ② 매니저(MANAGER) 권한 추가
  - `ROLES` 화이트리스트에 `MANAGER` 추가 → 회원 관리에서 MANAGER 부여 가능
  - `SecurityConfig`에 `.requestMatchers("/testdb").hasAnyRole("ADMIN", "MANAGER")` 추가

  | 주소 | USER | MANAGER | ADMIN |
  |---|---|---|---|
  | `/mypage` | ✓ | ✓ | ✓ |
  | `/testdb` (회원목록) | 403 | ✓ | ✓ |
  | `/admin/**` (회원 관리) | 403 | 403 | ✓ |
- 추가 도전 : `index.html`의 회원목록 메뉴에 `sec:authorize="hasAnyRole('ADMIN', 'MANAGER')"` → USER에게는 메뉴도 숨김

## 7주차 - REST API와 JSON

7주차 회원 관리 REST API(JSON) 완료

- 교수님 제공 화면 적용 : [admin/members-api.html](src/main/resources/templates/admin/members-api.html) (fetch 로 API 호출, 오른쪽 기록창), [admin/members.html](src/main/resources/templates/admin/members.html)에 "API 버전으로 보기" 링크
- 자바 파일
  - [MemberApiController.java](src/main/java/com/example/demo/controller/MemberApiController.java) (신규) : `GET /api/hello`(누구나), `GET /api/me`(로그인한 내 정보)
  - [MemberResponse.java](src/main/java/com/example/demo/model/dto/MemberResponse.java) (신규) : 응답 DTO(record) → id, username, name, role 만 내보내고 password(해시)는 숨김
  - [RoleRequest.java](src/main/java/com/example/demo/model/dto/RoleRequest.java) (신규) : 권한 변경 요청 본문 `{"role": "ADMIN"}`
  - [AdminApiController.java](src/main/java/com/example/demo/controller/AdminApiController.java) (신규) : `/api/admin/members` 목록·1명 조회·등록·권한 변경·삭제 (`ResponseEntity`로 상태 코드 지정)
  - [SecurityConfig.java](src/main/java/com/example/demo/config/SecurityConfig.java) : `/api/hello` 허용, `/api/admin/**` ADMIN 전용, `httpBasic()`, `/api/**` CSRF 제외, 로그인 안 한 API 요청은 302 대신 401
  - [MemberService.java](src/main/java/com/example/demo/model/service/MemberService.java) : `findById`, 회원가입 빈 값 검사, `changeRole()`이 바뀐 회원을 반환
  - [AdminController.java](src/main/java/com/example/demo/controller/AdminController.java) : `/admin/members-api` 화면
- 인증 : 브라우저는 로그인 세션 쿠키, Postman은 요청마다 Basic 인증(아이디:비밀번호) → 같은 API, 같은 결과

  | 메서드 · 주소 | 하는 일 | 결과 |
  |---|---|---|
  | `GET /api/admin/members` | 회원 목록 | 200 (비밀번호 없음), USER → 403 |
  | `GET /api/admin/members/{id}` | 회원 1명 | 200, 없으면 404 |
  | `POST /api/admin/members` | 회원 등록 | 201 + Location, 중복·빈 값 400, `role` 몰래 넣어도 USER |
  | `PATCH /api/admin/members/{id}/role` | 권한 변경 | 200, `SUPER` 400, 본인 변경 400 |
  | `DELETE /api/admin/members/{id}` | 회원 삭제 | 204 |
- 테스트 : [RestApiTests.java](src/test/java/com/example/demo/RestApiTests.java) - Basic 인증으로 401/200/201/204/400/403/404, 응답에 password 없음, 제공 화면 연결
  - `httpBasic()` 추가 후 Accept 헤더 없는 요청은 401이 되므로, 기존 302 테스트는 브라우저처럼 `Accept: text/html`을 보내도록 수정

### 7주차 연습문제 - REST API 개선하기

- ① 회원 검색 API : `GET /api/admin/members?keyword=student`
  - `@RequestParam(required = false) String keyword` → 검색어가 없으면 전체 목록, 있으면 검색
  - [MemberRepository.java](src/main/java/com/example/demo/model/repository/MemberRepository.java) : `findByUsernameContainingOrNameContainingOrderByIdAsc` → 아이디나 이름에 검색어가 들어간 회원만
  - 확인 : 제공 화면(`/admin/members-api`) 검색창에 `student` → student1 만 출력
- ② 오류 처리 한 곳에 모으기 : [ApiExceptionHandler.java](src/main/java/com/example/demo/controller/ApiExceptionHandler.java) (신규)
  - `@RestControllerAdvice(annotations = RestController.class)` → JSON API 컨트롤러에만 적용
  - `IllegalArgumentException` → 400 `{"error": "..."}` : 등록·권한 변경·삭제의 반복되던 try-catch 제거, 지운 뒤에도 상태 코드·본문 동일
  - JSON 형식 오류(`HttpMessageNotReadableException`) → 400 `{"error": "요청 JSON 형식이 올바르지 않습니다."}` : trace(서버 내부 정보) 숨김
- 테스트 : [RestApiTests.java](src/test/java/com/example/demo/RestApiTests.java) - 검색 결과, 서비스 오류 400, 잘못된 JSON에 trace 없음

## Lighthouse 성능 점검

- 2026-09-16 로컬 서버 측정: 데스크톱 96점, 모바일 60점
- 가장 큰 저하 원인 1: 미사용 CSS 약 292 KiB(모바일 예상 절감 1.23초)
- 가장 큰 저하 원인 2: 미사용 JavaScript 약 85 KiB(모바일 예상 절감 0.40초)
- 원인: 원본 TemplateMo가 Bootstrap, Bootstrap Icons, Magnific Popup, jQuery 플러그인 전체를 불러오지만 현재 화면은 그중 일부만 사용합니다.
- 첫 화면 아래의 이미지는 `loading="lazy"`로 지연 로딩했습니다.
- Lighthouse 점수는 PC 상태와 네트워크 환경에 따라 달라질 수 있습니다.

## 실행

```powershell
.\mvnw.cmd spring-boot:run
```

브라우저에서 `http://localhost:8080`으로 접속합니다.

VS Code를 다시 연면 프로젝트 설정이 JDK 25를 새 터미널의 `JAVA_HOME`으로 지정합니다. 이미 열려 있던 터미널이라면 다음처럼 현재 세션만 맞춘 뒤 실행합니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4'
.\mvnw.cmd test
```
