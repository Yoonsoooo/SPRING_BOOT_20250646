package com.example.demo.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// [7주차 연습문제 ②] API 오류 처리를 한 곳에 모은다 → 컨트롤러마다 반복되던 try-catch 제거
// annotations = RestController.class : JSON API 컨트롤러에만 적용 (화면 컨트롤러는 기존 방식 그대로)
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class) // 중복 아이디, 빈 값, 허용되지 않는 권한, 없는 회원, 본인 보호 …
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // 400
    }

    @ExceptionHandler(HttpMessageNotReadableException.class) // JSON 따옴표·쉼표·괄호 오류
    public ResponseEntity<Map<String, String>> handleBadJson(HttpMessageNotReadableException e) {
        // trace(서버 내부 정보) 대신 짧은 메시지만 → 실서비스에서 내부 구조 노출 방지
        return ResponseEntity.badRequest().body(Map.of("error", "요청 JSON 형식이 올바르지 않습니다.")); // 400
    }
}
