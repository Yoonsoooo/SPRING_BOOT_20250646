package com.example.demo.model.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity // TestDB 클래스를 DB 테이블과 매핑하는 JPA 엔티티
@Table(name = "testdb") // 매핑할 테이블 이름 : testdb
@Data // setter/getter/toString 등을 자동 생성
public class TestDB {

    @Id // 기본키(PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키 값을 DB가 자동 증가
    private Long id;

    @Column(nullable = true) // null 값 허용
    private String name;

    // 4주차 연습문제 : 나이, 성별 컬럼 추가
    @Column(nullable = true)
    private Integer age;

    @Column(nullable = true)
    private String gender;
}
