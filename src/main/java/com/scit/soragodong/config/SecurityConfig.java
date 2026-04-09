package com.scit.soragodong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;

import com.scit.soragodong.handler.CustomAuthenticationFailureHandler;
import com.scit.soragodong.handler.CustomAuthenticationSuccessHandler;
import com.scit.soragodong.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final CustomAuthenticationSuccessHandler successHandler;
        private final CustomAuthenticationFailureHandler failureHandler;

        @org.springframework.beans.factory.annotation.Autowired(required = false)
        private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

        private SpringSessionBackedSessionRegistry<?> sessionRegistry() {
                return new SpringSessionBackedSessionRegistry<>(this.sessionRepository);
        }

        // /api/internal/** 전용 체인 - 세션 없이 완전히 허용 (모니터링 엔드포인트)
        @Bean
        @Order(1)
        public SecurityFilterChain internalApiFilterChain(HttpSecurity http) throws Exception {
                http
                    .securityMatcher("/api/internal/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder
                                .userDetailsService(userDetailsService)
                                .passwordEncoder(bCryptPasswordEncoder());
                return authenticationManagerBuilder.build();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

                http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                                    .requestMatchers(
                                                    "/landing", "/auth/**", "/"
                                                    , "/css/**", "/js/**", "/images/**", "/static/**"
                                            //모니터링 용 추가
                                            , "/api/internal/**"
                                            , "/actuator/health", "/actuator/metrics/**")
                                    .permitAll()
                                    .anyRequest().authenticated())
                    .formLogin(form -> form
                                    .loginPage("/auth/login")
                                    .loginProcessingUrl("/auth/login-proc")
                                    .successHandler(successHandler)
                                    .failureHandler(failureHandler)
                                    .usernameParameter("userEmail")
                                    .passwordParameter("password"))
                    .logout(logout -> logout
                                    .logoutUrl("/logout")
                                    .logoutSuccessUrl("/login"))
                    .userDetailsService(userDetailsService);

                if (sessionRepository != null) {
                    http.sessionManagement(session -> session
                                    .maximumSessions(1)
                                    .sessionRegistry(sessionRegistry())
                                    .maxSessionsPreventsLogin(false)
                                    .expiredUrl("/auth/login?sessionExpired=true"));
                }

                return http.build();
        }

        @Bean
        public BCryptPasswordEncoder bCryptPasswordEncoder() {
                return new BCryptPasswordEncoder();
        }
}