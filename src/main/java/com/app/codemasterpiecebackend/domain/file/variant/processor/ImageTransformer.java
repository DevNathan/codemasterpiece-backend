package com.app.codemasterpiecebackend.domain.file.variant.processor;

import java.io.File;
import java.io.IOException;

/**
 * 물리적 이미지 변환을 담당하는 컴포넌트 규격입니다.
 * 애플리케이션의 메모리 고갈(OOM)을 방지하기 위해 전체 데이터를 메모리에 적재하는 바이트 배열 대신,
 * 물리적 임시 파일(File) 기반의 입출력을 강제합니다.
 */
public interface ImageTransformer {

    File toWebp(File original) throws IOException;

    File toAvif(File original) throws IOException;

    ResizeResult resizeWebp(File original, int maxWidth) throws IOException;

    record ResizeResult(File file, Integer width, Integer height) {
    }
}