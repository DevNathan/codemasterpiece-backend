package com.app.codemasterpiecebackend.service.filesystem.variant.cmd;

import com.app.codemasterpiecebackend.domain.entity.file.FileVariantKind;
import lombok.Builder;

import java.util.List;

@Builder
public record VariantProcessCmd(
        List<Target> targets
) {
    @Builder
    public record Target(
            FileVariantKind kind,
            Integer width,   // THUMB류에서 사용
            Integer height   // 보통 null(비율유지)
    ) {
        public static Target WEBP() { return Target.builder().kind(FileVariantKind.WEBP).build(); }
        public static Target AVIF() { return Target.builder().kind(FileVariantKind.AVIF).build(); }
        public static Target THUMB_512() { return Target.builder().kind(FileVariantKind.THUMB_512).width(512).build(); }
        public static Target THUMB_256() { return Target.builder().kind(FileVariantKind.THUMB_256).width(256).build(); }
    }
}
