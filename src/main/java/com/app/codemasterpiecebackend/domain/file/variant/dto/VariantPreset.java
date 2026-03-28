package com.app.codemasterpiecebackend.domain.file.variant.dto;

import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantProcessCmd.Target;

import java.util.List;
import java.util.Locale;

/**
 * 파일 변환(Variant)의 목적에 따른 대상(Target)을 정의하는 프리셋 열거형입니다.
 * 미디어 타입(Image, Video 등)에 따라 생성해야 할 하위 에셋의 종류를 결정합니다.
 */
public enum VariantPreset {
    DEFAULT,
    AVATAR,
    ICON,
    BANNER,
    BLOG_DEFAULT;

    /**
     * 현재 프리셋과 콘텐츠 타입에 맞는 변환 타겟 목록을 반환합니다.
     *
     * @param contentType 파일의 MIME 타입
     * @return 생성해야 할 타겟(Target) 목록
     */
    public List<Target> resolveTargets(String contentType) {
        if (contentType == null) return List.of();
        String ct = contentType.toLowerCase(Locale.ROOT);
        boolean isImage = ct.startsWith("image/");
        boolean isVideo = ct.startsWith("video/");

        return switch (this) {
            case AVATAR -> isImage ? List.of(Target.THUMB_256(), Target.WEBP()) :
                           isVideo ? List.of(Target.THUMB_256()) : List.of();
            case ICON -> isImage ? List.of(Target.THUMB_256()) : List.of();
            case BANNER -> isImage ? List.of(Target.THUMB_512(), Target.WEBP(), Target.AVIF()) :
                           isVideo ? List.of(Target.THUMB_512()) : List.of();
            case BLOG_DEFAULT -> isImage ? List.of(Target.THUMB_512(), Target.THUMB_256(), Target.WEBP(), Target.AVIF()) :
                                 isVideo ? List.of(Target.THUMB_512(), Target.THUMB_256()) : List.of();
            case DEFAULT -> isImage ? List.of(Target.WEBP()) :
                            isVideo ? List.of(Target.THUMB_256()) : List.of();
        };
    }
}