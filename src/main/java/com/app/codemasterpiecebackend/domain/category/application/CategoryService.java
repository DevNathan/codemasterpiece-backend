package com.app.codemasterpiecebackend.domain.category.application;

import com.app.codemasterpiecebackend.domain.category.dto.CategoryDTO;
import com.app.codemasterpiecebackend.domain.category.dto.CategorySitemapLinkDTO;

import java.util.List;

/**
 * 카테고리 도메인 서비스.
 *
 * <p>순서: Create → Read → Update → Move → Delete</p>
 */
public interface CategoryService {

    /** 카테고리를 생성한다(정렬값 갭 전략, 이미지 업로드/연결 포함). */
    void create(CategoryCommand.Create cmd);

    /** 전체 트리를 조회한다(정렬 포함). */
    List<CategoryDTO> getTree();

    List<CategorySitemapLinkDTO> getSitemapLinks();

    /** 카테고리를 수정한다(이름/링크/이미지 교체 및 포스트 링크 축 동기화). */
    void update(CategoryCommand.Update cmd);

    /** 카테고리를 이동한다(사이클 방지, 갭 없으면 리시퀀스 후 재계산). */
    void move(CategoryCommand.Move cmd);

    /** 카테고리를 삭제한다(자식/사용중이면 409, 이미지 detach 처리). */
    void delete(String categoryId);
}
