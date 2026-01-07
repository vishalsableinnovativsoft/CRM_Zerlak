package com.startica.privateapp.auth.service;

import com.startica.privateapp.auth.dto.LoginRequest;
import com.startica.privateapp.auth.dto.LoginResponse;
import com.startica.privateapp.auth.dto.RefreshTokenRequest;
import com.startica.privateapp.common.exception.ResourceNotFoundException;
import com.startica.privateapp.common.exception.UnauthorizedException;
import com.startica.privateapp.model.RefreshToken;
import com.startica.privateapp.model.User;
import com.startica.privateapp.repository.RefreshTokenRepository;
import com.startica.privateapp.repository.UserRepository;
import com.startica.privateapp.util.JwtUtil;
import com.startica.privateapp.multitenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days default
    private Long refreshTokenDuration;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String tenant = request.getTenant();
        if (tenant == null || tenant.isEmpty()) {
            throw new UnauthorizedException("Tenant is required for login");
        }
        try {
            TenantContext.setTenant(tenant);
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

            // Check if user is active
            if (!user.getActive()) {
                throw new UnauthorizedException("User account is deactivated");
            }

            // Generate tokens with tenant info
            String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name(), tenant);
            String refreshToken = createRefreshToken(user.getId());

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Build response
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole())
                    .active(user.getActive())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .build();

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .user(userInfo)
                    .build();
        } finally {
            TenantContext.clearTenant();
        }
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }
        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", refreshToken.getUserId()));
        String tenant = user.getTenant();
        if (tenant == null || tenant.isEmpty()) {
            throw new UnauthorizedException("Tenant is required for refresh");
        }
        try {
            TenantContext.setTenant(tenant);
            if (!user.getActive()) {
                throw new UnauthorizedException("User account is deactivated");
            }
            // Generate new access token with tenant info
            String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name(), tenant);
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole())
                    .active(user.getActive())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .build();
            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(requestRefreshToken)
                    .tokenType("Bearer")
                    .user(userInfo)
                    .build();
        } finally {
            TenantContext.clearTenant();
        }
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String createRefreshToken(Long userId) {
        // Delete existing refresh token for user
        refreshTokenRepository.deleteByUserId(userId);

        // Create new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenDuration / 1000));

        refreshTokenRepository.save(refreshToken);

        return refreshToken.getToken();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Transactional
    public User createAccount(User user, String tenant) {
        TenantContext.setTenant(tenant);
        try {
            String rawPassword = user.getPassword();
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            User saved = userRepository.save(user);
            return saved;
        } finally {
            TenantContext.clearTenant();
        }
    }

    @Transactional
    public boolean manualLogin(String username, String password, String tenant) {
        TenantContext.setTenant(tenant);
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            if (!matches) throw new UnauthorizedException("Invalid credentials");
            if (!user.getActive()) throw new UnauthorizedException("User is deactivated");
            return true;
        } finally {
            TenantContext.clearTenant();
        }
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username, String tenant) {
        TenantContext.setTenant(tenant);
        try {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        } finally {
            TenantContext.clearTenant();
        }
    }

    @Transactional
    public String changePassword(String username, String currentPassword, String newPassword, String tenant) {
        TenantContext.setTenant(tenant);
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new UnauthorizedException("Current password is incorrect.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return "Password updated successfully.";
        } finally {
            TenantContext.clearTenant();
        }
    }
}
