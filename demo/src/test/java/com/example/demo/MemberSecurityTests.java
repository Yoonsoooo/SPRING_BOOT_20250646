package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import com.example.demo.model.domain.Member;
import com.example.demo.model.repository.MemberRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 5주차 로그인/로그아웃 및 암호화 테스트 (테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class MemberSecurityTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		memberRepository.deleteAll();
	}

	@Test
	void publicPagesAreOpenWithoutLogin() throws Exception {
		mockMvc.perform(get("/")).andExpect(status().isOk());
		mockMvc.perform(get("/login")).andExpect(status().isOk());
		mockMvc.perform(get("/signup")).andExpect(status().isOk());
		mockMvc.perform(get("/css/bootstrap.min.css")).andExpect(status().isOk());
	}

	@Test
	void mainPageShowsLoginButtonBeforeLogin() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(content().string(containsString("회원목록")))
				.andExpect(content().string(containsString("로그인")));
	}

	@Test
	void memberListRedirectsToLoginWhenAnonymous() throws Exception {
		mockMvc.perform(get("/testdb"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void signupStoresBcryptHashedPassword() throws Exception {
		mockMvc.perform(post("/signup").with(csrf())
						.param("username", "student1")
						.param("password", "123123")
						.param("passwordConfirm", "123123")
						.param("name", "홍길동"))
				.andExpect(redirectedUrl("/login?signup"));

		Member member = memberRepository.findByUsername("student1").orElseThrow();
		assertNotEquals("123123", member.getPassword());
		assertTrue(member.getPassword().startsWith("$2a$10$"));
		assertEquals(60, member.getPassword().length());
		assertTrue(passwordEncoder.matches("123123", member.getPassword()));
		assertEquals("USER", member.getRole());
	}

	@Test
	void duplicateUsernameShowsError() throws Exception {
		signup("student1", "123123");

		mockMvc.perform(post("/signup").with(csrf())
						.param("username", "student1")
						.param("password", "999999")
						.param("passwordConfirm", "999999")
						.param("name", "김철수"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("이미 사용 중인 아이디입니다.")));
	}

	@Test
	void signupWithoutCsrfTokenIsForbidden() throws Exception {
		mockMvc.perform(post("/signup").param("username", "hacker"))
				.andExpect(status().isForbidden());
	}

	@Test
	void loginSucceedsWithCorrectPasswordAndFailsWithWrongOne() throws Exception {
		signup("student1", "123123");

		mockMvc.perform(formLogin("/login").user("student1").password("123123"))
				.andExpect(authenticated().withUsername("student1"))
				.andExpect(redirectedUrl("/"));

		mockMvc.perform(formLogin("/login").user("student1").password("wrong"))
				.andExpect(unauthenticated())
				.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	void logoutRedirectsToLoginPage() throws Exception {
		mockMvc.perform(logout("/logout"))
				.andExpect(redirectedUrl("/login?logout"));
	}

	// [5주차 연습문제 ②] 비밀번호 확인이 다르면 오류 메시지를 보여주고 DB에 저장하지 않는다.
	@Test
	void signupWithMismatchedPasswordIsRejectedAndNotSaved() throws Exception {
		mockMvc.perform(post("/signup").with(csrf())
						.param("username", "student2")
						.param("password", "123123")
						.param("passwordConfirm", "321321")
						.param("name", "김철수"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("비밀번호가 일치하지 않습니다.")))
				.andExpect(content().string(containsString("value=\"student2\"")));

		assertFalse(memberRepository.existsByUsername("student2"));
	}

	// [5주차 연습문제 ①] 로그인 상태 유지 체크 시 7일짜리 remember-me 쿠키가 발급된다.
	@Test
	void rememberMeCheckboxIssuesSevenDayCookie() throws Exception {
		signup("student1", "123123");

		mockMvc.perform(post("/login").with(csrf())
						.param("username", "student1")
						.param("password", "123123")
						.param("remember-me", "on"))
				.andExpect(authenticated().withUsername("student1"))
				.andExpect(cookie().exists("remember-me"))
				.andExpect(cookie().maxAge("remember-me", 60 * 60 * 24 * 7));
	}

	// 브라우저를 닫아 세션(JSESSIONID)이 없어져도 remember-me 쿠키만으로 회원목록에 들어갈 수 있다.
	@Test
	void rememberMeCookieAloneKeepsUserLoggedIn() throws Exception {
		signup("student1", "123123");

		Cookie rememberMe = mockMvc.perform(post("/login").with(csrf())
						.param("username", "student1")
						.param("password", "123123")
						.param("remember-me", "on"))
				.andReturn().getResponse().getCookie("remember-me");
		assertNotNull(rememberMe);

		mockMvc.perform(get("/testdb").cookie(rememberMe))
				.andExpect(status().isOk())
				.andExpect(authenticated().withUsername("student1"));
	}

	@Test
	void loginWithoutRememberMeIssuesNoCookie() throws Exception {
		signup("student1", "123123");

		mockMvc.perform(formLogin("/login").user("student1").password("123123"))
				.andExpect(cookie().doesNotExist("remember-me"));
	}

	private void signup(String username, String password) throws Exception {
		mockMvc.perform(post("/signup").with(csrf())
				.param("username", username)
				.param("password", password)
				.param("passwordConfirm", password)
				.param("name", "홍길동"));
	}
}
