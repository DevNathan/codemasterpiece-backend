package com.app.codemasterpiecebackend.service.post;

import com.app.codemasterpiecebackend.domain.dto.post.*;
import com.app.codemasterpiecebackend.service.post.cmd.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * PostService
 *
 * <p>CRUD 기준으로 메서드 순서를 정리했다.</p>
 *
 * <ul>
 *   <li><b>Create</b> : {@link #create(PostCreateCmd)}</li>
 *   <li><b>Read</b>   : {@link #getDetail(PostDetailCmd)}, {@link #getAll(PostSearchCmd)}, {@link #getEditById(String)}</li>
 *   <li><b>Update</b> : {@link #update(PostUpdateCmd)}</li>
 *   <li><b>Delete</b> : {@link #delete(String)}</li>
 * </ul>
 */
public interface PostService {

    // ----------------------------- C: Create -----------------------------

    /**
     * 게시글을 생성한다.
     *
     * @param cmd 생성 커맨드
     * @return 생성된 게시글의 slug
     */
    String create(PostCreateCmd cmd);

    // ------------------------------ R: Read ------------------------------

    /**
     * 게시글 상세(렌더링용)를 조회한다.
     *
     * @param cmd 상세 조회 커맨드
     * @return 상세 DTO
     */
    PostDetailDTO getDetail(PostDetailCmd cmd);

    /**
     * 게시글 목록(페이지네이션)을 조회한다.
     *
     * @param cmd 검색/정렬/페이징 커맨드
     * @return 페이지 결과
     */
    Page<PostListDTO> getAll(PostSearchCmd cmd);

    /**
     * 에디트 화면을 위한 편집용 데이터를 조회한다.
     *
     * @param postId 게시글 ID
     * @return 편집 DTO
     */
    PostEditDTO getEditById(String postId);

    List<PostSitemapDTO> getSitemaps();

    // ---------------------------- U: Update -----------------------------

    /**
     * 게시글을 수정한다.
     *
     * @param cmd 수정 커맨드
     * @return 수정 결과(식별자/slug 등)
     */
    PostUpdateResultDTO update(PostUpdateCmd cmd);

    // ---------------------------- D: Delete -----------------------------

    /**
     * 게시글을 삭제한다.
     * <p>파일 참조(FileRef), 좋아요, 태그/퀴즈, 댓글 등 연관 리소스가 도메인 규칙에 맞게 정리되어야 한다.</p>
     *
     * @param postId 게시글 ID
     */
    void delete(String postId);
}
