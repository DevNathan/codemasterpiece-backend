package com.app.codemasterpiecebackend.api.v1.request.category;

import com.app.codemasterpiecebackend.domain.entity.category.CategoryType;
import com.app.codemasterpiecebackend.service.category.cmd.CategoryCreateCmd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

/**
 * 카테고리 생성 요청 (record, i18n, 조건부 검증)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
@Builder
public record CategoryCreateRequest(

        /* 이름: 2~20, 공백 불가 */
        @NotBlank(message = "validation.category.name.notBlank")
        @Size(min = 2, max = 20, message = "{validation.category.name.size}")
        String name,

        /** 유형: 필수 */
        @NotNull(message = "validation.category.type.notNull")
        CategoryType type,

        /* 부모 ID: 선택 (ULID 29자 쓰면 패턴 추가 가능) */
        String parentId,

        /*
          외부 링크: 선택
          - 소문자 알파벳 + 하이픈만
          - 길이 제한(선택): 2~200
          - 단, type == LINK 인 경우 필수 → 아래 @AssertTrue로 강제
         */
        @Pattern(regexp = "^[a-z-]+$", message = "validation.category.link.pattern")
        @Size(min = 2, max = 200, message = "{validation.category.link.size}")
        String link,

        /* 이미지: 선택 */
        MultipartFile image
) {
    /**
     * 애플리케이션 계층에 전달할 커맨드 변환
     */
    public CategoryCreateCmd toCmd() {
        MultipartFile effectiveImage = (image != null && !image.isEmpty()) ? image : null;

        return new CategoryCreateCmd(
                name,
                type,
                parentId,
                link,
                effectiveImage
        );
    }
}
