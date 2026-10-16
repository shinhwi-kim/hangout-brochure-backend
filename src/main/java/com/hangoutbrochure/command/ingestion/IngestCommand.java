package com.hangoutbrochure.command.ingestion;

import com.hangoutbrochure.command.ingestion.source.IngestionSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.command", havingValue = "ingest")
public class IngestCommand implements ApplicationRunner {

    private final Map<String, IngestionSource> sources;

    public IngestCommand(List<IngestionSource> sourceList) {
        Map<String, IngestionSource> sourceMap = new LinkedHashMap<>();
        for (IngestionSource source : sourceList) {
            IngestionSource previous = sourceMap.put(source.sourceName(), source);
            if (previous != null) {
                throw new IllegalStateException("Duplicate ingestion source name: " + source.sourceName());
            }
        }
        this.sources = Map.copyOf(sourceMap);
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> requestedSources = args.getOptionValues("source");
        if (requestedSources == null || requestedSources.isEmpty()) {
            throw new IllegalArgumentException("Specify at least one source with --source=<source-name>.");
        }

        for (String requestedSource : requestedSources) {
            IngestionSource source = sources.get(requestedSource);
            if (source == null) {
                throw new IllegalArgumentException("Unknown ingestion source '" + requestedSource
                        + "'. Available sources: " + String.join(", ", sources.keySet()));
            }
            source.ingest();
        }
    }
}
