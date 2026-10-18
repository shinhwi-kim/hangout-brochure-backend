package com.hangoutbrochure.command.transform;

import com.hangoutbrochure.command.transform.source.TransformSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.command", havingValue = "transform")
public class TransformCommand implements ApplicationRunner {

    private final Map<String, TransformSource> sources;

    public TransformCommand(List<TransformSource> sourceList) {
        Map<String, TransformSource> sourceMap = new LinkedHashMap<>();
        for (TransformSource source : sourceList) {
            TransformSource previous = sourceMap.put(source.sourceName(), source);
            if (previous != null) {
                throw new IllegalStateException("Duplicate transform source name: " + source.sourceName());
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
            TransformSource source = sources.get(requestedSource);
            if (source == null) {
                throw new IllegalArgumentException("Unknown transform source '" + requestedSource
                        + "'. Available sources: " + String.join(", ", sources.keySet()));
            }
            source.transform();
        }
    }
}
