-- 4주차 연습문제 : 사용자 추가 및 출력하기
-- 1) MySQL Command Line Client 또는 VS Code MySQL 확장에서 먼저 DB를 만든다.
CREATE DATABASE IF NOT EXISTS spring;
FLUSH PRIVILEGES;
USE spring;

-- 2) 스프링 서버를 한 번 실행하면 ddl-auto=update 설정으로 testdb 테이블(id, name, age, gender)이 자동 생성된다.
--    서버 실행 후 아래 INSERT 문을 실행한다.
INSERT INTO testdb (id, name, age, gender) VALUES
    (1, '홍길동', 25, '남'),
    (2, '아저씨', 40, '남'),
    (3, '김영희', 30, '여'),
    (4, '이철수', 22, '남');

-- 3) 결과 확인
SELECT * FROM testdb;
