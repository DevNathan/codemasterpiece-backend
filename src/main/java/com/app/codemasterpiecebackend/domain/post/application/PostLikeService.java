package com.app.codemasterpiecebackend.domain.post.application;

import com.app.codemasterpiecebackend.domain.post.dto.PostResult;

/**
 * 리액션(좋아요) 전담 서비스.
 * - 중복방지, 토글, 카운트 싱크 책임 분리
 */
public interface PostLikeService {

    /**
     * 좋아요 토글. (true면 좋아요 ON, false면 OFF로 맞춘다)
     */
    PostResult.Like toggle(PostCommand.Like cmd);
}
