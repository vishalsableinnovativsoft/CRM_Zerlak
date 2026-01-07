package com.startica.privateapp.auth.dto;

import com.startica.privateapp.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private Role role;
        private Boolean active;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastLogin;

        public static UserInfoBuilder builder() { return new UserInfoBuilder(); }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        public java.time.LocalDateTime getLastLogin() { return lastLogin; }
        public void setLastLogin(java.time.LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    }

    public static class UserInfoBuilder {
        private final UserInfo userInfo = new UserInfo();
        public UserInfoBuilder id(Long id) { userInfo.setId(id); return this; }
        public UserInfoBuilder username(String username) { userInfo.setUsername(username); return this; }
        public UserInfoBuilder fullName(String fullName) { userInfo.setFullName(fullName); return this; }
        public UserInfoBuilder email(String email) { userInfo.setEmail(email); return this; }
        public UserInfoBuilder phone(String phone) { userInfo.setPhone(phone); return this; }
        public UserInfoBuilder role(Role role) { userInfo.setRole(role); return this; }
        public UserInfoBuilder active(Boolean active) { userInfo.setActive(active); return this; }
        public UserInfoBuilder createdAt(java.time.LocalDateTime createdAt) { userInfo.setCreatedAt(createdAt); return this; }
        public UserInfoBuilder lastLogin(java.time.LocalDateTime lastLogin) { userInfo.setLastLogin(lastLogin); return this; }
        public UserInfo build() { return userInfo; }
    }

    public static LoginResponseBuilder builder() { return new LoginResponseBuilder(); }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public static class LoginResponseBuilder {
        private final LoginResponse response = new LoginResponse();
        public LoginResponseBuilder accessToken(String accessToken) { response.setAccessToken(accessToken); return this; }
        public LoginResponseBuilder refreshToken(String refreshToken) { response.setRefreshToken(refreshToken); return this; }
        public LoginResponseBuilder tokenType(String tokenType) { response.setTokenType(tokenType); return this; }
        public LoginResponseBuilder user(UserInfo user) { response.setUser(user); return this; }
        public LoginResponse build() { return response; }
    }
}
