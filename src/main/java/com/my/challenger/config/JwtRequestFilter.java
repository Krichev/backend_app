// src/main/java/com/my/challenger/config/JwtRequestFilter.java
package com.my.challenger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    private static final String BEARER_PREFIX = "Bearer ";

    // Initialize ObjectMapper with JavaTimeModule for LocalDateTime serialization
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // Get JWT Token from Authorization header
        if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
            jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length());
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            } catch (ExpiredJwtException e) {
                // Token is expired - return 401 immediately
                log.error("JWT Token has expired", e);
                sendUnauthorizedResponse(response, "JWT token has expired");
                return; // Stop filter chain
            } catch (Exception e) {
                log.error("Unable to get JWT Token", e);
                sendUnauthorizedResponse(response, "Invalid JWT token");
                return; // Stop filter chain
            }
        } else {
            log.debug("JWT Token does not begin with Bearer String or is missing");
        }

        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails;
                try {
                    userDetails = this.userDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    // Fallback: try userId claim (handles username changes gracefully)
                    Long userId = jwtTokenUtil.getUserIdFromToken(jwtToken);
                    if (userId != null) {
                        userDetails = this.userDetailsService.loadUserById(userId);
                        log.info("JWT username '{}' not found, resolved by userId {} (username likely changed)",
                                username, userId);
                    } else {
                        log.warn("User not found for JWT subject '{}' and no userId claim present. Returning 401.", username);
                        sendUnauthorizedResponse(response, "User not found. Token may be stale - please refresh.");
                        return;
                    }
                }

                if (jwtTokenUtil.validateToken(jwtToken)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set in SecurityContext for user: {}", username);
                } else {
                    log.warn("JWT Token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                log.error("Error setting authentication for user: {}", username, e);
                sendUnauthorizedResponse(response, "Authentication failed");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Send 401 Unauthorized response with JSON error message
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", 401);
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}