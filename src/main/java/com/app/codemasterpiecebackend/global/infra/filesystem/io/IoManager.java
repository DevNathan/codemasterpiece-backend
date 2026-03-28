package com.app.codemasterpiecebackend.global.infra.filesystem.io;

import com.app.codemasterpiecebackend.domain.file.core.dto.FileObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 물리적 파일 입출력(I/O)을 추상화하는 시스템의 핵심 인터페이스입니다.
 * 애플리케이션 계층이 특정 스토리지 기술(Local, S3 등)에 종속되지 않도록 분리하며,
 * 대용량 트래픽 환경에서 메모리 고갈을 방지하기 위한 다이렉트 스트리밍 규칙을 강제합니다.
 */
public interface IoManager {

    /**
     * 지정된 키(경로)로 데이터 스트림을 메모리 버퍼링 없이 저장소에 직접 업로드합니다.
     *
     * @param key 업로드할 파일의 고유 식별 키 (경로 포함)
     * @param in 업로드할 데이터의 입력 스트림 (전체 파일 내용을 메모리에 올리지 않고 스트리밍 방식으로 전달)
     * @param contentLength 파일의 정확한 바이트 크기 (스트리밍 업로드를 위해 필수 요소)
     * @param contentType 파일의 MIME 타입
     * @return 저장된 파일의 메타데이터 정보
     * @throws IOException 네트워크 오류 또는 파일 처리 중 예외 발생 시
     */
    FileObjectMetadata put(String key, InputStream in, long contentLength, String contentType) throws IOException;

    /**
     * 저장소 내부에서 지정된 객체를 새로운 경로로 논리적 이동(복사 후 삭제) 처리합니다.
     *
     * @param srcKey 이동할 원본 객체의 키
     * @param dstKey 대상 객체의 새로운 키
     * @return 이동된 객체의 메타데이터 정보
     * @throws IOException 스토리지 연동 중 예외 발생 시
     */
    FileObjectMetadata move(String srcKey, String dstKey) throws IOException;

    /**
     * 지정된 키의 객체를 삭제합니다.
     * 대상 객체가 존재하지 않더라도 예외를 발생시키지 않고 멱등성을 보장합니다.
     *
     * @param key 삭제할 객체의 키
     * @return 삭제 성공 여부 (객체가 없어도 true 반환)
     * @throws IOException 스토리지 연동 중 예외 발생 시
     */
    boolean delete(String key) throws IOException;

    /**
     * 실제 저장소에 해당 객체가 존재하는지 확인하고 메타데이터만 조회합니다.
     *
     * @param key 조회할 객체의 키
     * @return 객체가 존재할 경우 메타데이터를 포함한 Optional, 존재하지 않을 경우 Optional.empty()
     * @throws IOException 스토리지 연동 중 예외 발생 시
     */
    Optional<FileObjectMetadata> head(String key) throws IOException;

    /**
     * 저장소에 있는 객체의 전체 데이터를 읽을 수 있는 입력 스트림을 엽니다.
     *
     * @param key 읽어올 객체의 키
     * @return 객체 데이터의 입력 스트림
     * @throws IOException 객체를 찾을 수 없거나 접근 중 예외 발생 시
     */
    InputStream openStream(String key) throws IOException;

    /**
     * 객체 데이터의 특정 범위(Range)만 읽어올 수 있는 입력 스트림을 엽니다.
     * 주로 미디어 파일의 부분 스트리밍에 사용됩니다.
     *
     * @param key 읽어올 객체의 키
     * @param startInclusive 읽기를 시작할 바이트 인덱스 (포함)
     * @param endExclusive 읽기를 종료할 바이트 인덱스 (미포함)
     * @return 특정 범위의 데이터를 포함하는 입력 스트림
     * @throws IOException 스토리지 연동 중 예외 발생 시 또는 범위 읽기가 지원되지 않을 시
     */
    default InputStream openRange(String key, long startInclusive, long endExclusive) throws IOException {
        throw new UnsupportedOperationException("Range read not supported");
    }

    /**
     * 클라이언트가 백엔드를 거치지 않고 저장소에서 직접 파일을 다운로드할 수 있는 임시 URL을 발급합니다.
     *
     * @param key 대상 객체의 키
     * @param ttl URL의 유효 기간
     * @return 다운로드 가능한 Presigned URL 문자열
     * @throws IOException URL 생성 중 예외 발생 시
     */
    String presignRead(String key, Duration ttl) throws IOException;

    /**
     * 클라이언트가 백엔드를 거치지 않고 저장소에 직접 파일을 업로드할 수 있는 임시 URL과 필수 헤더를 발급합니다.
     *
     * @param key 업로드될 대상 객체의 키
     * @param ttl URL의 유효 기간
     * @param contentType 업로드할 파일의 예상 MIME 타입
     * @param maxSize 허용되는 최대 파일 크기 (바이트 단위)
     * @return Presigned URL과 연관된 HTTP 헤더 정보를 포함하는 객체
     * @throws IOException URL 생성 중 예외 발생 시
     */
    PresignWriteResult presignWrite(String key, Duration ttl, String contentType, long maxSize) throws IOException;

    /**
     * 특정 접두어(Prefix)로 시작하는 경로 하위의 모든 객체를 일괄 삭제합니다.
     * 페이지네이션 단위로 처리되어 대용량 삭제 시 시스템 메모리 안전성을 보장합니다.
     *
     * @param prefix 삭제할 대상의 접두어 경로 (예: 특정 디렉토리 경로)
     * @return 삭제 처리된 총 객체 수
     * @throws IOException 스토리지 연동 중 예외 발생 시
     */
    int deletePrefix(String prefix) throws IOException;

    /**
     * 클라이언트 직접 업로드(Presigned Write)를 위한 통신 객체입니다.
     */
    record PresignWriteResult(String url, Map<String, String> headersOrFormFields) {
    }
}