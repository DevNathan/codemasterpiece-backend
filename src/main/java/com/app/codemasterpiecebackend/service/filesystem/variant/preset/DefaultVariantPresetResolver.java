package com.app.codemasterpiecebackend.service.filesystem.variant.preset;

import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class DefaultVariantPresetResolver implements VariantPresetResolver {

    @Override
    public List<VariantProcessCmd.Target> resolve(String preset, String contentType) {
        if (preset == null || preset.isBlank()) {
            return defaultTargets(contentType);
        }

        return switch (preset.toUpperCase()) {
            case "AVATAR"       -> avatarTargets(contentType);
            case "ICON"         -> iconTargets(contentType);
            case "BANNER"       -> bannerTargets(contentType);
            case "BLOG_DEFAULT" -> blogTargets(contentType);
            default             -> defaultTargets(contentType);
        };
    }

    // ---- preset + 미디어 타입별 분기 ----

    private List<VariantProcessCmd.Target> defaultTargets(String ct) {
        if (isImage(ct)) return List.of(
                VariantProcessCmd.Target.WEBP()
        );
        if (isVideo(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_256() // 썸네일만
        );
        return List.of(); // 기타 미지원 타입은 skip
    }

    private List<VariantProcessCmd.Target> avatarTargets(String ct) {
        if (isImage(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_256(),
                VariantProcessCmd.Target.WEBP()
        );
        if (isVideo(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_256()
        );
        return List.of();
    }

    private List<VariantProcessCmd.Target> iconTargets(String ct) {
        if (isImage(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_256()
        );
        return List.of();
    }

    private List<VariantProcessCmd.Target> bannerTargets(String ct) {
        if (isImage(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_512(),
                VariantProcessCmd.Target.WEBP(),
                VariantProcessCmd.Target.AVIF()
        );
        if (isVideo(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_512()
                // 필요하면 HLS preset 추가
                //  VariantProcessCmd.Target.HLS_1080P(),
                //  VariantProcessCmd.Target.HLS_720P()
        );
        return List.of();
    }

    private List<VariantProcessCmd.Target> blogTargets(String ct) {
        if (isImage(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_512(),
                VariantProcessCmd.Target.THUMB_256(),
                VariantProcessCmd.Target.WEBP(),
                VariantProcessCmd.Target.AVIF()
        );
        if (isVideo(ct)) return List.of(
                VariantProcessCmd.Target.THUMB_512(),
                VariantProcessCmd.Target.THUMB_256()
                // HLS 필요 시 여기
                //  VariantProcessCmd.Target.HLS_1080P(),
                //  VariantProcessCmd.Target.HLS_720P()
        );
        return List.of();
    }

    // ---- helper ----
    private boolean isImage(String ct) {
        return ct != null && ct.toLowerCase().startsWith("image/");
    }

    private boolean isVideo(String ct) {
        return ct != null && ct.toLowerCase().startsWith("video/");
    }
}
