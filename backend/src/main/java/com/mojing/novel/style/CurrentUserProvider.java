package com.mojing.novel.style;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalStateException("当前请求尚未登录");
        try { return Long.parseLong(authentication.getName()); }
        catch (NumberFormatException error) { throw new IllegalStateException("登录身份无效"); }
    }
}
