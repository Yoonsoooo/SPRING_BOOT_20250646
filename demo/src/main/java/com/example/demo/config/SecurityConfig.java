package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration // 스프링 설정 클래스 등록
@EnableWebSecurity // 스프링 시큐리티 활성화
@EnableMethodSecurity // [6주차] 메서드 보안 활성화 : @PreAuthorize 사용
public class SecurityConfig {

    // [5주차 연습문제] remember-me 쿠키 서명용 비밀키 (GitHub에 노출되지 않도록 application-secret.properties 에 저장)
    @Value("${remember-me.key}")
    private String rememberMeKey;

    @Bean // 비밀번호 암호화 객체 등록 (BCrypt 해시)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // 보안 필터 체인 등록
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth // 1. URL 접근 규칙 (인가)
                .requestMatchers("/", "/hello", "/detailed_web.html",
                                 "/login", "/signup", "/error").permitAll()
                // 내 포트폴리오에서 공개하는 실습/상세 페이지
                .requestMatchers("/hello2", "/detailed_ai.html",
                                 "/detailed_security.html", "/detailed_game.html").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                .requestMatchers("/api/hello").permitAll() // [7주차] 첫 번째 API : 누구나
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN") // [6주차] 관리자만 ([7주차] 관리자 API 포함)
                .requestMatchers("/testdb").hasAnyRole("ADMIN", "MANAGER") // [6주차 연습문제 ②] 회원목록 : 관리자·매니저만
                .anyRequest().authenticated()) // 나머지 : 로그인 필요
            .formLogin(form -> form // 2. 폼 로그인 설정 (인증)
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout // 3. 로그아웃 설정
                .logoutUrl("/logout") // 로그아웃 처리 URL (POST)
                .logoutSuccessUrl("/login?logout") // 로그아웃 후 이동
                .invalidateHttpSession(true) // 세션 삭제
                .deleteCookies("JSESSIONID", "remember-me")) // 쿠키 삭제
            .rememberMe(remember -> remember // 4. [5주차 연습문제] 로그인 상태 유지
                .key(rememberMeKey) // 쿠키 위조 방지용 비밀키
                .rememberMeParameter("remember-me") // login.html 체크박스 name
                .tokenValiditySeconds(60 * 60 * 24 * 7)) // 7일 유지
            // 5. [7주차] REST API 설정
            .httpBasic(Customizer.withDefaults()) // Postman : 요청마다 아이디·비밀번호 (Basic 인증)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // JSON API 는 CSRF 토큰 검사 제외
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor( // 로그인 안 한 API 요청 :
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), // 로그인 화면(302) 대신 401
                request -> request.getRequestURI().startsWith("/api/")));
        return http.build();
    }
}
