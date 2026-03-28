package com.app.codemasterpiecebackend.domain.file.api.v1;

import com.app.codemasterpiecebackend.domain.file.core.application.StoreCmd;
import com.app.codemasterpiecebackend.domain.file.core.dto.FileInfo;
import com.app.codemasterpiecebackend.domain.file.media.application.ImageService;
import com.app.codemasterpiecebackend.domain.file.media.dto.ImageUploadResponse;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.global.util.CdnProperties;
import com.app.codemasterpiecebackend.global.util.FileUrlResolver;
import com.app.codemasterpiecebackend.domain.file.core.job.FileCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

/**
 * 이미지 자산의 업로드 및 시스템 관리를 담당하는 API 컨트롤러입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageV1Controller {

    private final ImageService imageService;
    private final FileCleaner fileCleaner;
    private final CdnProperties cdnProperties;

    /**
     * 클라이언트로부터 이미지 파일을 업로드 받아 처리합니다.
     *
     * @param file   업로드할 이미지 파일
     * @param preset 이미지 변환 프리셋 (BLOG_DEFAULT, AVATAR, ICON, BANNER 등)
     * @return 저장된 파일 정보 및 CDN URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<ImageUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "preset", defaultValue = "DEFAULT") String preset
    ) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "error.badrequest");
        }

        Supplier<InputStream> supplier = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        var cmd = StoreCmd.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .content(supplier)
                .profileHint(preset)
                .build();

        FileInfo uploaded = imageService.upload(cmd);
        String url = FileUrlResolver.toFileUrl(cdnProperties, uploaded);

        return SuccessPayload.of(ImageUploadResponse.from(uploaded, url), "success.file.created");
    }

    /**
     * 파일 시스템의 가비지 컬렉터(Mark & Sweep)를 수동으로 즉시 실행합니다.
     */
    @PostMapping("/admin/gc")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<String> triggerGarbageCollection() {
        log.warn("Manual File GC triggered by AUTHOR.");

        fileCleaner.nightlyCleanup();

        return SuccessPayload.of("OK", "success.file.gc.executed");
    }
}