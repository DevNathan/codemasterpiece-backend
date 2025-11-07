package com.app.codemasterpiecebackend.domain.dto.file;

import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;

/**
 * 이미지 업로드 성공 응답 DTO.
 * 컨트롤러에서 FileInfo + CDN URL 조합 결과를 담는다.
 */
public record ImageUploadResponse(
        String fileId,
        String url,
        String contentType,
        long byteSize
) {
    public static ImageUploadResponse from(FileInfo info, String url) {
        return new ImageUploadResponse(
                info.fileId(),
                url,
                info.contentType(),
                info.byteSize()
        );
    }
}
