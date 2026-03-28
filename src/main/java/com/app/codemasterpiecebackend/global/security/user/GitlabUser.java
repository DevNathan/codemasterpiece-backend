package com.app.codemasterpiecebackend.global.security.user;

import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class GitlabUser {
    private final String id;
    private final String username;
    private final String avatar_url;

    public GitlabUser(Map<String, Object> attrs) {
        this.id = "GITLAB" + attrs.get("id").toString();
        this.username = attrs.get("username").toString();
        this.avatar_url = attrs.get("avatar_url").toString();
    }
}
