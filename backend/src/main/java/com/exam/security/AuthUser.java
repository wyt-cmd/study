package com.exam.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public record AuthUser(Long id, String username, String role) {

    public static AuthUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthUser user ? user : null;
    }

    public static AuthUser require() {
        AuthUser user = current();
        if (user == null) {
            throw new com.exam.common.BizException(401, "未登录");
        }
        return user;
    }
}
