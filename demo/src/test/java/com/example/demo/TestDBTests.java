package com.example.demo;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.demo.controller.DemoController;
import com.example.demo.model.domain.TestDB;
import com.example.demo.model.repository.TestRepository;
import com.example.demo.model.service.TestService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

// 4주차 데이터베이스 연동 테스트 (테스트용 H2 메모리 DB 사용)
@SpringBootTest
@AutoConfigureMockMvc
class TestDBTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	TestRepository testRepository;

	@Autowired
	TestService testService;

	@Autowired
	DemoController demoController;

	@BeforeEach
	void setUp() {
		testRepository.deleteAll();
		testRepository.save(user("홍길동", 25, "남"));
		testRepository.save(user("아저씨", 40, "남"));
		testRepository.save(user("김영희", 30, "여"));
		testRepository.save(user("이철수", 22, "남"));
	}

	@Test
	void findByNameReturnsOneUser() {
		TestDB test = testService.findByName("홍길동");
		assertEquals(25, test.getAge());
		assertEquals("남", test.getGender());
	}

	@Test
	void testdbMappingPutsAllUsersInModel() {
		Model model = new ExtendedModelMap();

		assertEquals("testdb", demoController.getAllTestDBs(model));

		@SuppressWarnings("unchecked")
		List<TestDB> users = (List<TestDB>) model.getAttribute("users");
		assertEquals(4, users.size());
		assertEquals("김영희", users.get(2).getName());
		assertEquals("여", users.get(2).getGender());
	}

	@Test
	@WithMockUser // 5주차 : /testdb 는 로그인한 회원만 볼 수 있다.
	void testdbPageRendersUserTable() throws Exception {
		mockMvc.perform(get("/testdb"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("User List")))
				.andExpect(content().string(containsString("<td>이철수</td>")))
				.andExpect(content().string(containsString("<td>22</td>")));
	}

	private TestDB user(String name, int age, String gender) {
		TestDB user = new TestDB();
		user.setName(name);
		user.setAge(age);
		user.setGender(gender);
		return user;
	}
}
