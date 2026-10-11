package com.project.distributed_lovable.common_lib.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class JwtAuthFilter extends OncePerRequestFilter {

    AuthUtil authUtil;
    HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();
        final String dispatcherType = request.getDispatcherType().name();
        final String requestHeaderToken = request.getHeader("Authorization");
        final boolean hasBearerToken = requestHeaderToken != null && requestHeaderToken.startsWith("Bearer ");

        log.info("JwtAuthFilter incoming request: [{}] {}, DispatcherType: {}, AuthHeaderPresent: {}",
                method, requestURI, dispatcherType, hasBearerToken);

        if (!hasBearerToken) {
            log.debug("No Bearer token found for [{}] {}, proceeding along filter chain", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = requestHeaderToken.substring(7);

        try {
            log.debug("Token verification started for [{}] {}", method, requestURI);
            JwtUserPrincipal user = authUtil.verifyAccessToken(token);

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                token,
                                user.authorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.info("Authentication established: userId={}, username='{}' for [{}] {}",
                        user.userId(), user.username(), method, requestURI);
            }
        } catch (Exception e) {
            log.error("Token verification failed for [{}] {}: {}", method, requestURI, e.getMessage(), e);
            handlerExceptionResolver.resolveException(request, response, null, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"status\":\"UNAUTHORIZED\",\"message\":\"Authentication failed: %s\"}",
                        e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Invalid token"
                ));
            }
            return;
        }

        try {
            log.debug("Filter chain started for [{}] {}", method, requestURI);
            filterChain.doFilter(request, response);
            log.debug("Filter chain returned for [{}] {}, responseStatus={}", method, requestURI, response.getStatus());
        } catch (Exception e) {
            log.error("Downstream exception processing [{}] {}: {}", method, requestURI, e.getMessage(), e);
            handlerExceptionResolver.resolveException(request, response, null, e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"status\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Internal server error: %s\"}",
                        e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unexpected error"
                ));
            }
        }
    }
}
