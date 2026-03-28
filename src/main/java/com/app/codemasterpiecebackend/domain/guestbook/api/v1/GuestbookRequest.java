package com.app.codemasterpiecebackend.domain.guestbook.api.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

/**
 * 방명록(Guestbook) 도메인의 API 요청(Request) 규격을 통합 관리하는 레코드입니다.
 */
public record GuestbookRequest() {

    /**
     * 방명록 항목 생성 요청 규격.
     */
    public record Create(
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

    /**
     * 방명록 항목 수정 요청 규격.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Update(
            /** 수정할 본문 */
            @NotBlank(message = "validation.guestbook.content.not_blank")
            @Size(max = 2000, message = "validation.guestbook.content.max_length")
            String content,

            /** 본인 확인용 PIN 번호 */
            @Nullable 
            @Pattern(regexp = "\\d{6}", message = "validation.guestbook.pin.pattern")
            String guestPassword
    ) {
    }

    /**
     * 방명록 항목 삭제 요청 규격.
     */
    public record Delete(
            /** 본인 확인용 PIN 번호 */
            @Nullable String guestPassword
    ) {
    }
}