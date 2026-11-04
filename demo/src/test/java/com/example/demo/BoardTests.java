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

import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.repository.PostRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 9주차 게시판 CRUD와 본인 확인 테스트 (슬라이드 16·17·20·23의 테스트 표, 테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class BoardTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PostRepository postRepository;

	Member student1;
	Member student3;
	Member admin;

	@BeforeEach
	void setUp() {
		postRepository.deleteAll(); // 글이 회원을 FK 로 참조하므로 글부터 삭제
		memberRepository.deleteAll();
		student1 = memberRepository.save(member("student1", "홍길동", "USER"));
		student3 = memberRepository.save(member("student3", "김철수", "USER"));
		admin = memberRepository.save(member("admin", "관리자", "ADMIN"));
	}

	@AfterEach
	void tearDown() { // 다른 테스트의 memberRepository.deleteAll() 이 FK 때문에 실패하지 않도록
		postRepository.deleteAll();
	}

	// 슬라이드 16 ① : 로그인 안 함 → 목록은 보이고 글쓰기 버튼은 없음
	@Test
	void anonymousCanSeeListWithoutWriteButton() throws Exception {
		savePost("첫 글", student1);
		mockMvc.perform(get("/board"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("첫 글")))
				.andExpect(content().string(containsString("홍길동"))) // post.author.name
				.andExpect(content().string(not(containsString("/board/write"))));
	}

	// 슬라이드 16 ② : 로그인 안 함 → /board/write 는 로그인 화면으로
	@Test
	void anonymousWriteRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/board/write").accept(MediaType.TEXT_HTML))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	// 슬라이드 16 ③ : student1 글쓰기 → 상세 화면으로, 작성자 = 로그인한 사람
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void writeSavesLoginUserAsAuthor() throws Exception {
		mockMvc.perform(post("/board/write").with(csrf())
				.param("title", "  안녕하세요  ").param("content", "첫 글입니다."))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/board/*"));
		Post saved = postRepository.findAll().get(0);
		assertEquals("안녕하세요", saved.getTitle()); // trim
		assertEquals(student1.getId(), saved.getAuthor().getId()); // LAZY : id 는 프록시에서 바로 꺼낼 수 있다
		assertNotNull(saved.getCreatedAt());
	}

	// 슬라이드 16 ④ : 제목 비우고 등록 → 오류 메시지, 저장 안 됨
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void writeWithBlankTitleShowsError() throws Exception {
		mockMvc.perform(post("/board/write").with(csrf()).param("title", " ").param("content", "내용"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("제목과 내용을 모두 입력하세요.")));
		assertEquals(0, postRepository.count());
	}

	// 슬라이드 24 : th:action 대신 action 사용 → CSRF 토큰이 없으면 403
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void writeWithoutCsrfIsForbidden() throws Exception {
		mockMvc.perform(post("/board/write").param("title", "제목").param("content", "내용"))
				.andExpect(status().isForbidden());
	}

	// 슬라이드 17 : 상세는 누구나, 없는 글은 404 화면
	@Test
	void detailIsPublicAndMissingPostIs404() throws Exception {
		Post post = savePost("상세 글", student1);
		mockMvc.perform(get("/board/" + post.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("상세 글")));
		mockMvc.perform(get("/board/99999").accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound());
	}

	// 슬라이드 23 ② : student1 내 글 수정 → 수정됨, 수정 시간 기록
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void writerCanEditOwnPost() throws Exception {
		Post post = savePost("원래 제목", student1);
		mockMvc.perform(get("/board/" + post.getId() + "/edit"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("원래 제목")));
		mockMvc.perform(post("/board/" + post.getId() + "/edit").with(csrf())
				.param("title", "바꾼 제목").param("content", "바꾼 내용"))
				.andExpect(redirectedUrl("/board/" + post.getId()));
		Post edited = postRepository.findById(post.getId()).orElseThrow();
		assertEquals("바꾼 제목", edited.getTitle());
		assertNotNull(edited.getUpdatedAt());
	}

	// 슬라이드 20 ② : student1 내 글 삭제 → 목록 + "삭제되었습니다"
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void writerCanDeleteOwnPost() throws Exception {
		Post post = savePost("지울 글", student1);
		mockMvc.perform(post("/board/" + post.getId() + "/delete").with(csrf()))
				.andExpect(redirectedUrl("/board?deleted"));
		assertFalse(postRepository.existsById(post.getId()));
	}

	// 슬라이드 23 ① : student3 이 남의 글 /edit → 403, 저장 시도도 403 이고 글은 그대로
	@Test
	@WithMockUser(username = "student3", roles = "USER")
	void otherUserCannotEditOrDelete() throws Exception {
		Post post = savePost("student1 의 글", student1);
		mockMvc.perform(get("/board/" + post.getId() + "/edit"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/board/" + post.getId() + "/edit").with(csrf())
				.param("title", "몰래 수정").param("content", "몰래"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/board/" + post.getId() + "/delete").with(csrf()))
				.andExpect(status().isForbidden());
		assertEquals("student1 의 글", postRepository.findById(post.getId()).orElseThrow().getTitle());
	}

	// 슬라이드 20 ③ : 남의 글 상세에는 수정·삭제 버튼이 없음
	@Test
	@WithMockUser(username = "student3", roles = "USER")
	void otherUserSeesNoEditButtons() throws Exception {
		Post post = savePost("student1 의 글", student1);
		mockMvc.perform(get("/board/" + post.getId()))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("/edit"))))
				.andExpect(content().string(not(containsString("/delete"))));
	}

	// 슬라이드 23 ③·④·⑤ : 관리자는 남의 글 삭제 가능, 수정은 403
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanDeleteButCannotEditOthersPost() throws Exception {
		Post post = savePost("student3 의 글", student3);
		mockMvc.perform(get("/board/" + post.getId()))
				.andExpect(content().string(containsString("관리자 삭제")));
		mockMvc.perform(get("/board/" + post.getId() + "/edit"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/board/" + post.getId() + "/delete").with(csrf()))
				.andExpect(redirectedUrl("/board?deleted"));
		assertFalse(postRepository.existsById(post.getId()));
	}

	private Post savePost(String title, Member author) {
		Post post = new Post();
		post.setTitle(title);
		post.setContent("내용");
		post.setAuthor(author);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
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
