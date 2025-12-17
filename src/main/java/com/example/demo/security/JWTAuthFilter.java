package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // ✅ REMOVED /api/payment/ - it NEEDS authentication!
        return path.startsWith("/auth/")
            || path.startsWith("/api/auth/")
            || path.startsWith("/actuator/")
            || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        
        System.out.println("🔍 JWT Filter - Path: " + request.getServletPath());
        System.out.println("🔍 JWT Filter - Authorization header: " + (header != null ? "Present" : "Missing"));
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String email = jwtUtil.extractEmail(token);
                System.out.println("✅ JWT Filter - Extracted email: " + email);
                
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.emptyList()
                        );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                
                System.out.println("✅ JWT Filter - Authentication set for: " + email);
            } catch (Exception e) {
                System.err.println("❌ JWT Filter - Token validation failed: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else {
            System.err.println("❌ JWT Filter - No valid Authorization header");
        }
        
        filterChain.doFilter(request, response);
    }
}