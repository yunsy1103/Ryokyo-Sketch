package com.travel.japan.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.japan.dto.GlobalResDto;
import com.travel.japan.entity.RefreshToken;
import com.travel.japan.repository.RefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String accessToken = jwtTokenProvider.getHeaderToken((HttpServletRequest) request, "Access");
        String refreshToken = jwtTokenProvider.getHeaderToken((HttpServletRequest) request, "Refresh");


        log.info("Access Token: {}", accessToken);
        log.info("Refresh Token: {}", refreshToken);

        if (accessToken != null) {
            // accessToken 유효한 경우
            if (jwtTokenProvider.validateToken(accessToken)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("AccessToken 유효");
            }
            // accessToken 만료, refreshToken 유효성 검사
            else if (refreshToken != null && jwtTokenProvider.refreshTokenValidation(refreshToken)) {

                Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByRefreshToken(refreshToken);

                if (refreshTokenOpt.isPresent()) {
                    String email = refreshTokenOpt.get().getEmail();

                    // 새 AccessToken과 RefreshToken 발급
                    TokenInfo tokenInfo = jwtTokenProvider.generateToken(email, Collections.singletonList("ROLE_USER"));
                    String newAccessToken = tokenInfo.getAccessToken();
                    String newRefreshToken = tokenInfo.getRefreshToken();

                    // 새로 발급된 토큰을 로그로 출력
                    log.info("새로운 Access Token: {}", newAccessToken);
                    log.info("새로운 Refresh Token: {}", newRefreshToken);

                    // 헤더에 새 Access Token 설정
                    jwtTokenProvider.setHeaderAccessToken((HttpServletResponse) response, newAccessToken);

                    // 인증 정보 갱신
                    Authentication authentication = jwtTokenProvider.getAuthentication(newAccessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("AccessToken이 만료되었고, 새로운 AccessToken이 발급되었습니다.");
                } else {
                    // RefreshToken이 존재하지 않는 경우
                    log.error("Refresh Token이 존재하지 않습니다.");
                    jwtExceptionHandler((HttpServletResponse) response, "Invalid RefreshToken", HttpStatus.UNAUTHORIZED);
                    return;
                }
            }
            // RefreshToken도 만료된 경우
            else {
                log.error("AccessToken과 RefreshToken 모두 만료되었습니다.");
                jwtExceptionHandler((HttpServletResponse) response, "RefreshToken Expired", HttpStatus.UNAUTHORIZED);
                return;
            }
        } else {
            // AccessToken이 존재하지 않는 경우
            log.error("Authorization 헤더가 없거나 잘못된 형식입니다.");
            jwtExceptionHandler((HttpServletResponse) response, "Unauthorized", HttpStatus.UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    // 예외 핸들러 메서드
    public void jwtExceptionHandler(HttpServletResponse response, String msg, HttpStatus status) {
        response.setStatus(status.value());
        response.setContentType("application/json");
        try {
            String json = new ObjectMapper().writeValueAsString(new GlobalResDto(msg, status.value()));
            response.getWriter().write(json);
        } catch (Exception e) {
            log.error("Error while handling JWT exception: {}", e.getMessage());
        }
    }
}