package com.app.codemasterpiecebackend.api.v1.request.comment;

import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.service.comment.cmd.CommentCreateCmd;
import com.app.codemasterpiecebackend.support.exception.FieldValidationException;
import com.app.codemasterpiecebackend.util.ActorUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

/**
 * 댓글 생성 요청 DTO.
 * - 기본 필드의 정적 검증은 Bean Validation으로 처리
 * - ANON 전용 필드는 toCmd(actor)에서 조건부 검증/전처리
 */
public record CommentCreateRequest(

        /** 대상 포스트 식별자 (예: PO-ULID) */
        @NotBlank(message = "validation.comment.post_id.not_blank")
        String postId,

        /** 본문 (최대 2000) */
        @NotBlank(message = "validation.comment.content.not_blank")
        @Size(max = 2000, message = "validation.comment.content.max_length")
        String content,

        String parentId,

        /* ---------- ANON 전용 (GITHUB에서는 무시) ---------- */

        /** 게스트 닉네임 */
        @Size(max = 10, message = "validation.comment.nickname.max_length")
        String guestDisplayName,

        String guestImageUrl,

        /** 게스트 4자리 PIN (숫자 6자리) */
        @Pattern(regexp = "\\d{6}", message = "validation.comment.pin.pattern")
        String guestPin
) {
    public CommentCreateCmd toCmd(ActorUtil.Actor actor, AppUserDetails userDetails) {

        switch (actor.provider()) {
            case GITHUB -> {
                return new CommentCreateCmd(
                        postId,
                        content,
                        parentId,
                        actor,
                        userDetails.getAppUser().nickname(),
                        null
                );
            }
            case ANON -> {
                if (guestDisplayName == null) {
                    throw new FieldValidationException(
                            Map.of("guestDisplayName", "validation.comment.guest_dm.not_blank")
                    );
                }
                if (guestPin == null) {
                    throw new FieldValidationException(
                            Map.of("guestPin", "validation.comment.guest_pin.not_blank")
                    );
                }

                return new CommentCreateCmd(
                        postId,
                        content,
                        parentId,
                        actor,
                        guestDisplayName,
                        new CommentCreateCmd.GuestPayload(guestImageUrl, guestPin)
                );
            }
            default -> throw new FieldValidationException(
                    Map.of("content", "error.internal")
            );
        }
    }
}
