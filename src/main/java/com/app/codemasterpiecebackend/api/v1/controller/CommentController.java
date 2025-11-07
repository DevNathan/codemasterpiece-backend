package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.api.v1.request.comment.CommentCreateRequest;
import com.app.codemasterpiecebackend.api.v1.request.comment.CommentDeleteRequest;
import com.app.codemasterpiecebackend.api.v1.request.guestbook.CommentUpdateRequest;
import com.app.codemasterpiecebackend.domain.dto.comment.CommentDTO;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.service.comment.CommentService;
import com.app.codemasterpiecebackend.service.comment.cmd.*;
import com.app.codemasterpiecebackend.support.constant.HttpConstants;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.util.ActorUtil;
import com.app.codemasterpiecebackend.util.PageUtil;
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

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public SuccessPayload<?> createComment(
            @Valid @RequestBody CommentCreateRequest body,
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

        var result = commentService.getPageByPostId(new CommentViewCmd(postId, elevated, actor.actorId(), pageable));

        return SuccessPayload.of(PageUtil.toResponseMap(result));
    }

    @PatchMapping("/{commentId}")
    public SuccessPayload<?> updateComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentUpdateRequest body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        CommentDTO dto = commentService.update(new CommentUpdateCmd(
                commentId,
                body.content(),
                actor.elevated(),
                actor.actorId(),
                guestPassword
        ));

        return SuccessPayload.of(dto);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(
            @PathVariable String commentId,
            @RequestBody @Valid CommentDeleteRequest body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        commentService.delete(new CommentDeleteCmd(
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
        boolean result = commentService.toggleHide(new CommentLikeCmd(
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

        var cmd = new CommentReactCmd(
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
