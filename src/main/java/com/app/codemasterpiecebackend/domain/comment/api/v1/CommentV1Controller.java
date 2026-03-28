package com.app.codemasterpiecebackend.domain.comment.api.v1;

import com.app.codemasterpiecebackend.domain.comment.application.CommentCommand;
import com.app.codemasterpiecebackend.domain.comment.application.CommentService;
import com.app.codemasterpiecebackend.domain.comment.dto.CommentDTO;
import com.app.codemasterpiecebackend.domain.comment.entity.ReactionValue;
import com.app.codemasterpiecebackend.global.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.global.support.constant.HttpConstants;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.global.util.ActorUtil;
import com.app.codemasterpiecebackend.global.util.PageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentV1Controller {
    private final CommentService commentService;

    @PostMapping
    public SuccessPayload<?> createComment(
            @Valid @RequestBody CommentRequest.Create body,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        var dto = commentService.create(body.toCmd(actor, userDetails));

        return SuccessPayload.of(dto);
    }

    @GetMapping
    public SuccessPayload<?> listComments(
            @RequestParam(name = "post-id") String postId,
            @PageableDefault(size = 5) Pageable pageable,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        final boolean elevated = userDetails != null && userDetails.hasRole("AUTHOR");

        var result = commentService.getPageByPostId(new CommentCommand.View(postId, elevated, actor.actorId(), pageable));

        return SuccessPayload.of(PageUtil.toResponseMap(result));
    }

    @GetMapping("/{commentId}/raw")
    public SuccessPayload<String> getRawComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        // 원본 데이터를 가져오는 서비스 호출
        String rawContent = commentService.getRawContent(new CommentCommand.Raw(
                commentId,
                actor.elevated(),
                actor.actorId()
        ));

        return SuccessPayload.of(rawContent);
    }

    @PatchMapping("/{commentId}")
    public SuccessPayload<?> updateComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentRequest.Update body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        CommentDTO dto = commentService.update(new CommentCommand.Update(
                commentId,
                Objects.requireNonNull(body).content(),
                actor.elevated(),
                actor.actorId(),
                guestPassword
        ));

        return SuccessPayload.of(dto);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentRequest.Delete body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        commentService.delete(new CommentCommand.Delete(
                commentId,
                actor.elevated(),
                actor.actorId(),
                guestPassword
        ));
    }

    @PatchMapping("/{commentId}/visibility")
    public SuccessPayload<?> toggleHideComment(
            @PathVariable String commentId,
            @RequestParam(name = "hidden") boolean hidden
    ) {
        boolean result = commentService.toggleHide(new CommentCommand.Like(
                commentId,
                hidden
        ));

        return SuccessPayload.of(Map.of("hide", result));
    }


    @PostMapping("/{id}/reaction")
    public SuccessPayload<?> react(
            @PathVariable("id") String commentId,
            @RequestParam(value = "value", required = false) ReactionValue value,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);
        if (actor.actorId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "error.need_client_key");
        }

        var cmd = new CommentCommand.React(
                commentId,
                actor.provider(),
                actor.actorId(),
                value
        );
        ReactionValue react = commentService.react(cmd);
        var payload = new HashMap<String, Object>();
        payload.put("myReaction", react);
        return SuccessPayload.of(payload);
    }
}
