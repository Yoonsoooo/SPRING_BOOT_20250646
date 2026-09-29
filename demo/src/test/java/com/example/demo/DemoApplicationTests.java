package com.example.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.demo.controller.DemoController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void helloMappingsReturnViewsAndModelData() {
		DemoController controller = new DemoController();
		Model helloModel = new ExtendedModelMap();
		Model hello2Model = new ExtendedModelMap();

		assertEquals("hello", controller.hello(helloModel));
		assertNotNull(helloModel.getAttribute("data"));

		assertEquals("hello2", controller.hello2(hello2Model));
		assertEquals(5, hello2Model.asMap().size());
		assertEquals("20250646", hello2Model.getAttribute("studentId"));
	}

	@Test
	void portfolioResourcesAreInSpringBootLocations() {
		String[] requiredResources = {
				"templates/index.html",
				"templates/index_copy.html",
				"templates/hello.html",
				"templates/hello2.html",
				"public/detailed_web.html",
				"public/detailed_ai.html",
				"public/detailed_security.html",
				"public/detailed_game.html",
				"static/css/templatemo-first-portfolio-style.css",
				"static/js/custom.js",
				"static/images/profile.png",
				"static/fonts/bootstrap-icons.woff2"
		};

		for (String resource : requiredResources) {
			assertTrue(new ClassPathResource(resource).exists(), resource + " 파일이 필요합니다.");
		}
	}

	@Test
	void portfolioIndexKeepsOriginalTemplateStructureAndAssignmentLinks() throws IOException {
		String index = new ClassPathResource("templates/index.html")
				.getContentAsString(StandardCharsets.UTF_8);

		assertTrue(index.lines().count() >= 500, "TemplateMo 원본 전체 구조를 유지해야 합니다.");
		assertTrue(index.contains("class=\"clients section-padding\""));
		assertTrue(index.contains("id=\"section_1\""));
		assertTrue(index.contains("id=\"section_5\""));
		assertTrue(index.contains("th:href=\"@{/hello}\""));
		assertTrue(index.contains("th:href=\"@{/hello2}\""));
		assertTrue(index.contains("th:href=\"@{/detailed_web.html}\""));
		assertTrue(index.contains("th:href=\"@{/detailed_ai.html}\""));
		assertTrue(index.contains("th:href=\"@{/detailed_security.html}\""));
		assertTrue(index.contains("th:href=\"@{/detailed_game.html}\""));
		assertTrue(index.contains("<label for=\"name\">"));
		assertTrue(index.contains("<label for=\"email\">"));
		assertTrue(index.contains("<label for=\"message\">"));
	}

}
