package com.hangoutbrochure.repository.jpa;

import com.hangoutbrochure.repository.jpa.entity.HangoutEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface HangoutJpaRepository extends JpaRepository<HangoutEntity, UUID> {

    @Query("""
            SELECT hangout.id
            FROM HangoutEntity hangout
            WHERE hangout.endsAt >= :from
              AND hangout.startsAt <= :to
              AND (:cursor IS NULL OR hangout.id > :cursor)
            ORDER BY hangout.id ASC
            """)
    List<UUID> findMatchingIds(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT hangout.id
            FROM HangoutEntity hangout
            JOIN hangout.categories category
            WHERE category.code IN :categories
              AND hangout.endsAt >= :from
              AND hangout.startsAt <= :to
              AND (:cursor IS NULL OR hangout.id > :cursor)
            ORDER BY hangout.id ASC
            """)
    List<UUID> findMatchingIdsByCategories(
            @Param("categories") List<String> categories,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT hangout
            FROM HangoutEntity hangout
            LEFT JOIN FETCH hangout.categories
            WHERE hangout.id IN :ids
            """)
    List<HangoutEntity> findWithCategoriesByIds(@Param("ids") List<UUID> ids);
}
