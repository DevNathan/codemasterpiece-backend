package com.app.codemasterpiecebackend.security.user;

import com.app.codemasterpiecebackend.domain.types.RoleType;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public class AppUserDetails implements OAuth2User {

    private final AppUser appUser;
    private final Map<String, Object> attributes;

    public AppUserDetails(String userId, String nickname, RoleType role, Map<String, Object> attributes) {
        this.appUser = new AppUser(userId, nickname, role);
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appUser.role().name()));
    }

    @Override
    public String getName() {
        return appUser.userId();
    }

    public record AppUser(String userId, String nickname, RoleType role) {}

    public boolean hasRole(String role) {
        return getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
