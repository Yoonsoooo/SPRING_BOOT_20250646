package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.domain.Member;
import com.example.demo.model.repository.MemberRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// 7주차 REST API(JSON) 테스트 : Postman 처럼 Basic 인증으로 호출 (테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class RestApiTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	Member student1;
	Member admin;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		student1 = memberRepository.save(member("student1", "홍길동", "USER"));
		admin = memberRepository.save(member("admin", "관리자", "ADMIN"));
	}

	// 단계 1 : /api/hello 는 누구나 JSON
	@Test
	void helloIsPublicJson() throws Exception {
		mockMvc.perform(get("/api/hello"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("안녕하세요, REST API!"));
	}

	// 단계 2·3·4 : /api/me 는 인증 필요, 응답에 password 없음
	@Test
	void meWithoutAuthIs401() throws Exception {
		mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void meWithWrongPasswordIs401() throws Exception {
		mockMvc.perform(get("/api/me").with(httpBasic("student1", "wrong")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meReturnsMemberResponseWithoutPassword() throws Exception {
		mockMvc.perform(get("/api/me").with(httpBasic("student1", "1234")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("student1"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	// 단계 5 : 조회
	@Test
	void adminListsMembersWithoutPassword() throws Exception {
		mockMvc.perform(get("/api/admin/members").with(httpBasic("admin", "1234")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(content().string(not(containsString("password"))));
	}

	@Test
	void adminGetsOneMemberOr404() throws Exception {
		mockMvc.perform(get("/api/admin/members/{id}", student1.getId()).with(httpBasic("admin", "1234")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("student1"));
		mockMvc.perform(get("/api/admin/members/{id}", 9999L).with(httpBasic("admin", "1234")))
				.andExpect(status().isNotFound());
	}

	@Test
	void userCallingAdminApiIs403() throws Exception {
		mockMvc.perform(get("/api/admin/members").with(httpBasic("student1", "1234")))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/admin/members/{id}", admin.getId()).with(httpBasic("student1", "1234")))
				.andExpect(status().isForbidden());
		assertTrue(memberRepository.existsById(admin.getId()));
	}

	// 단계 6 : 등록
	@Test
	void adminCreatesMember201AndRoleCannotBeInjected() throws Exception {
		String body = """
				{"username":"postman1","password":"1234","passwordConfirm":"1234","name":"포스트맨","role":"ADMIN"}""";
		mockMvc.perform(post("/api/admin/members").with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/api/admin/members/")))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());

		Member saved = memberRepository.findByUsername("postman1").orElseThrow();
		assertTrue(saved.getPassword().startsWith("$2a$10$"));

		mockMvc.perform(post("/api/admin/members").with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("이미 사용 중인 아이디입니다."));
	}

	@Test
	void createWithMissingFieldsIs400() throws Exception {
		mockMvc.perform(post("/api/admin/members").with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("아이디, 비밀번호, 이름은 필수입니다."));
	}

	// 단계 7 : 수정 · 삭제 (슬라이드 기대 결과 표)
	@Test
	void changeRoleResults() throws Exception {
		mockMvc.perform(patch("/api/admin/members/{id}/role", student1.getId()).with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
		mockMvc.perform(patch("/api/admin/members/{id}/role", student1.getId()).with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPER\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(patch("/api/admin/members/{id}/role", admin.getId()).with(httpBasic("admin", "1234"))
						.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
				.andExpect(status().isBadRequest());
		assertEquals("ADMIN", memberRepository.findById(admin.getId()).orElseThrow().getRole());
	}

	@Test
	void adminDeletesMember204() throws Exception {
		mockMvc.perform(delete("/api/admin/members/{id}", student1.getId()).with(httpBasic("admin", "1234")))
				.andExpect(status().isNoContent());
		assertTrue(memberRepository.findByUsername("student1").isEmpty());
	}

	// 단계 8 : 제공 화면
	@Test
	void membersApiPageIsServedToAdmin() throws Exception {
		mockMvc.perform(get("/admin/members-api").with(httpBasic("admin", "1234")))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/members-api"));
	}

	private Member member(String username, String name, String role) {
		Member member = new Member();
		member.setUsername(username);
		member.setPassword(passwordEncoder.encode("1234"));
		member.setName(name);
		member.setRole(role);
		return member;
	}
}
