package com.app.codemasterpiecebackend.service.filesystem.file;

import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.service.filesystem.file.cmd.StoreCmd;
import com.app.codemasterpiecebackend.infra.filesystem.io.IoManager;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;

import java.util.Optional;

/**
 * 파일 업로드 및 메타데이터 조회를 담당하는 서비스 인터페이스.
 *
 * <p>이 서비스는 {@link IoManager}를 통해
 * 실제 파일 저장소(S3, Local 등)에 파일을 업로드하고,
 * 업로드된 파일의 메타데이터를 데이터베이스에 기록하는 역할을 맡는다.</p>
 *
 * <p>주요 책임:</p>
 * <ul>
 *   <li>파일 저장 (파일 이름, MIME 타입, 크기, 저장 위치 관리)</li>
 *   <li>저장된 파일의 메타데이터 조회</li>
 *   <li>파일 스토리지 상의 실제 상태 확인 (IoManager.head)</li>
 * </ul>
 *
 * <p>도메인 로직의 핵심은 DB 상의 {@link StoredFile} 엔티티를 중심으로 동작하며,
 * 업로드 실패/삭제는 예외로 처리한다.</p>
 */
public interface FileService {

    /**
     * 파일을 실제 저장소에 업로드하고, DB에 {@link StoredFile} 엔티티를 생성한다.
     *
     * <p>구체적인 절차:
     * <ol>
     *   <li>스토리지 경로 결정 (StoragePathStrategy 적용)</li>
     *   <li>{@link IoManager#put} 호출로 업로드</li>
     *   <li>업로드 성공 시 DB에 {@link StoredFile} 기록</li>
     *   <li>필요 시 dedup(중복 업로드 방지) 로직 포함 가능</li>
     * </ol>
     * </p>
     *
     * @param storeCmd 업로드 명령 객체(파일 스트림, 이름, MIME, 크기 등 포함)
     * @return 업로드된 파일의 {@link StoredFile} 엔티티
     * @throws java.io.IOException 업로드 중 I/O 오류 발생 시
     */
    FileInfo store(StoreCmd storeCmd);

    /**
     * 파일 ID로 DB에 저장된 {@link StoredFile} 엔티티를 조회한다.
     *
     * <p>이 메서드는 데이터베이스에 저장된 메타데이터만 조회하며,
     * 실제 스토리지 상의 파일 존재 여부는 검증하지 않는다.</p>
     *
     * @param fileId 파일의 고유 식별자
     * @return {@link StoredFile} 존재 시 Optional로 감싼 결과, 없으면 Optional.empty()
     */
    Optional<FileInfo> getFile(String fileId);

    /**
     * 파일의 “스토리지 실제 상태”를 조회한다.
     *
     * <p>이 메서드는 {@link IoManager#head}를 호출하여,
     * S3 또는 LocalStorage에 해당 키가 실제 존재하는지, 크기/타입이 무엇인지 확인할 때 사용된다.</p>
     *
     * <p>즉, DB에는 기록되어 있지만 스토리지에서 사라졌을 수도 있는 파일을 검증하는 용도다.
     * 파일 복구, 정합성 점검, 모니터링 등에서 필요할 수 있다.</p>
     *
     * @param fileId 파일의 고유 식별자
     * @return 스토리지 상에서 조회된 메타 정보가 존재하면 {@link StoredFile}, 없으면 Optional.empty()
     */
    Optional<FileInfo> getFileHeadMeta(String fileId);

}
