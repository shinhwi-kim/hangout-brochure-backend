package com.hangoutbrochure.command.ingestion.persistence;

import com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent.SeoulCulturalEventRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Repository
public class SeoulCulturalEventPersistence {

    private final JdbcTemplate jdbcTemplate;

    public SeoulCulturalEventPersistence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID startRun(String sourceName) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ingestion_runs (source_name, status)
                VALUES (?, 'RUNNING')
                RETURNING id
                """, UUID.class, sourceName);
    }

    public void saveDocument(String sourceName, SeoulCulturalEventRecord event) {
        jdbcTemplate.update("""
                INSERT INTO ingestion_raw_seoul_cultural_hangouts (
                    source_name, cultcode, detail_url, content_hash,
                    title, source_category, source_theme, district, place, organizer, organizer_url,
                    start_date, end_date, event_time_desc, register_date, target_audience, fee_desc,
                    is_free, ticket_type, contact, performers, program_desc, etc_desc, image_url,
                    latitude, longitude
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::numeric, ?::numeric)
                ON CONFLICT (cultcode) DO UPDATE SET
                    detail_url = EXCLUDED.detail_url,
                    content_hash = EXCLUDED.content_hash,
                    title = EXCLUDED.title,
                    source_category = EXCLUDED.source_category,
                    source_theme = EXCLUDED.source_theme,
                    district = EXCLUDED.district,
                    place = EXCLUDED.place,
                    organizer = EXCLUDED.organizer,
                    organizer_url = EXCLUDED.organizer_url,
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    event_time_desc = EXCLUDED.event_time_desc,
                    register_date = EXCLUDED.register_date,
                    target_audience = EXCLUDED.target_audience,
                    fee_desc = EXCLUDED.fee_desc,
                    is_free = EXCLUDED.is_free,
                    ticket_type = EXCLUDED.ticket_type,
                    contact = EXCLUDED.contact,
                    performers = EXCLUDED.performers,
                    program_desc = EXCLUDED.program_desc,
                    etc_desc = EXCLUDED.etc_desc,
                    image_url = EXCLUDED.image_url,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    last_seen_at = CURRENT_TIMESTAMP
                """, sourceName, event.cultcode(), event.detailUrl(), contentHash(event),
                event.title(), event.sourceCategory(), event.sourceTheme(), event.district(),
                event.place(), event.organizer(), event.organizerUrl(), event.startDate(),
                event.endDate(), event.eventTimeDesc(), event.registerDate(), event.targetAudience(),
                event.feeDesc(), event.isFree(), event.ticketType(), event.contact(),
                event.performers(), event.programDesc(), event.etcDesc(), event.imageUrl(),
                event.latitude() == null ? null : event.latitude().toPlainString(),
                event.longitude() == null ? null : event.longitude().toPlainString());
    }

    /**
     * Identifies the event itself, so two rows the source registered under different cultcodes
     * hash alike. Fields that drift between such rows, such as district or image_url, are left out.
     */
    private String contentHash(SeoulCulturalEventRecord e) {
        return sha256(String.join("|",
                normalize(e.title()), normalize(e.place()), normalize(e.organizer()),
                String.valueOf(e.startDate()), String.valueOf(e.endDate())));
    }

    /** Collapses runs of whitespace so that spacing alone never splits two equal events. */
    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public void completeRun(UUID runId, int fetchedCount, int storedCount) {
        jdbcTemplate.update("""
                UPDATE ingestion_runs
                SET status = 'SUCCEEDED', completed_at = CURRENT_TIMESTAMP,
                    fetched_count = ?, stored_count = ?
                WHERE id = ?
                """, fetchedCount, storedCount, runId);
        jdbcTemplate.update("""
                UPDATE ingestion_sources
                SET last_successful_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE source_name = (SELECT source_name FROM ingestion_runs WHERE id = ?)
                """, runId);
    }

    public void failRun(UUID runId, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE ingestion_runs
                SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP, error_message = ?
                WHERE id = ?
                """, truncate(errorMessage, 4000), runId);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
