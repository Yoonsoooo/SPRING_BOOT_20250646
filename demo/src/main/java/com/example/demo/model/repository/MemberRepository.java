package com.example.demo.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.domain.Member;

@Repository // 리포지토리 등록
public interface MemberRepository extends JpaRepository<Member, Long> {

    // SELECT * FROM member WHERE username = ?
    Optional<Member> findByUsername(String username);

    // 아이디 중복 확인 (존재하면 true)
    boolean existsByUsername(String username);

    // [7주차 연습문제 ①] 회원 검색 : 아이디나 이름에 검색어가 들어간 회원 (id 순서)
    // SELECT * FROM member WHERE username LIKE %?% OR name LIKE %?% ORDER BY id
    List<Member> findByUsernameContainingOrNameContainingOrderByIdAsc(String username, String name);
}
