package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.domain.Comment;
import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;
import com.example.demo.model.repository.CommentRepository;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.repository.PostRepository;
import com.example.demo.model.service.MemberService;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 10주차 페이징·검색·댓글 테스트 (단계 1~6 + 연습문제 확인 표, 테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class PagingCommentTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PostRepository postRepository;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	MemberService memberService;

	Member student1;
	Member student3;
	Member admin;

	@BeforeEach
	void setUp() {
		commentRepository.deleteAll(); // FK 순서 : 댓글 → 글 → 회원
		postRepository.deleteAll();
		memberRepository.deleteAll();
		student1 = memberRepository.save(member("student1", "홍길동", "USER"));
		student3 = memberRepository.save(member("student3", "김철수", "USER"));
		admin = memberRepository.save(member("admin", "관리자", "ADMIN"));
	}

	@AfterEach
	void tearDown() { // 다른 테스트의 memberRepository.deleteAll() 이 FK 때문에 실패하지 않도록
		commentRepository.deleteAll();
		postRepository.deleteAll();
	}

	// 단계 1 ① : 글 25개 → 첫 화면은 최신 10개 + "1 / 3 페이지"
	@Test
	void firstPageShowsNewestTenPosts() throws Exception {
		savePosts(25);
		mockMvc.perform(get("/board"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("전체 25개")))
				.andExpect(content().string(containsString("1 / 3 페이지")))
				.andExpect(content().string(containsString(">테스트 글 25<")))
				.andExpect(content().string(containsString(">테스트 글 16<")))
				.andExpect(content().string(not(containsString(">테스트 글 15<"))));
	}

	// 단계 1 ② : 2페이지 → 다음 10개 (/board?page=1)
	@Test
	void secondPageShowsNextTenPosts() throws Exception {
		savePosts(25);
		mockMvc.perform(get("/board").param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("2 / 3 페이지")))
				.andExpect(content().string(containsString(">테스트 글 15<")))
				.andExpect(content().string(not(containsString(">테스트 글 16<"))));
	}

	// 단계 1 ④·⑤ : page=-1 은 첫 페이지로, page=999 는 빈 목록 (오류 없음)
	@Test
	void wrongPageNumberDoesNotFail() throws Exception {
		savePosts(25);
		mockMvc.perform(get("/board").param("page", "-1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("1 / 3 페이지")));
		mockMvc.perform(get("/board").param("page", "999"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("아직 글이 없습니다")));
	}

	// 단계 2 ① : 제목 또는 내용에 검색어가 들어간 글만, 검색창에 검색어 유지
	@Test
	void searchFindsTitleOrContent() throws Exception {
		savePost("스프링 스터디 모집", "내용", student1);
		savePost("일반 글", "이번 주 스프링 스터디 공지", student1);
		savePost("관계없는 글", "내용", student1);
		mockMvc.perform(get("/board").param("keyword", "스터디"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("검색 결과")))
				.andExpect(content().string(containsString("value=\"스터디\"")))
				.andExpect(content().string(containsString("스프링 스터디 모집")))
				.andExpect(content().string(containsString("일반 글")))
				.andExpect(content().string(not(containsString("관계없는 글"))));
	}

	// 단계 2 ② : 검색 결과도 10개씩 나뉘고, 페이지 링크에 keyword 가 붙는다
	@Test
	void searchResultIsPagedWithKeywordInLinks() throws Exception {
		savePosts(13, "스터디 글 ");
		savePosts(5);
		mockMvc.perform(get("/board").param("keyword", "스터디"))
				.andExpect(content().string(containsString("1 / 2 페이지")))
				.andExpect(content().string(containsString("keyword=")));
	}

	// 단계 2 ③·⑤ : 결과 없음 안내, ' OR 1=1 -- 는 그냥 글자로 검색 → 0개 (SQL 인젝션 방지)
	@Test
	void searchWithNoResultAndSqlInjection() throws Exception {
		savePosts(5);
		mockMvc.perform(get("/board").param("keyword", "없는검색어"))
				.andExpect(content().string(containsString("검색 결과가 없습니다.")));
		mockMvc.perform(get("/board").param("keyword", "' OR 1=1 --"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("검색 결과가 없습니다.")));
	}

	// 단계 3 ① : 로그인 안 함 → 댓글은 보이고 쓰기 칸 대신 로그인 안내
	@Test
	void anonymousSeesCommentsButNoForm() throws Exception {
		Post post = savePost("글", "내용", student1);
		saveComment(post, student1, "첫 댓글");
		mockMvc.perform(get("/board/" + post.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("첫 댓글")))
				.andExpect(content().string(containsString("하면 댓글을 쓸 수 있습니다.")))
				.andExpect(content().string(not(containsString("<textarea name=\"content\""))));
	}

	// 단계 3 : 로그인 안 하고 댓글 POST → 로그인 화면
	@Test
	void anonymousCannotWriteComment() throws Exception {
		Post post = savePost("글", "내용", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/comments").with(csrf())
				.param("content", "몰래 댓글").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
		assertEquals(0, commentRepository.count());
	}

	// 단계 3 ② : student3 이 student1 의 글에 댓글 → 작성자 = student3, 상세로 redirect (PRG)
	@Test
	@WithMockUser(username = "student3", roles = "USER")
	void loginUserCanWriteComment() throws Exception {
		Post post = savePost("student1 의 글", "내용", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/comments").with(csrf())
				.param("content", "  좋은 글이네요  "))
				.andExpect(redirectedUrl("/board/" + post.getId() + "#comments"));
		Comment saved = commentRepository.findAll().get(0);
		assertEquals("좋은 글이네요", saved.getContent());
		assertEquals("student3", memberRepository.findById(saved.getAuthor().getId()).orElseThrow().getUsername());
	}

	// 단계 3 ④ : 빈 댓글·500자 초과 → ?commentError, 저장 안 됨
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void blankOrTooLongCommentIsRejected() throws Exception {
		Post post = savePost("글", "내용", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/comments").with(csrf()).param("content", "  "))
				.andExpect(redirectedUrl("/board/" + post.getId() + "?commentError#comments"));
		mockMvc.perform(post("/board/" + post.getId() + "/comments").with(csrf()).param("content", "가".repeat(501)))
				.andExpect(redirectedUrl("/board/" + post.getId() + "?commentError#comments"));
		assertEquals(0, commentRepository.count());
	}

	// 단계 3 : CSRF 토큰 없이 댓글 POST → 403
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void commentWithoutCsrfIsForbidden() throws Exception {
		Post post = savePost("글", "내용", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/comments").param("content", "댓글"))
				.andExpect(status().isForbidden());
	}

	// 단계 4 ① : student1 이 student3 의 댓글 삭제 → 403, 댓글은 그대로
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void otherUserCannotDeleteComment() throws Exception {
		Post post = savePost("글", "내용", student1);
		Comment comment = saveComment(post, student3, "student3 의 댓글");
		mockMvc.perform(post("/board/" + post.getId() + "/comments/" + comment.getId() + "/delete").with(csrf()))
				.andExpect(status().isForbidden());
		assertTrue(commentRepository.existsById(comment.getId()));
	}

	// 단계 4 ② : 본인 댓글 삭제 → ?commentDeleted
	@Test
	@WithMockUser(username = "student3", roles = "USER")
	void writerCanDeleteOwnComment() throws Exception {
		Post post = savePost("글", "내용", student1);
		Comment comment = saveComment(post, student3, "내 댓글");
		mockMvc.perform(post("/board/" + post.getId() + "/comments/" + comment.getId() + "/delete").with(csrf()))
				.andExpect(redirectedUrl("/board/" + post.getId() + "?commentDeleted#comments"));
		assertFalse(commentRepository.existsById(comment.getId()));
	}

	// 단계 4 ③ : 관리자는 남의 댓글 삭제 가능 (화면에 "관리자 삭제")
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanDeleteOthersComment() throws Exception {
		Post post = savePost("글", "내용", student1);
		Comment comment = saveComment(post, student3, "student3 의 댓글");
		mockMvc.perform(get("/board/" + post.getId()))
				.andExpect(content().string(containsString("관리자 삭제")));
		mockMvc.perform(post("/board/" + post.getId() + "/comments/" + comment.getId() + "/delete").with(csrf()))
				.andExpect(redirectedUrl("/board/" + post.getId() + "?commentDeleted#comments"));
		assertFalse(commentRepository.existsById(comment.getId()));
	}

	// 단계 4 : 없는 댓글 삭제 → 404
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void deletingMissingCommentIs404() throws Exception {
		Post post = savePost("글", "내용", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/comments/99999/delete").with(csrf())
				.accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound());
	}

	// 단계 5 ① : 목록 제목 옆에 댓글 수 [2]
	@Test
	void listShowsCommentCount() throws Exception {
		Post post = savePost("댓글 많은 글", "내용", student1);
		saveComment(post, student1, "하나");
		saveComment(post, student3, "둘");
		mockMvc.perform(get("/board"))
				.andExpect(content().string(containsString("[2]")));
	}

	// 단계 5 ② : 댓글이 있는 글도 삭제 가능 (cascade) → 댓글도 함께 삭제, 500 아님
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void deletingPostAlsoDeletesComments() throws Exception {
		Post post = savePost("댓글 달린 글", "내용", student1);
		saveComment(post, student3, "댓글");
		mockMvc.perform(post("/board/" + post.getId() + "/delete").with(csrf()))
				.andExpect(redirectedUrl("/board?deleted"));
		assertFalse(postRepository.existsById(post.getId()));
		assertEquals(0, commentRepository.count());
	}

	// 단계 6 : 글이나 댓글이 있는 회원은 삭제 안 됨 (500 대신 친절한 오류), 없는 회원은 삭제됨
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void memberWithPostsOrCommentsCannotBeDeleted() {
		Post post = savePost("글", "내용", student1);
		saveComment(post, student3, "댓글");
		assertThrows(IllegalArgumentException.class, () -> memberService.deleteMember(student1.getId())); // 글 있음
		assertThrows(IllegalArgumentException.class, () -> memberService.deleteMember(student3.getId())); // 댓글 있음
		Member writer10 = memberRepository.save(member("writer10", "글없음", "USER"));
		memberService.deleteMember(writer10.getId());
		assertFalse(memberRepository.existsById(writer10.getId()));
	}

	// [연습문제 1] 내가 쓴 글도 10개씩 + 페이지 번호, 링크는 /board/my
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void myPostsArePaged() throws Exception {
		savePosts(15); // student1 의 글 15개
		savePost("student3 의 글", "내용", student3);
		mockMvc.perform(get("/board/my"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("내가 쓴 글")))
				.andExpect(content().string(containsString("전체 15개")))
				.andExpect(content().string(containsString("1 / 2 페이지")))
				.andExpect(content().string(containsString("/board/my?page=1")))
				.andExpect(content().string(containsString(">테스트 글 15<")))
				.andExpect(content().string(not(containsString(">테스트 글 5<"))))
				.andExpect(content().string(not(containsString("student3 의 글"))));
		mockMvc.perform(get("/board/my").param("page", "1"))
				.andExpect(content().string(containsString("2 / 2 페이지")))
				.andExpect(content().string(containsString(">테스트 글 5<")))
				.andExpect(content().string(not(containsString(">테스트 글 15<"))));
	}

	// [연습문제 2] ① 범위 "작성자" + "관리" → 관리자가 쓴 글만, 선택 상자 유지
	@Test
	void searchByWriterName() throws Exception {
		savePost("홍길동 글", "관리 방법", student1); // 내용에 "관리"가 있어도 작성자 검색에는 안 걸린다
		savePost("관리자 공지", "내용", admin);
		mockMvc.perform(get("/board").param("type", "writer").param("keyword", "관리"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("관리자 공지")))
				.andExpect(content().string(not(containsString("홍길동 글"))))
				.andExpect(content().string(containsString("<option value=\"writer\" selected=\"selected\">")));
		mockMvc.perform(get("/board").param("type", "all").param("keyword", "관리"))
				.andExpect(content().string(containsString("홍길동 글")))
				.andExpect(content().string(containsString("관리자 공지")));
	}

	// [연습문제 2] ② 작성자 검색 결과 2페이지로 넘어가도 type·keyword 유지
	@Test
	void writerSearchKeepsTypeAcrossPages() throws Exception {
		for (int i = 1; i <= 12; i++) {
			savePost("공지 " + i, "내용", admin);
		}
		savePosts(3);
		mockMvc.perform(get("/board").param("type", "writer").param("keyword", "관리자"))
				.andExpect(content().string(containsString("1 / 2 페이지")))
				.andExpect(content().string(containsString("type=writer")));
		mockMvc.perform(get("/board").param("type", "writer").param("keyword", "관리자").param("page", "1"))
				.andExpect(content().string(containsString("2 / 2 페이지")))
				.andExpect(content().string(not(containsString(">테스트 글"))));
	}

	private void savePosts(int count) {
		savePosts(count, "테스트 글 ");
	}

	private void savePosts(int count, String prefix) {
		for (int i = 1; i <= count; i++) {
			savePost(prefix + i, "페이징 확인용 " + i + "번째 글입니다.", student1);
		}
	}

	private Post savePost(String title, String content, Member author) {
		Post post = new Post();
		post.setTitle(title);
		post.setContent(content);
		post.setAuthor(author);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
	}

	private Comment saveComment(Post post, Member author, String content) {
		Comment comment = new Comment();
		comment.setPost(post);
		comment.setAuthor(author);
		comment.setContent(content);
		comment.setCreatedAt(LocalDateTime.now());
		return commentRepository.save(comment);
	}

	private Member member(String username, String name, String role) {
		Member member = new Member();
		member.setUsername(username);
		member.setPassword("$2a$10$abcdefghijklmnopqrstuuJ0t2ZbH0Yt7oD8Yp6q1nC1i6Gm8lQ5e");
		member.setName(name);
		member.setRole(role);
		return member;
	}
}
