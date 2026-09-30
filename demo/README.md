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
