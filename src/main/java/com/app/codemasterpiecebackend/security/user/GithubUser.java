package com.app.codemasterpiecebackend.security.user;

import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class GithubUser {
    private final String userId;
    private final String nickname;
    private final String avatarUrl;

    public GithubUser(Map<String, Object> attrs) {
        this.userId = attrs.get("id").toString();
        this.nickname = attrs.get("login").toString();
        this.avatarUrl = "https://avatars.githubusercontent.com/u/" + this.userId;
    }
}
