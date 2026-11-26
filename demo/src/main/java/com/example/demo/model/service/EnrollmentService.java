package com.example.demo.model.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.model.domain.Enrollment;
import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;
import com.example.demo.model.repository.EnrollmentRepository;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.repository.PostRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher; // 추가
import com.example.demo.model.dto.SeatMessage;
import com.example.demo.model.dto.Notification;

@Service // [11주차] 단계 4 : 신청 서비스
public class EnrollmentService {
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired // [12주차] 실시간 메시지
    // private SimpMessagingTemplate messagingTemplate;
    private ApplicationEventPublisher eventPublisher; // [12주차] 단계 3 : 이벤트 발행 (보내기는 커밋 뒤에 RealtimeNotifier 가)

    // 내 신청 내역
    public List<Enrollment> findMine(String username) {
        return enrollmentRepository.findByMember_UsernameOrderByIdDesc(username);
    }

    // 단계 4 : 바구니의 모집글을 한 번에 신청 확정
    @Transactional // 단계 5 : 하나라도 실패하면 모두 취소 (롤백)
    public int confirm(String username, Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            throw new IllegalStateException("바구니가 비어 있습니다.");
        }
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("회원이 존재하지 않습니다."));
        for (Long postId : postIds) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalStateException("삭제된 모집글이 바구니에 있습니다."));
            if (enrollmentRepository.existsByMemberAndPost(member, post)) {
                throw new IllegalStateException("이미 신청한 모집글입니다 : " + post.getTitle());
            }
            if (post.isFull()) { // 담을 때는 자리가 있었지만, 그사이 마감됐을 수 있다
                throw new IllegalStateException("정원이 마감되었습니다 : " + post.getTitle());
            }
            Enrollment enrollment = new Enrollment();
            enrollment.setMember(member);
            enrollment.setPost(post);
            enrollment.setCreatedAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment); // ① INSERT 신청 내역
            post.setAppliedCount(post.getAppliedCount() + 1);
            postRepository.save(post); // ② UPDATE 신청 인원 + 1 (남은 자리 - 1)
            // messagingTemplate.convertAndSend("/topic/seats", SeatMessage.from(post)); //
            // [12주차] 단계 2 : 남은 자리를 바로 보낸다
            eventPublisher.publishEvent(SeatMessage.from(post)); // [12주차] 단계 3 : 이벤트만 던진다 (커밋되면 RealtimeNotifier 가 보낸다)
            String writer = post.getAuthor().getUsername();
            if (!writer.equals(username)) { // [12주차] 단계 4 : 모집글 쓴 사람에게 알림 (내 글에 내가 신청하면 생략)
                eventPublisher.publishEvent(new Notification(writer,
                        member.getName() + "님이 '" + post.getTitle() + "' 에 신청했습니다. ("
                                + post.getAppliedCount() + "/" + post.getCapacity() + ")",
                        post.getId()));
            }

        }
        return postIds.size();
    }

    // [11주차 연습문제 1] 신청 취소 : 신청 내역 삭제 + 신청 인원 - 1 (한 트랜잭션)
    @Transactional
    public void cancel(Long enrollmentId, String username) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신청 내역이 존재하지 않습니다."));
        if (!enrollment.getMember().getUsername().equals(username)) { // 본인 신청만 → 아니면 403
            throw new AccessDeniedException("본인 신청만 취소할 수 있습니다.");
        }
        Post post = enrollment.getPost();
        post.setAppliedCount(post.getAppliedCount() - 1); // save() 없이 변경 감지로 커밋 때 UPDATE
        enrollmentRepository.delete(enrollment); // DELETE 신청 내역
        eventPublisher.publishEvent(SeatMessage.from(post)); // [12주차 연습문제 1] 남은 자리 + 1 을 커밋 뒤에 모두에게
        String writer = post.getAuthor().getUsername();
        if (!writer.equals(username)) { // [12주차 연습문제 1] 모집글 쓴 사람에게 취소 알림 (내 글이면 생략)
            eventPublisher.publishEvent(new Notification(writer,
                    enrollment.getMember().getName() + "님이 '" + post.getTitle() + "' 신청을 취소했습니다.",
                    post.getId()));
        }
    }
}
