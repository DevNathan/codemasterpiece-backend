package com.app.codemasterpiecebackend.api.v1.request.guestbook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestbookEntryCreateRequest(

        /** 본문 (최대 2000자) */
        @NotBlank(message = "validation.guestbook.content.not_blank")
        @Size(max = 2000, message = "validation.guestbook.content.max_length")
        String content,

        /* ---------- ANON 전용 ---------- */

        /** 게스트 닉네임 */
        @Size(max = 10, message = "validation.guestbook.nickname.max_length")
        String guestDisplayName,

        /** 게스트 프로필 이미지 URL */
        String guestImageUrl,

        /** 게스트 PIN (숫자 6자리) */
        @Pattern(regexp = "\\d{6}", message = "validation.guestbook.pin.pattern")
        String guestPin
) {
}