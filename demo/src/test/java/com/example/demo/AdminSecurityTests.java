package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.domain.Member;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.service.MemberService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// 6주차 권한(ROLE) 접근 제어, 관리자 페이지 테스트 (테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	MemberService memberService;

	Member student1;
	Member admin;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
		student1 = memberRepository.save(member("student1", "홍길동", "USER"));
		admin = memberRepository.save(member("admin", "관리자", "ADMIN"));
	}

	// 슬라이드 22 : 세 가지 상태로 /admin/members 접속
	@Test
	void adminPageRedirectsAnonymousToLogin() throws Exception {
		mockMvc.perform(get("/admin/members"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void adminPageIsForbiddenForUserAndShows403Page() throws Exception {
		mockMvc.perform(get("/admin/members"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminPageListsMembersForAdmin() throws Exception {
		mockMvc.perform(get("/admin/members"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/members"))
				.andExpect(content().string(containsString("student1")))
				.andExpect(content().string(containsString("회원 관리")));
	}

	// 슬라이드 19 의 공격(USER 가 자기 권한을 ADMIN 으로) : URL 규칙으로 차단
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void userCannotPromoteThemselfThroughAdminUrl() throws Exception {
		mockMvc.perform(post("/admin/members/{id}/role", student1.getId()).with(csrf()).param("role", "ADMIN"))
				.andExpect(status().isForbidden());

		assertEquals("USER", memberRepository.findById(student1.getId()).orElseThrow().getRole());
	}

	// 슬라이드 25 이중 잠금 : URL 규칙을 거치지 않고 서비스를 직접 호출해도 @PreAuthorize 가 막는다.
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void preAuthorizeBlocksUserEvenWhenServiceIsCalledDirectly() {
		assertThrows(AuthorizationDeniedException.class,
				() -> memberService.changeRole(student1.getId(), "ADMIN"));
		assertThrows(AuthorizationDeniedException.class,
				() -> memberService.deleteMember(admin.getId()));

		assertEquals("USER", memberRepository.findById(student1.getId()).orElseThrow().getRole());
		assertTrue(memberRepository.existsById(admin.getId()));
	}

	// 슬라이드 18 : 관리자가 권한 변경 → 성공 메시지
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanChangeRole() throws Exception {
		mockMvc.perform(post("/admin/members/{id}/role", student1.getId()).with(csrf()).param("role", "ADMIN"))
				.andExpect(redirectedUrl("/admin/members"))
				.andExpect(flash().attribute("message", "권한을 ADMIN (으)로 변경했습니다."));

		assertEquals("ADMIN", memberRepository.findById(student1.getId()).orElseThrow().getRole());
	}

	// 슬라이드 16 ROLES 화이트리스트 : 허용되지 않은 권한(MANAGER, SUPER)은 거부
	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void roleOutsideWhitelistIsRejected() throws Exception {
		for (String role : new String[] {"MANAGER", "SUPER"}) {
			mockMvc.perform(post("/admin/members/{id}/role", student1.getId()).with(csrf()).param("role", role))
					.andExpect(redirectedUrl("/admin/members"))
					.andExpect(flash().attribute("error", "허용되지 않는 권한입니다 : " + role));
		}
		assertEquals("USER", memberRepository.findById(student1.getId()).orElseThrow().getRole());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminCanDeleteMember() throws Exception {
		mockMvc.perform(post("/admin/members/{id}/delete", student1.getId()).with(csrf()))
				.andExpect(redirectedUrl("/admin/members"))
				.andExpect(flash().attribute("message", "회원을 삭제했습니다."));

		assertTrue(memberRepository.findByUsername("student1").isEmpty());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void deletingMissingMemberShowsError() throws Exception {
		mockMvc.perform(post("/admin/members/{id}/delete", 9999L).with(csrf()))
				.andExpect(flash().attribute("error", "회원이 존재하지 않습니다."));
	}

	// 슬라이드 11 : /mypage 는 로그인한 내 정보만 (id 를 URL 로 받지 않는다 → IDOR 차단)
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void mypageShowsLoggedInMemberOnly() throws Exception {
		mockMvc.perform(get("/mypage").param("id", String.valueOf(admin.getId())))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("student1")))
				.andExpect(content().string(containsString("[ROLE_USER]")))
				.andExpect(content().string(not(containsString("관리자 페이지로 이동"))));
	}

	// 슬라이드 8, 10 : 메뉴와 배지는 권한에 따라 다르게 보인다.
	@Test
	@WithMockUser(username = "student1", roles = "USER")
	void userSeesMyPageMenuAndUserBadgeButNoAdminMenu() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(content().string(containsString("내 정보")))
				.andExpect(content().string(containsString("role-badge role-USER")))
				.andExpect(content().string(not(containsString("href=\"/admin/members\""))));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void adminSeesRedAdminMenuAndAdminBadge() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(content().string(containsString("href=\"/admin/members\"")))
				.andExpect(content().string(containsString("role-badge role-ADMIN")));
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
