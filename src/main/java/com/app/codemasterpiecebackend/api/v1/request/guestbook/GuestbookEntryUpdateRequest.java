package com.app.codemasterpiecebackend.api.v1.request.guestbook;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuestbookEntryUpdateRequest(
        @NotBlank(message = "validation.guestbook.content.not_blank")
        @Size(max = 2000, message = "validation.guestbook.content.max_length")
        String content,

        @Nullable @Pattern(regexp = "\\d{6}", message = "validation.guestbook.pin.pattern")
        String guestPassword
) {
}