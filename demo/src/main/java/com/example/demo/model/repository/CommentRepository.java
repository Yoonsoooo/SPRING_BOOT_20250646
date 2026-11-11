package com.example.demo.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.domain.Comment;
import com.example.demo.model.domain.Post;
import com.example.demo.model.domain.Member;

@Repository // 댓글 리포지토리
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByIdAsc(Post post); // 이 글의 댓글 (오래된 순)

    // [10주차] 단계 6 : 이 회원이 쓴 댓글이 있는지 (회원 삭제 전 확인)
    boolean existsByAuthor(Member author);
}