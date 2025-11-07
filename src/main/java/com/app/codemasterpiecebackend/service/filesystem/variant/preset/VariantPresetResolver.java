package com.app.codemasterpiecebackend.service.filesystem.variant.preset;

import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;

import java.util.List;

public interface VariantPresetResolver {
    List<VariantProcessCmd.Target> resolve(String preset, String contentType);
}
