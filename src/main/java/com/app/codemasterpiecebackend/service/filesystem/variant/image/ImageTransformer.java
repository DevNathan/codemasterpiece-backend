package com.app.codemasterpiecebackend.service.filesystem.variant.image;

import java.io.IOException;

public interface ImageTransformer {
  byte[] toWebp(byte[] original) throws IOException;
  byte[] toAvif(byte[] original) throws IOException;

  ResizeResult resizeWebp(byte[] original, int maxWidth) throws IOException;

    record ResizeResult(byte[] bytes, Integer width, Integer height) {}
}
