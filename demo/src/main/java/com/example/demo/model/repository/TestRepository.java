package com.example.demo.model.repository;

import org.springframework.data.jpa.repository.JpaRepository; // JPA 기본 CRUD 기능
import org.springframework.stereotype.Repository; // 리포지토리 빈 등록
import com.example.demo.model.domain.TestDB; // 엔티티 클래스

@Repository // 리포지토리 계층 명시
public interface TestRepository extends JpaRepository<TestDB, Long> {

    // 이름으로 사용자 1명 조회 (SELECT * FROM testdb WHERE name = ?)
    TestDB findByName(String name);
}
