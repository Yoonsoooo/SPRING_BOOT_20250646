package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 스프링 설정 클래스 등록
@EnableWebSecurity // 스프링 시큐리티 활성화
public class SecurityConfig {

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
                .anyRequest().authenticated())
            .formLogin(form -> form // 2. 폼 로그인 설정 (인증)
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout // 3. 로그아웃 설정
                .logoutUrl("/logout") // 로그아웃 처리 URL (POST)
                .logoutSuccessUrl("/login?logout") // 로그아웃 후 이동
                .invalidateHttpSession(true) // 세션 삭제
                .deleteCookies("JSESSIONID", "remember-me")); // 쿠키 삭제
        return http.build();
    }
}
