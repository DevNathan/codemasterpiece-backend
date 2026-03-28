package com.app.codemasterpiecebackend.domain.comment.api.v1;

import com.app.codemasterpiecebackend.domain.comment.application.CommentCommand;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.global.support.exception.FieldValidationException;
import com.app.codemasterpiecebackend.global.util.ActorUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * 댓글(Comment) 도메인의 API 요청(Request) 규격을 통합 관리하는 레코드입니다.
 */
public record CommentRequest() {

    /**
     * 댓글 생성 요청 규격.
     * 인증 방식(OAuth, ANON)에 따른 조건부 검증 로직을 포함합니다.
     */
    public record Create(
            @NotBlank(message = "validation.comment.post_id.not_blank")
            String postId,

            @NotBlank(message = "validation.comment.content.not_blank")
            @Size(max = 2000, message = "validation.comment.content.max_length")
            String content,

            String parentId,

            @Size(max = 10, message = "validation.comment.nickname.max_length")
            String guestDisplayName,

            String guestImageUrl,

            @Pattern(regexp = "\\d{6}", message = "validation.comment.pin.pattern")
            String guestPin
    ) {
        /**
         * 요청 데이터를 기반으로 애플리케이션 명령(Command) 객체를 생성합니다.
         * 익명 사용자의 경우 닉네임과 PIN 번호에 대한 추가 검증을 수행합니다.
         */
        public CommentCommand.Create toCmd(ActorUtil.Actor actor, AppUserDetails userDetails) {
            String avatarUrl = (actor.provider() == ActorProvider.ANON)
                    ? guestImageUrl
                    : userDetails.getAppUser().avatarUrlSmall();

            return switch (actor.provider()) {
                case GITHUB, GITLAB -> new CommentCommand.Create(
                        postId,
                        content,
                        parentId,
                        actor,
                        userDetails.getAppUser().nickname(),
                        avatarUrl,
                        null
                );
                case ANON -> {
                    if (guestDisplayName == null) {
                        throw new FieldValidationException(Map.of("guestDisplayName", "validation.comment.guest_dm.not_blank"));
                    }
                    if (guestPin == null) {
                        throw new FieldValidationException(Map.of("guestPin", "validation.comment.guest_pin.not_blank"));
                    }

                    yield new CommentCommand.Create(
                            postId,
                            content,
                            parentId,
                            actor,
                            guestDisplayName,
                            avatarUrl,
                            new CommentCommand.Create.GuestPayload(guestImageUrl, guestPin)
                    );
                }
            };
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Update(
            @NotBlank(message = "validation.comment.content.not_blank")
            @Size(max = 2000, message = "validation.comment.content.max_length")
            String content,

            @Nullable String guestPassword
    ) {
    }

    /**
     * 댓글 삭제 요청 규격.
     */
    public record Delete(
            @Nullable String guestPassword
    ) {
    }
}