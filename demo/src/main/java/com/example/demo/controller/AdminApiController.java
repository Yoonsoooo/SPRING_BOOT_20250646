package com.example.demo.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;
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

    @GetMapping // 회원 목록 : GET /api/admin/members
    public List<MemberResponse> list() {
        List<Member> members = memberService.findAll();
        return members.stream().map(MemberResponse::from).toList(); // 엔티티 목록 → DTO 목록
    }

    @GetMapping("/{id}") // 회원 1명 : GET /api/admin/members/3
    public ResponseEntity<MemberResponse> get(@PathVariable Long id) {
        return memberService.findById(id)
                .map(member -> ResponseEntity.ok(MemberResponse.from(member))) // 있으면 200 OK
                .orElse(ResponseEntity.notFound().build()); // 없으면 404
    }

    @PostMapping // 회원 등록 : POST /api/admin/members (본문 JSON → MemberForm)
    public ResponseEntity<?> create(@RequestBody MemberForm form) {
        try {
            Member saved = memberService.signup(form); // 5주차 회원가입 로직 재사용 (BCrypt)
            return ResponseEntity
                    .created(URI.create("/api/admin/members/" + saved.getId())) // 201 + Location
                    .body(MemberResponse.from(saved));
        } catch (IllegalArgumentException e) { // 중복 아이디, 빈 값 …
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); // 400
        }
    }

    @PatchMapping("/{id}/role") // 권한 변경 : PATCH /api/admin/members/3/role {"role":"ADMIN"}
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody RoleRequest request) {
        try {
            Member member = memberService.changeRole(id, request.role());
            return ResponseEntity.ok(MemberResponse.from(member)); // 200 OK + 바뀐 회원
        } catch (IllegalArgumentException e) { // 허용되지 않는 권한, 없는 회원 …
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}") // 회원 삭제 : DELETE /api/admin/members/3
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            memberService.deleteMember(id);
            return ResponseEntity.noContent().build(); // 204 No Content (돌려줄 본문 없음)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
