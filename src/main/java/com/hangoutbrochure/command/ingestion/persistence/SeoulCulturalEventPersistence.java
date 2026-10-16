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
        String externalId = event.sourceUrl() == null || event.sourceUrl().isBlank()
                ? sha256(String.join("|", nullToEmpty(event.title()), nullToEmpty(event.period()),
                nullToEmpty(event.place()), nullToEmpty(event.organization())))
                : event.sourceUrl().trim();

        jdbcTemplate.update("""
                INSERT INTO ingestion_seoul_cultural_hangouts_raw (
                    source_name, external_id, source_url, category, district, title, event_date,
                    place, organization, target_audience, fee, inquiry, performers, program,
                    additional_info, homepage_url, image_url, application_date, ticket_type,
                    starts_on, ends_on, theme, longitude, latitude, free_or_paid, detail_url,
                    event_time, raw_payload, payload_hash
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::numeric, ?::numeric, ?, ?, ?, ?::jsonb, ?)
                ON CONFLICT (source_name, external_id) DO UPDATE SET
                    source_url = EXCLUDED.source_url,
                    category = EXCLUDED.category,
                    district = EXCLUDED.district,
                    title = EXCLUDED.title,
                    event_date = EXCLUDED.event_date,
                    place = EXCLUDED.place,
                    organization = EXCLUDED.organization,
                    target_audience = EXCLUDED.target_audience,
                    fee = EXCLUDED.fee,
                    inquiry = EXCLUDED.inquiry,
                    performers = EXCLUDED.performers,
                    program = EXCLUDED.program,
                    additional_info = EXCLUDED.additional_info,
                    homepage_url = EXCLUDED.homepage_url,
                    image_url = EXCLUDED.image_url,
                    application_date = EXCLUDED.application_date,
                    ticket_type = EXCLUDED.ticket_type,
                    starts_on = EXCLUDED.starts_on,
                    ends_on = EXCLUDED.ends_on,
                    theme = EXCLUDED.theme,
                    longitude = EXCLUDED.longitude,
                    latitude = EXCLUDED.latitude,
                    free_or_paid = EXCLUDED.free_or_paid,
                    detail_url = EXCLUDED.detail_url,
                    event_time = EXCLUDED.event_time,
                    raw_payload = EXCLUDED.raw_payload,
                    payload_hash = EXCLUDED.payload_hash,
                    last_seen_at = CURRENT_TIMESTAMP
                """, sourceName, externalId, event.sourceUrl(), event.category(), event.district(), event.title(),
                event.period(), event.place(), event.organization(), event.audience(), event.fee(), event.inquiry(),
                event.performers(), event.program(), event.description(), event.homepageUrl(), event.imageUrl(),
                event.applicationDate(), event.ticketType(), event.startsOn(), event.endsOn(), event.theme(),
                event.longitude() == null ? null : event.longitude().toPlainString(),
                event.latitude() == null ? null : event.latitude().toPlainString(), event.freeOrPaid(),
                event.sourceUrl(), event.eventTime(), event.rawPayload(), sha256(event.rawPayload()));
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
