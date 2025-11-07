package com.app.codemasterpiecebackend.service.post.cmd;

import org.springframework.data.domain.Pageable;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record PostSearchCmd(
        Pageable pageable,
        boolean elevated,
        String link,
        String keyword
) {
    public PostSearchCmd(Pageable pageable, boolean elevated, String link, String keyword) {
        this.pageable = pageable;
        this.elevated = elevated;
        this.link = trimToNull(link);
        this.keyword = trimToNull(keyword);
    }
}
