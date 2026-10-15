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
              AND (
                  :keyword IS NULL
                  OR LOCATE(LOWER(:keyword), LOWER(category.code)) > 0
                  OR LOCATE(LOWER(:keyword), LOWER(category.name)) > 0
              )
            ORDER BY category.id ASC
            """)
    List<HangoutCategoryEntity> findCategories(
            @Param("keyword") String keyword,
            @Param("cursor") Integer cursor,
            Pageable pageable
    );
}
