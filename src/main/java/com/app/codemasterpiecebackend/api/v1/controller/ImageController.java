package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.domain.dto.file.ImageUploadResponse;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.util.CdnProperties;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.image.ImageService;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.MediaKind;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.UploadCmd;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final CdnProperties cdnProperties;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<ImageUploadResponse> upload(@RequestPart("file") MultipartFile file) {
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

        var cmd = UploadCmd.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .content(supplier)
                .kind(MediaKind.IMAGE)
                .build();

        FileInfo uploaded = imageService.upload(cmd);
        String url = FileUrlResolver.toFileUrl(cdnProperties, uploaded);

        var response = ImageUploadResponse.from(uploaded, url);
        return SuccessPayload.of(response, "success.file.created");
    }
}
