package com.app.codemasterpiecebackend.domain.shared.security;

public enum RoleType {
    AUTHOR, USER;

    public static boolean isAuthor(RoleType roleType) {
        return roleType.equals(AUTHOR);
    }
}
