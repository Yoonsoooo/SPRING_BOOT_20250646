package com.example.demo.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.model.domain.Post;
import com.example.demo.model.domain.Member;

@Repository // 게시글 리포지토리 : save, findAll, findById, delete 는 이미 있다
public interface PostRepository extends JpaRepository<Post, Long> {
    // [9주차 연습문제 ②] 내가 쓴 글 : 작성자(author)의 username 으로 찾는다
    // [10주차 연습문제 1] 이름 끝의 OrderByIdDesc 대신 Pageable 이 정렬·10개씩을 맡는다
    // SQL : ... WHERE m.username = ? ORDER BY p.id DESC LIMIT ?, ? (+ count 쿼리)
    Page<Post> findByAuthor_Username(String username, Pageable pageable);

    // [10주차] 단계 2 : 제목 또는 내용에 검색어가 들어간 글 (JPQL)
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> search(@Param("keyword") String keyword, Pageable pageable);

    // [10주차 연습문제 2] 작성자 이름에 검색어가 들어간 글 (posts JOIN member)
    @Query("SELECT p FROM Post p WHERE p.author.name LIKE %:name%")
    Page<Post> searchByWriter(@Param("name") String name, Pageable pageable);

    // [10주차] 단계 6 : 이 회원이 쓴 글이 있는지 (회원 삭제 전 확인, SQL : ... LIMIT 1)
    boolean existsByAuthor(Member author);
}