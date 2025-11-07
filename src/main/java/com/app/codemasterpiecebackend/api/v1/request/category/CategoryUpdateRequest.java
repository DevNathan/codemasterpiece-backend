package com.app.codemasterpiecebackend.api.v1.request.category;

import com.app.codemasterpiecebackend.service.category.cmd.CategoryUpdateCmd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

/**
 * 카테고리 수정 요청 (생성과 동일 규칙, 제공된 필드만 검증)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@Builder
public record CategoryUpdateRequest(
        /* 이름: 제공될 경우 2~20 */
        @Nullable
        @Size(min = 2, max = 20, message = "{validation.category.name.size}")
        String name,

        /*
          링크: 제공될 경우 생성과 동일 규칙
          - 소문자 알파벳/하이픈만
          - 길이 2~200
         */
        @Nullable
        @Pattern(regexp = "^[a-z-]+$", message = "{validation.category.link.pattern}")
        @Size(min = 2, max = 200, message = "{validation.category.link.size}")
        String link,

        /* 이미지: null=삭제 의도가 아님, 빈 파일은 무시됨 */
        @Nullable MultipartFile image,

        /* 이미지 삭제 플래그 */
        boolean removeImage
) {
    public CategoryUpdateCmd toCmd(String categoryId) {
        // 이미지: removeImage=true면 null로 강제
        MultipartFile effectiveImage = null;
        if (!removeImage && image != null && !image.isEmpty()) {
            effectiveImage = image;
        }

        // 링크 소문자 정규화
        String normalizedLink = null;
        if (trimToNull(link) != null) {
            normalizedLink = trimToNull(link).toLowerCase();
        }

        return new CategoryUpdateCmd(
                categoryId,
                trimToNull(name),
                normalizedLink,
                effectiveImage,
                removeImage
        );
    }
}
