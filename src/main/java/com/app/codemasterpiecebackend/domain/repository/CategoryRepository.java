package com.app.codemasterpiecebackend.domain.repository;

import com.app.codemasterpiecebackend.domain.dto.category.CategorySitemapLinkDTO;
import com.app.codemasterpiecebackend.domain.entity.category.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    @Query("""
            select coalesce(max(c.sortOrder), 0)
              from Category c
             where ( :parentId is null and c.parent is null )
                or ( c.parent.id = :parentId )
            """)
    int findMaxOrder(@Param("parentId") String parentId);

    @Query("select c.level from Category c where c.id = :id")
    Optional<Integer> findLevelById(@Param("id") String id);

    @Query("""
                select distinct c
                  from Category c
                  left join fetch c.image
                  left join fetch c.parent p
                 order by c.level asc, c.sortOrder asc
            """)
    List<Category> findAllFlatOrderByLevelThenSort();

    // 동일 부모 내 이름 중복 (자기 자신 제외)
    @Query("""
                select case when count(c)>0 then true else false end
                  from Category c
                 where ( :parentId is null and c.parent is null or c.parent.id = :parentId )
                   and c.name = :name
                   and c.id <> :selfId
            """)
    boolean existsByParentIdAndName(@Param("parentId") String parentId,
                                    @Param("name") String name,
                                    @Param("selfId") String selfId);

    // 형제들 잠금 후 정렬용 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select c
                  from Category c
                 where ( :parentId is null and c.parent is null ) or ( c.parent.id = :parentId )
                 order by c.sortOrder asc
            """)
    List<Category> findSiblingsForUpdate(@Param("parentId") String parentId);

    // 자기 하위인지(사이클 방지) — ancestor가 candidate의 조상인가?
    @Query(value = """
            with recursive r as (
                select category_id, parent_id
                  from tbl_category
                 where category_id = :candidate
                union all
                select c.category_id, c.parent_id
                  from tbl_category c
                  join r on c.category_id = r.parent_id
            )
            select exists(select 1 from r where category_id = :ancestor)
            """, nativeQuery = true)
    boolean isDescendantOf(@Param("candidate") String candidate,
                           @Param("ancestor") String ancestor);

    // 서브트리 전체 id (이동 시 level 일괄 업데이트용)
    @Query(value = """
            with recursive r as (
                select category_id
                  from tbl_category
                 where category_id = :root
                union all
                select c.category_id
                  from tbl_category c
                  join r on c.parent_id = r.category_id
            )
            select category_id from r
            """, nativeQuery = true)
    List<String> findSubtreeIds(@Param("root") String root);

    // level += delta 일괄
    @Modifying
    @Query(value = """
            update tbl_category
               set level = level + :delta
             where category_id in (:ids)
            """, nativeQuery = true)
    int bulkBumpLevels(@Param("ids") List<String> ids, @Param("delta") int delta);

    // 부모 기준 재시퀀스(윈도우 함수 사용)
    @Modifying
    @Query(value = """
            with ordered as (
                select category_id,
                       row_number() over(order by sort_order, category_id) as rn
                  from tbl_category
                 where ( :parentId is null and parent_id is null )
                    or ( parent_id = :parentId )
            )
            update tbl_category t
               set sort_order = o.rn * :gap
              from ordered o
             where t.category_id = o.category_id
            """, nativeQuery = true)
    int resequenceByParent(@Param("parentId") String parentId, @Param("gap") int gap);

    // 단건 FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Category c where c.id = :id")
    Optional<Category> findByIdForUpdate(@Param("id") String id);

    boolean existsByLink(String link);

    @Query("""
        select new com.app.codemasterpiecebackend.domain.dto.category.CategorySitemapLinkDTO(
            c.link,
            coalesce(max(p.updatedAt), current_timestamp)
        )
        from Category c
        left join Post p
           on p.category.id = c.id
          and p.published = true
        where c.type = com.app.codemasterpiecebackend.domain.entity.category.CategoryType.LINK
          and c.link is not null
        group by c.id, c.name, c.link
        order by c.sortOrder asc, c.id asc
    """)
    List<CategorySitemapLinkDTO> findSitemapLinks();
}
