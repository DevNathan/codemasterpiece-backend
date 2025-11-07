package com.app.codemasterpiecebackend.service.filesystem.variant;

import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import com.app.codemasterpiecebackend.support.exception.VariantProcessException;

/**
 * 파일 변환(variant 생성)을 담당하는 프로세서 인터페이스.
 *
 * <p>이미지, 비디오, 오디오 등 다양한 파일 타입별로 변형(variants)을 생성할 수 있으며,
 * 원본 파일을 읽어 리사이징, 포맷 변환, 썸네일 추출 등의 작업을 수행한다.</p>
 *
 * <p>각 구현체는 자신의 도메인에 맞는 처리를 수행한다:</p>
 * <ul>
 *   <li>{@code ImageVariantProcessor} - WebP, AVIF, thumbnail 등 이미지 변환</li>
 *   <li>{@code VideoVariantProcessor} - MP4, HLS, thumbnail 등 비디오 변환</li>
 * </ul>
 *
 * <p>보통 이 프로세서는 비동기 워커에서 호출된다.</p>
 */
public interface VariantProcessor {

    /**
     * 원본 파일을 받아 변형(variants)을 생성한다.
     *
     * <p>구체적 흐름:</p>
     * <ol>
     *   <li>IoManager를 통해 원본 파일을 InputStream으로 가져온다.</li>
     *   <li>요청된 변형 사양({@link VariantProcessCmd})을 기반으로 각 variant를 생성한다.</li>
     *   <li>생성된 variant를 IoManager에 저장하고, FileService를 통해 DB에 반영한다.</li>
     * </ol>
     *
     * @param originalFile 원본 파일 엔티티 (DB 상 정보)
     * @param processCmd   변환 명세 (타입, 타겟 크기, 포맷 등)
     * @throws VariantProcessException 변환 도중 I/O 또는 처리 실패 시
     */
    void process(StoredFile originalFile, VariantProcessCmd processCmd) throws VariantProcessException;

    /**
     * 현재 프로세서가 처리 가능한 파일 타입인지 확인한다.
     *
     * <p>예를 들어, {@code ImageVariantProcessor}는 {@code image/*} MIME 타입만 처리하도록 한다.</p>
     *
     * @param contentType 파일의 MIME 타입
     * @return 처리 가능 여부
     */
    boolean supports(String contentType);
}
