package com.app.codemasterpiecebackend.domain.comment.application;

import com.app.codemasterpiecebackend.domain.comment.entity.ReactionValue;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.util.ActorUtil;
import org.springframework.data.domain.Pageable;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 댓글(Comment) 도메인의 모든 유즈케이스 입력을 정의하는 통합 Command 클래스.
 */
public final class CommentCommand {

    private CommentCommand() {}

    public record Create(
            String postId,
            String content,
            String parentId,
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

        public Create(String postId, String content, String parentId, ActorUtil.Actor actor, String displayName, String avatarUrl, GuestPayload guest) {
            this.postId = trimToNull(postId);
            this.content = trimToNull(content);
            this.parentId = trimToNull(parentId);
            this.actor = actor;
            this.displayName = trimToNull(displayName);
            this.avatarUrl = avatarUrl;
            this.guest = guest;
        }
    }

    public record Delete(
            String commentId,
            boolean elevated,
            String userId,
            String password
    ) {
        public Delete(String commentId, boolean elevated, String userId, String password) {
            this.commentId = trimToNull(commentId);
            this.elevated = elevated;
            this.userId = trimToNull(userId);
            this.password = trimToNull(password);
        }
    }

    public record Like(
            String commentId,
            boolean setHidden
    ) {
        public Like(String commentId, boolean setHidden) {
            this.commentId = trimToNull(commentId);
            this.setHidden = setHidden;
        }
    }

    public record Raw(
            String commentId,
            boolean elevated,
            String actorId
    ) {}

    public record React(
            String commentId,
            ActorProvider actorProvider,
            String actorId,
            ReactionValue value // null이면 "취소" 의도
    ) {}

    public record Update(
            String commentId,
            String content,
            boolean elevated,
            String userId,
            String password
    ) {
        public Update(String commentId, String content, boolean elevated, String userId, String password) {
            this.commentId = trimToNull(commentId);
            this.content = trimToNull(content);
            this.elevated = elevated;
            this.userId = trimToNull(userId);
            this.password = trimToNull(password);
        }
    }

    public record View(
            String postId,
            boolean elevated,
            String actorId,
            Pageable pageable
    ) {
        public View(String postId, boolean elevated, String actorId, Pageable pageable) {
            this.postId = trimToNull(postId);
            this.elevated = elevated;
            this.actorId = trimToNull(actorId);
            this.pageable = pageable;
        }
    }
}