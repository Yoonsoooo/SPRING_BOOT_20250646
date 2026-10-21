package com.example.demo.controller;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.domain.Member;
import com.example.demo.model.dto.MemberForm;
import com.example.demo.model.dto.MemberResponse;
import com.example.demo.model.dto.RoleRequest;
import com.example.demo.model.service.MemberService;

@RestController // [7주차] 관리자 회원 관리 API (JSON)
@RequestMapping("/api/admin/members") // 주소는 하나, 할 일은 메서드로 구분
public class AdminApiController {

    @Autowired
    MemberService memberService;

    @GetMapping // 회원 목록 : GET /api/admin/members ([연습문제 ①] ?keyword=student 이면 검색)
    public List<MemberResponse> list(@RequestParam(required = false) String keyword) {
        List<Member> members = (keyword == null || keyword.isBlank())
                ? memberService.findAll() // 검색어 없음 → 전체
                : memberService.search(keyword.trim()); // 아이디·이름에 검색어 포함
        return members.stream().map(MemberResponse::from).toList(); // 엔티티 목록 → DTO 목록
    }

    @GetMapping("/{id}") // 회원 1명 : GET /api/admin/members/3
    public ResponseEntity<MemberResponse> get(@PathVariable Long id) {
        return memberService.findById(id)
                .map(member -> ResponseEntity.ok(MemberResponse.from(member))) // 있으면 200 OK
                .orElse(ResponseEntity.notFound().build()); // 없으면 404
    }

    // [연습문제 ②] try-catch 제거 : 실패(IllegalArgumentException)는 ApiExceptionHandler 가 400 으로 응답
    @PostMapping // 회원 등록 : POST /api/admin/members (본문 JSON → MemberForm)
    public ResponseEntity<MemberResponse> create(@RequestBody MemberForm form) {
        Member saved = memberService.signup(form); // 5주차 회원가입 로직 재사용 (BCrypt)
        return ResponseEntity
                .created(URI.create("/api/admin/members/" + saved.getId())) // 201 + Location
                .body(MemberResponse.from(saved));
    }

    @PatchMapping("/{id}/role") // 권한 변경 : PATCH /api/admin/members/3/role {"role":"ADMIN"}
    public ResponseEntity<MemberResponse> changeRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        Member member = memberService.changeRole(id, request.role());
        return ResponseEntity.ok(MemberResponse.from(member)); // 200 OK + 바뀐 회원
    }

    @DeleteMapping("/{id}") // 회원 삭제 : DELETE /api/admin/members/3
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build(); // 204 No Content (돌려줄 본문 없음)
    }
}
