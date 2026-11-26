package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.model.domain.Enrollment;
import com.example.demo.model.domain.Member;
import com.example.demo.model.domain.Post;
import com.example.demo.model.dto.Notification;
import com.example.demo.model.dto.SeatMessage;
import com.example.demo.model.repository.CommentRepository;
import com.example.demo.model.repository.EnrollmentRepository;
import com.example.demo.model.repository.MemberRepository;
import com.example.demo.model.repository.PostRepository;
import com.example.demo.model.service.CommentService;
import com.example.demo.model.service.EnrollmentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 12주차 WebSocket 실시간 알림 테스트 (단계 3·4 + 연습문제 확인 표, 테스트용 H2 메모리 DB 사용)
// 실제 브라우저 대신 "메시지 보내는 도구"를 가짜(Mock)로 바꿔 끼우고, 누구에게 무엇이 갔는지 확인한다
@SpringBootTest
class RealtimeTests {

	@MockitoBean
	SimpMessagingTemplate messagingTemplate; // 가짜 : 실제로 보내지 않고 "보낸 기록"만 남긴다

	@Autowired
	EnrollmentService enrollmentService;

	@Autowired
	CommentService commentService;

	@Autowired
	MemberRepository memberRepository;

	@Autowired
	PostRepository postRepository;

	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	CommentRepository commentRepository;

	Member admin;
	Member student1;
	Member student3;

	@BeforeEach
	void setUp() {
		cleanUp();
		memberRepository.deleteAll();
		admin = memberRepository.save(member("admin", "관리자", "ADMIN"));
		student1 = memberRepository.save(member("student1", "홍길동", "USER"));
		student3 = memberRepository.save(member("student3", "김학생", "USER"));
	}

	@AfterEach
	void tearDown() { // 다른 테스트의 memberRepository.deleteAll() 이 FK 때문에 실패하지 않도록
		cleanUp();
	}

	private void cleanUp() { // FK 순서 : 신청 · 댓글 → 글
		enrollmentRepository.deleteAll();
		commentRepository.deleteAll();
		postRepository.deleteAll();
	}

	// 단계 3·4 : 확정 성공 → 남은 자리 4 를 모두에게 + 모집글 쓴 admin 에게 알림
	@Test
	void confirmSendsSeatsAndNotification() {
		Post a = saveRecruit("모집글 A", 5, admin);
		enrollmentService.confirm("student1", List.of(a.getId()));

		verify(messagingTemplate).convertAndSend("/topic/seats", new SeatMessage(a.getId(), 1, 5, 4));
		Notification n = captureNotification("admin");
		assertEquals("홍길동님이 '모집글 A' 에 신청했습니다. (1/5)", n.text());
	}

	// 단계 3 : B, C 중 C 가 마감 → 롤백 → 남은 자리 · 알림 둘 다 가지 않는다 (유령 메시지 없음)
	@Test
	void rollbackSendsNothing() {
		Post b = saveRecruit("모집글 B", 5, admin);
		Post c = saveRecruit("모집글 C", 1, admin);
		c.setAppliedCount(1); // C 는 이미 마감
		postRepository.save(c);

		assertThrows(IllegalStateException.class,
				() -> enrollmentService.confirm("student1", List.of(b.getId(), c.getId())));

		verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
		verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
		assertEquals(0, postRepository.findById(b.getId()).orElseThrow().getAppliedCount()); // DB 도 그대로
	}

	// 연습 ① : 마감된 C 를 student3 이 취소 → 남은 자리 1 (모집 중) + admin 에게 취소 알림
	@Test
	void cancelSendsSeatsAndNotification() {
		Post c = saveRecruit("모집글 C", 1, admin);
		enrollmentService.confirm("student3", List.of(c.getId())); // 정원 1 → 마감
		Enrollment enrollment = enrollmentRepository.findByMember_UsernameOrderByIdDesc("student3").get(0);

		enrollmentService.cancel(enrollment.getId(), "student3");

		verify(messagingTemplate).convertAndSend("/topic/seats", new SeatMessage(c.getId(), 0, 1, 1));
		List<Notification> sent = captureNotifications("admin", 2); // 신청 알림 + 취소 알림
		assertEquals("김학생님이 '모집글 C' 신청을 취소했습니다.", sent.get(1).text());
		assertEquals(c.getId(), sent.get(1).postId());
	}

	// 연습 ① : 내 모집글을 내가 신청 → 취소해도 나에게는 알림 없음
	@Test
	void cancelOwnPostSendsNoNotification() {
		Post a = saveRecruit("모집글 A", 5, admin);
		enrollmentService.confirm("admin", List.of(a.getId()));
		Enrollment enrollment = enrollmentRepository.findByMember_UsernameOrderByIdDesc("admin").get(0);

		enrollmentService.cancel(enrollment.getId(), "admin");

		verify(messagingTemplate).convertAndSend("/topic/seats", new SeatMessage(a.getId(), 0, 5, 5)); // 남은 자리는 간다
		verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
	}

	// 연습 ② 1번 : student1 이 admin 의 글에 댓글 → admin 에게 댓글 알림
	@Test
	void commentSendsNotificationToWriter() {
		Post post = saveRecruit("스프링 스터디", null, admin); // 일반 글 (정원 없음)
		commentService.write(post.getId(), "저도 참여하고 싶어요", "student1");

		Notification n = captureNotification("admin");
		assertEquals("홍길동님이 '스프링 스터디' 에 댓글을 남겼습니다.", n.text());
		assertEquals(post.getId(), n.postId());
	}

	// 연습 ② 2번 : admin 이 자기 글에 댓글 → 알림 없음
	@Test
	void ownCommentSendsNoNotification() {
		Post post = saveRecruit("스프링 스터디", null, admin);
		commentService.write(post.getId(), "공지 : 장소 변경", "admin");

		verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(Object.class));
	}

	private Notification captureNotification(String to) {
		return captureNotifications(to, 1).get(0);
	}

	private List<Notification> captureNotifications(String to, int times) {
		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(messagingTemplate, org.mockito.Mockito.times(times))
				.convertAndSendToUser(eq(to), eq("/queue/notifications"), captor.capture());
		return captor.getAllValues();
	}

	private Post saveRecruit(String title, Integer capacity, Member author) {
		Post post = new Post();
		post.setTitle(title);
		post.setContent("내용");
		post.setAuthor(author);
		post.setCapacity(capacity);
		post.setCreatedAt(LocalDateTime.now());
		return postRepository.save(post);
	}

	private Member member(String username, String name, String role) {
		Member member = new Member();
		member.setUsername(username);
		member.setPassword("$2a$10$abcdefghijklmnopqrstuuJ0t2ZbH0Yt7oD8Yp6q1nC1i6Gm8lQ5e");
		member.setName(name);
		member.setRole(role);
		return member;
	}
}
