package com.app.codemasterpiecebackend.global.security.user;

import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@ToString
public class GithubUser {
    private final String login;
    private final String id;
    private final String avatar_url;

    public GithubUser(Map<String, Object> attrs) {
        this.login = attrs.get("login").toString();
        this.id = "GITHUB" + attrs.get("id").toString();
        this.avatar_url = attrs.get("avatar_url").toString();
    }
}