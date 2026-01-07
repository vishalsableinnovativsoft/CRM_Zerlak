package com.startica.privateapp.util;

import com.startica.privateapp.model.User;
import com.startica.privateapp.repository.UserRepository;
import com.startica.privateapp.multitenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Service;

// @Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenant = TenantContext.getTenant();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UsernameNotFoundException("User account is deactivated");
        }
        return new CustomUserDetails(user);
    }
}

// Duplicate bean removed to resolve ConflictingBeanDefinitionException
// This file is intentionally commented out to avoid bean conflicts.
