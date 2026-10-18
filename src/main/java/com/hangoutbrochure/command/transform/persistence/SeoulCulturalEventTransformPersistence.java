package com.hangoutbrochure.command.transform.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class SeoulCulturalEventTransformPersistence {

    /**
     * One event can reach us under several cultcodes, so content_hash picks a single canonical row
     * per event. The most recently registered row wins, since that is the source's latest take.
     */
    private static final String CANONICAL = """
            SELECT DISTINCT ON (content_hash) *
            FROM ingestion_raw_seoul_cultural_hangouts
            WHERE source_name = ?
            ORDER BY content_hash, register_date DESC NULLS LAST, cultcode DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public SeoulCulturalEventTransformPersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Rows missing coordinates or carrying an impossible date range cannot become hangouts. */
    public int promote(String sourceName) {
        return jdbcTemplate.update("""
                WITH canonical AS (""" + CANONICAL + """
                )
                INSERT INTO hangouts (
                    source_name, source_raw_id, detail_url, title, location_name, district,
                    latitude, longitude, start_date, end_date, event_time_desc,
                    organizer, organizer_url, image_url, is_free, fee_desc, contact,
                    target_audience, program_desc, performers, etc_desc
                )
                SELECT c.source_name, c.id, c.detail_url, left(c.title, 200), left(c.place, 200),
                       left(c.district, 50), c.latitude, c.longitude, c.start_date, c.end_date,
                       c.event_time_desc, left(nullif(c.organizer, '기타'), 200), c.organizer_url,
                       c.image_url, c.is_free, c.fee_desc, left(c.contact, 200),
                       left(c.target_audience, 200), c.program_desc, c.performers, c.etc_desc
                FROM canonical c
                WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL
                  AND c.start_date IS NOT NULL AND c.end_date IS NOT NULL
                  AND c.end_date >= c.start_date
                  AND c.title IS NOT NULL AND c.place IS NOT NULL
                ON CONFLICT (source_raw_id) DO UPDATE SET
                    detail_url = EXCLUDED.detail_url,
                    title = EXCLUDED.title,
                    location_name = EXCLUDED.location_name,
                    district = EXCLUDED.district,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    event_time_desc = EXCLUDED.event_time_desc,
                    organizer = EXCLUDED.organizer,
                    organizer_url = EXCLUDED.organizer_url,
                    image_url = EXCLUDED.image_url,
                    is_free = EXCLUDED.is_free,
                    fee_desc = EXCLUDED.fee_desc,
                    contact = EXCLUDED.contact,
                    target_audience = EXCLUDED.target_audience,
                    program_desc = EXCLUDED.program_desc,
                    performers = EXCLUDED.performers,
                    etc_desc = EXCLUDED.etc_desc,
                    updated_at = CURRENT_TIMESTAMP
                """, sourceName);
    }

    /** Drops assignments the mapping no longer produces, so editing the mapping converges on re-run. */
    public int removeStaleCategories(String sourceName) {
        return jdbcTemplate.update("""
                DELETE FROM hangout_category_assignments a
                USING hangouts h, ingestion_raw_seoul_cultural_hangouts r
                WHERE a.hangout_id = h.id
                  AND h.source_raw_id = r.id
                  AND h.source_name = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM ingestion_category_mappings m
                      WHERE m.source_name = r.source_name
                        AND m.source_category = r.source_category
                        AND m.category_id = a.category_id
                  )
                """, sourceName);
    }

    public int assignCategories(String sourceName) {
        return jdbcTemplate.update("""
                INSERT INTO hangout_category_assignments (hangout_id, category_id)
                SELECT h.id, m.category_id
                FROM hangouts h
                JOIN ingestion_raw_seoul_cultural_hangouts r ON r.id = h.source_raw_id
                JOIN ingestion_category_mappings m
                  ON m.source_name = r.source_name AND m.source_category = r.source_category
                WHERE h.source_name = ?
                ON CONFLICT DO NOTHING
                """, sourceName);
    }

    /**
     * A row that became a hangout is done, and so is one skipped as a duplicate. A canonical row
     * that could not be promoted is held for review rather than silently dropped.
     */
    public int markProcessed(String sourceName) {
        return jdbcTemplate.update("""
                WITH canonical AS (""" + CANONICAL + """
                )
                UPDATE ingestion_raw_seoul_cultural_hangouts r
                SET processing_status = CASE
                        WHEN EXISTS (SELECT 1 FROM hangouts h WHERE h.source_raw_id = r.id) THEN 'PROCESSED'
                        WHEN NOT EXISTS (SELECT 1 FROM canonical c WHERE c.id = r.id) THEN 'PROCESSED'
                        ELSE 'REVIEW'
                    END
                WHERE r.source_name = ?
                """, sourceName, sourceName);
    }

    public Map<String, Object> summary(String sourceName) {
        return jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT count(*) FROM ingestion_raw_seoul_cultural_hangouts WHERE source_name = ?) AS raw_rows,
                    (SELECT count(*) FROM ingestion_raw_seoul_cultural_hangouts
                      WHERE source_name = ? AND processing_status = 'REVIEW') AS held_for_review,
                    (SELECT count(*) FROM hangouts WHERE source_name = ?) AS hangouts,
                    (SELECT count(*) FROM hangout_category_assignments a
                      JOIN hangouts h ON h.id = a.hangout_id WHERE h.source_name = ?) AS category_links
                """, sourceName, sourceName, sourceName, sourceName);
    }
}
