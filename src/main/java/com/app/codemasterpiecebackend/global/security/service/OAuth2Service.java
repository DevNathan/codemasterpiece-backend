package com.app.codemasterpiecebackend.global.security.service;

import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.domain.shared.security.RoleType;
import com.app.codemasterpiecebackend.global.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.global.security.user.GithubUser;
import com.app.codemasterpiecebackend.global.security.user.GitlabUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OAuth2Service extends DefaultOAuth2UserService {
    @Value("${app.auth.author-github-id}")
    private String authorGithubId;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User delegate = super.loadUser(userRequest);
        Map<String, Object> attrs = delegate.getAttributes();

        String provider = userRequest.getClientRegistration().getRegistrationId();

        System.out.println("attrs = " + attrs);

        switch (provider) {
            case "github": {
                GithubUser user = new GithubUser(attrs);
                RoleType role = user.getId().equals("GITHUB" + authorGithubId) ? RoleType.AUTHOR : RoleType.USER;
                return new AppUserDetails(
                        user.getId(),
                        user.getAvatar_url(),
                        user.getAvatar_url() + "&s=64",
                        user.getLogin(),
                        role,
                        ActorProvider.GITHUB,
                        attrs
                );
            }
            case "gitlab": {
                GitlabUser user = new GitlabUser(attrs);
                return new AppUserDetails(
                        user.getId(),
                        user.getAvatar_url(),
                        user.getAvatar_url() + "?width=64",
                        user.getUsername(),
                        RoleType.USER,
                        ActorProvider.GITLAB,
                        attrs
                );
            }
            default:
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_provider", "error.oauth.not_support", null)
                );
        }
    }
}
