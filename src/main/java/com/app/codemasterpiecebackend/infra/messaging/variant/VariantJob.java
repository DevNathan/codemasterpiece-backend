package com.app.codemasterpiecebackend.infra.messaging.variant;

import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import lombok.Builder;

import java.util.List;

@Builder
public record VariantJob(
        String fileId,
        String preset,                          // 선택: 상위에서 정책키만 전달
        List<VariantProcessCmd.Target> targets   // 또는 직접 타깃 전달(우선)
) {}
