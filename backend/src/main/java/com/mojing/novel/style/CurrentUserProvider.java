package com.mojing.novel.style;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 接入 Spring Security 后，只需把这里替换为从 Authentication 读取用户ID。 */
@Component
public class CurrentUserProvider {
    private final long localUserId;

    public CurrentUserProvider(@Value("${mojing.security.local-user-id:1}") long localUserId) {
        this.localUserId = localUserId;
    }

    public long currentUserId() { return localUserId; }
}
