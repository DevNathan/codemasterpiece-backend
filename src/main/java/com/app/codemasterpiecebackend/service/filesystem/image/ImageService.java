package com.app.codemasterpiecebackend.service.filesystem.image;

import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.UploadCmd;

public interface ImageService {
    FileInfo upload(UploadCmd cmd);
}
