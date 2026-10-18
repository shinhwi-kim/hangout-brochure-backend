package com.hangoutbrochure.command.transform.source.seoulCulturalEvent;

import com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent.SeoulCulturalEventIngestionSource;
import com.hangoutbrochure.command.transform.persistence.SeoulCulturalEventTransformPersistence;
import com.hangoutbrochure.command.transform.source.TransformSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeoulCulturalEventTransform implements TransformSource {

    private static final Logger log = LoggerFactory.getLogger(SeoulCulturalEventTransform.class);

    private final SeoulCulturalEventTransformPersistence persistence;

    public SeoulCulturalEventTransform(SeoulCulturalEventTransformPersistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public String sourceName() {
        return SeoulCulturalEventIngestionSource.SOURCE_NAME;
    }

    @Override
    @Transactional
    public void transform() {
        String source = sourceName();
        int promoted = persistence.promote(source);
        int removedCategories = persistence.removeStaleCategories(source);
        int assignedCategories = persistence.assignCategories(source);
        persistence.markProcessed(source);

        var summary = persistence.summary(source);
        log.info("Transformed {}: {} raw rows -> {} hangouts ({} held for review), {} category links.",
                source, summary.get("raw_rows"), summary.get("hangouts"),
                summary.get("held_for_review"), summary.get("category_links"));
        log.info("This run wrote {} hangouts, added {} category links and removed {}.",
                promoted, assignedCategories, removedCategories);
    }
}
