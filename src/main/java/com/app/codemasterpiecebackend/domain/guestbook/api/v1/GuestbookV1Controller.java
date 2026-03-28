package com.app.codemasterpiecebackend.domain.guestbook.api.v1;

import com.app.codemasterpiecebackend.domain.guestbook.application.GuestbookCommand;
import com.app.codemasterpiecebackend.domain.guestbook.application.GuestbookService;
import com.app.codemasterpiecebackend.domain.guestbook.dto.EntryDTO;
import com.app.codemasterpiecebackend.global.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.global.support.constant.HttpConstants;
import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.global.util.ActorUtil;
import com.app.codemasterpiecebackend.global.util.SliceUtil;
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
public class GuestbookV1Controller {

    private final GuestbookService guestbookService;

    // ====== CREATE ======
    @PostMapping
    public SuccessPayload<?> createGuestbookEntry(
            @RequestBody @Valid GuestbookRequest.Create body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);
        final boolean isAnon = ActorUtil.isGuest(actor);

        String displayName = isAnon ? body.guestDisplayName() : userDetails.getAppUser().nickname();
        String avatarUrl = isAnon ? body.guestImageUrl() : userDetails.getAppUser().avatarUrlSmall();

        EntryDTO dto = guestbookService.create(new GuestbookCommand.Create(
                body.content(),
                actor,
                displayName,
                avatarUrl,
                new GuestbookCommand.Create.GuestPayload(
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
                new GuestbookCommand.Slice(cursor, size)
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
            @RequestBody @Valid GuestbookRequest.Update body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        var dto = guestbookService.update(new GuestbookCommand.Update(
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
            @RequestBody(required = false) @Valid GuestbookRequest.Delete body,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestHeader(value = HttpConstants.HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var actor = ActorUtil.resolve(userDetails, clientKey);

        String guestPassword = (body != null) ? body.guestPassword() : null;

        guestbookService.delete(new GuestbookCommand.Delete(
                entryId,
                actor.elevated(),
                actor.actorId(),
                guestPassword
        ));
    }
}
