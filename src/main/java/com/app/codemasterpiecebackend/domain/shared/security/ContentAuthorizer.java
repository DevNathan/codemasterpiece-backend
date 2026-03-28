package com.app.codemasterpiecebackend.domain.shared.security;

import com.app.codemasterpiecebackend.domain.shared.embeddable.GuestAuth;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import com.app.codemasterpiecebackend.global.support.exception.FieldValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * 콘텐츠 수정 및 삭제 권한을 검증하는 공통 보안 유틸리티 클래스입니다.
 */
public final class ContentAuthorizer {

    private ContentAuthorizer() {}

    /**
     * 엔티티의 소유권 및 권한을 검증합니다.
     *
     * @param elevated 관리자 권한 여부
     * @param provider 작성자 제공자 (GITHUB, ANON 등)
     * @param entityActorId 엔티티에 저장된 작성자 ID
     * @param guestAuth 엔티티에 저장된 게스트 인증 정보
     * @param requestUserId 요청자가 제출한 사용자 ID
     * @param requestPassword 요청자가 제출한 게스트 비밀번호
     * @param passwordEncoder 비밀번호 검증기
     * @param pinErrorKey 비밀번호 불일치 시 반환할 에러 키
     */
    public static void verifyOwnership(
            boolean elevated,
            ActorProvider provider,
            String entityActorId,
            GuestAuth guestAuth,
            String requestUserId,
            String requestPassword,
            PasswordEncoder passwordEncoder,
            String pinErrorKey
    ) {
        if (elevated) return;

        if (provider != ActorProvider.ANON) {
            boolean isOwner = entityActorId != null && entityActorId.equals(requestUserId);
            if (isOwner) return;
            throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
        }

        boolean pinOk = guestAuth != null &&
                requestPassword != null &&
                passwordEncoder.matches(requestPassword, guestAuth.getPinHash());

        if (pinOk) return;

        throw new FieldValidationException(Map.of("guestPassword", pinErrorKey));
    }
}