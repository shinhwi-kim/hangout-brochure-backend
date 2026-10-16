package com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent;

import com.hangoutbrochure.command.ingestion.source.IngestionSource;
import com.hangoutbrochure.command.ingestion.persistence.SeoulCulturalEventPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeoulCulturalEventIngestionSource implements IngestionSource {

    public static final String SOURCE_NAME = "seoul-cultural-events";
    private static final Logger log = LoggerFactory.getLogger(SeoulCulturalEventIngestionSource.class);

    private final SeoulCulturalEventClient client;
    private final SeoulCulturalEventPersistence persistence;

    public SeoulCulturalEventIngestionSource(SeoulCulturalEventClient client, SeoulCulturalEventPersistence persistence) {
        this.client = client;
        this.persistence = persistence;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public void ingest() {
        var runId = persistence.startRun(SOURCE_NAME);
        try {
            List<SeoulCulturalEventRecord> events = client.fetchAll();
            events.forEach(event -> persistence.saveDocument(SOURCE_NAME, event));
            persistence.completeRun(runId, events.size(), events.size());
            log.info("Ingested {} Seoul cultural event records (runId={}).",
                    events.size(), runId);
            events.stream().limit(5).forEach(event -> log.info(
                    "Ingested source record: title='{}', category='{}', district='{}', startsOn='{}', endsOn='{}'",
                    event.title(), event.category(), event.district(), event.startsOn(), event.endsOn()
            ));
        } catch (RuntimeException e) {
            persistence.failRun(runId, e.getMessage());
            throw e;
        }
    }
}
