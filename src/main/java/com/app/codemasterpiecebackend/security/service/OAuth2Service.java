package com.app.codemasterpiecebackend.security.service;

import com.app.codemasterpiecebackend.domain.types.RoleType;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.security.user.GithubUser;
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
        if (!"github".equals(provider)) {
            // 필터 레벨에서 인증실패로 처리되도록
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_provider", "error.oauth.only_github", null)
            );
        }

        GithubUser user = new GithubUser(attrs);
        RoleType role = user.getUserId().equals(authorGithubId) ? RoleType.AUTHOR : RoleType.USER;

        return new AppUserDetails(
                user.getUserId(),
                user.getNickname(),
                role,
                attrs
        );
    }
}
