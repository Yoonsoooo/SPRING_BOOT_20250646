package com.example.demo.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.model.domain.TestDB;
import com.example.demo.model.repository.TestRepository;

@Service // 서비스 계층, 비즈니스 로직 처리
public class TestService {

    @Autowired // 리포지토리 객체 의존성 주입(DI)
    private TestRepository testRepository;

    public TestDB findByName(String name) { // 이름으로 1명 조회
        return testRepository.findByName(name);
    }

    public List<TestDB> findAll() { // 전체 사용자 조회 (SELECT * FROM testdb)
        return testRepository.findAll();
    }
}
