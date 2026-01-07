package com.startica.privateapp.multitenant;

import com.startica.privateapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String tenant = null;
            try {
                tenant = jwtUtil.extractTenant(token);
            } catch (Exception ignored) {}
            if (tenant != null) {
                TenantContext.setTenant(tenant);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clearTenant();
        }
    }
}
