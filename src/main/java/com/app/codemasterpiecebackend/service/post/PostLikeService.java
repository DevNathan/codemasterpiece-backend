package com.app.codemasterpiecebackend.service.post;

import com.app.codemasterpiecebackend.domain.dto.post.PostLikeResultDTO;
import com.app.codemasterpiecebackend.service.post.cmd.PostLikeCmd;

/**
 * 리액션(좋아요) 전담 서비스.
 * - 중복방지, 토글, 카운트 싱크 책임 분리
 */
public interface PostLikeService {

    /**
     * 좋아요 토글. (true면 좋아요 ON, false면 OFF로 맞춘다)
     */
    PostLikeResultDTO toggle(PostLikeCmd cmd);
}
