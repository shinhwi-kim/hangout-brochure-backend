package com.hangoutbrochure.repository.jpa;

import com.hangoutbrochure.repository.jpa.entity.HangoutCategoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HangoutCategoryJpaRepository extends JpaRepository<HangoutCategoryEntity, Integer> {

    @Query("""
            SELECT category
            FROM HangoutCategoryEntity category
            WHERE (:cursor IS NULL OR category.id > :cursor)
            ORDER BY category.id ASC
            """)
    List<HangoutCategoryEntity> findCategories(
            @Param("cursor") Integer cursor,
            Pageable pageable
    );

    @Query("""
            SELECT category
            FROM HangoutCategoryEntity category
            WHERE (:cursor IS NULL OR category.id > :cursor)
              AND (
                  LOCATE(LOWER(CAST(:keyword AS String)), LOWER(category.code)) > 0
                  OR LOCATE(LOWER(CAST(:keyword AS String)), LOWER(category.name)) > 0
              )
            ORDER BY category.id ASC
            """)
    List<HangoutCategoryEntity> findCategoriesByKeyword(
            @Param("keyword") String keyword,
            @Param("cursor") Integer cursor,
            Pageable pageable
    );
}
