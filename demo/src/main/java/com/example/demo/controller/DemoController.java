package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.domain.TestDB;
import com.example.demo.model.service.TestService; // 서비스 클래스 연동

@Controller
public class DemoController {

    @Autowired
    TestService testService; // DemoController 클래스 아래 객체 주입

    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("data", "반갑습니다. 2주차 URL 매핑이 정상적으로 동작합니다.");
        return "hello";
    }

    @GetMapping("/hello2")
    public String hello2(Model model) {
        model.addAttribute("studentId", "20250646");
        model.addAttribute("course", "자바웹프로그래밍(2)");
        model.addAttribute("week", "2주차 연습문제");
        model.addAttribute("framework", "Spring Boot 4.1.1");
        model.addAttribute("goal", "MVC와 Thymeleaf의 흐름 이해하기");
        return "hello2";
    }

    // 4주차 - 데이터베이스 연동 테스트 페이지
    @GetMapping("/testdb")
    public String getAllTestDBs(Model model) {
        // 실습 1 : 이름으로 1명 조회 (다수 사용자 출력으로 바꾸면서 주석 처리)
        // TestDB test = testService.findByName("홍길동");
        // model.addAttribute("data4", test);
        // System.out.println("데이터 출력 디버그 : " + test);

        // 실습 2 / 연습문제 : 전체 사용자 출력
        List<TestDB> users = testService.findAll();
        model.addAttribute("users", users);
        System.out.println("데이터 출력 디버그 : " + users);
        return "testdb";
    }
}
