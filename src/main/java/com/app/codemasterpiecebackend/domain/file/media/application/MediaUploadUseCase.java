package com.app.codemasterpiecebackend.domain.file.media.application;

import com.app.codemasterpiecebackend.domain.file.core.application.StoreCmd;
import com.app.codemasterpiecebackend.domain.file.core.dto.FileInfo;

/**
 * 도메인별 미디어 파일 업로드 및 비동기 후처리 로직을 정의하는 유스케이스 인터페이스입니다.
 */
public interface MediaUploadUseCase {

    /**
     * 지정된 정책에 따라 미디어 파일을 업로드하고, 필요한 경우 후처리를 연계합니다.
     *
     * @param cmd 클라이언트의 업로드 요청 데이터가 담긴 커맨드 객체
     * @return 스토리지 및 DB에 저장 완료된 파일 메타데이터 정보
     */
    FileInfo upload(StoreCmd cmd);

    /**
     * 이미 저장된 미디어 파일에 대하여 비동기 변환(예: 모자이크 처리) 작업을 수행합니다.
     * <p>
     * 멱등성(Idempotency)을 보장하여, 이미 처리가 완료된 파일에 대해서는
     * 중복 작업을 수행하지 않고 안전하게 건너뛰어야 합니다.
     * </p>
     *
     * @param fileId 대상 파일의 고유 식별자
     */
    void process(String fileId);
}