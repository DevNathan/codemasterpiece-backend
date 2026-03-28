package com.app.codemasterpiecebackend.domain.guestbook.application;

import com.app.codemasterpiecebackend.global.util.ActorUtil;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 방명록(Guestbook) 도메인의 모든 유즈케이스 입력을 정의하는 통합 Command 클래스입니다.
 */
public final class GuestbookCommand {

    private GuestbookCommand() {}

    public record Create(
            String content,
            ActorUtil.Actor actor,
            String displayName,
            String avatarUrl,
            GuestPayload guest
    ) {
        public record GuestPayload(
                String imageUrl,
                String pin
        ) {
            public GuestPayload(String imageUrl, String pin) {
                this.imageUrl = trimToNull(imageUrl);
                this.pin = trimToNull(pin);
            }
        }

        public Create(String content, ActorUtil.Actor actor, String displayName, String avatarUrl, GuestPayload guest) {
            this.content = trimToNull(content);
            this.actor = actor;
            this.displayName = trimToNull(displayName);
            this.avatarUrl = avatarUrl;
            this.guest = guest;
        }
    }

    public record Delete(
            String entryId,
            boolean elevated,
            String userId,
            String password
    ) {
        public Delete(String entryId, boolean elevated, String userId, String password) {
            this.entryId = trimToNull(entryId);
            this.elevated = elevated;
            this.userId = trimToNull(userId);
            this.password = trimToNull(password);
        }
    }

    public record Update(
            String entryId,
            String content,
            boolean elevated,
            String userId,
            String password
    ) {
        public Update(String entryId, String content, boolean elevated, String userId, String password) {
            this.entryId = trimToNull(entryId);
            this.content = trimToNull(content);
            this.elevated = elevated;
            this.userId = trimToNull(userId);
            this.password = trimToNull(password);
        }
    }

    public record Slice(
            String cursor,
            int size
    ) {
        public int safeSize() {
            return (size <= 0 || size > 100) ? 20 : size;
        }
    }
}