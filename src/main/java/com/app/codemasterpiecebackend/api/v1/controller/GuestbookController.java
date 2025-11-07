package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.api.v1.request.guestbook.GuestbookEntryCreateRequest;
import com.app.codemasterpiecebackend.api.v1.request.guestbook.GuestbookEntryDeleteRequest;
import com.app.codemasterpiecebackend.api.v1.request.guestbook.GuestbookEntryUpdateRequest;
import com.app.codemasterpiecebackend.domain.dto.guestbook.EntryDTO;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.service.guestbook.GuestbookService;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryCreateCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryDeleteCmd;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntrySliceCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryUpdateCmd;
import com.app.codemasterpiecebackend.support.constant.HttpConstants;
import com.app.codemasterpiecebackend.util.ActorUtil;
import com.app.codemasterpiecebackend.util.SliceUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/guestbook")
@RequiredArgsConstructor
public class GuestbookController {

    private final GuestbookService guestbookService;

    // ====== CREATE ======
    @PostMapping
    public SuccessPayload<?> createGuestbookEntry(
            @RequestBody @Valid GuestbookEntryCreateRequest body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        final boolean isAnon = actor.provider() == ActorProvider.ANON;
        final String displayName = isAnon
                ? body.guestDisplayName()
                : (userDetails != null && userDetails.getAppUser() != null
                ? userDetails.getAppUser().nickname()
                : null);

        EntryDTO dto = guestbookService.create(new EntryCreateCommand(
                body.content(),
                actor,
                displayName,
                new EntryCreateCommand.GuestPayload(
                        isAnon ? body.guestImageUrl() : null,
                        isAnon ? body.guestPin() : null
                )
        ));
        return SuccessPayload.of(dto);
    }

    // ====== SLICE ======
    @GetMapping
    public SuccessPayload<?> getGuestbookSlice(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        var slice = guestbookService.getSlice(
                new EntrySliceCommand(cursor, size)
        );

        var payload = SliceUtil.toResponseMap(slice, tail -> {
            long epochMs = tail.getCreatedAt().atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            String raw = epochMs + "|" + tail.getEntryId();
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        });
        return SuccessPayload.of(payload);
    }

    // ====== UPDATE ======
    @PatchMapping("/{entryId}")
    public SuccessPayload<?> updateGuestbookEntry(
            @PathVariable String entryId,
            @RequestBody @Valid GuestbookEntryUpdateRequest body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        var dto = guestbookService.update(new EntryUpdateCmd(
                entryId,
                body.content(),
                actor.elevated(),
                actor.actorId(),
                body.guestPassword()
        ));
        return SuccessPayload.of(dto);
    }

    // ====== DELETE ======
    @DeleteMapping("/{entryId}")
    public void deleteGuestbookEntry(
            @PathVariable String entryId,
            @RequestBody(required = false) @Valid GuestbookEntryDeleteRequest body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        guestbookService.delete(new EntryDeleteCmd(
                entryId,
                actor.elevated(),
                actor.actorId(),
                guestPassword
        ));
    }
}
