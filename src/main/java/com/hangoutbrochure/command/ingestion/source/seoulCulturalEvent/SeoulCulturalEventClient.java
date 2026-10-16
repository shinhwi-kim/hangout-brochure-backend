package com.hangoutbrochure.command.ingestion.source.seoulCulturalEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SeoulCulturalEventClient {

    private static final int PAGE_SIZE = 1000;
    private static final String SERVICE_NAME = "culturalEventInfo";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public SeoulCulturalEventClient(
            ObjectMapper objectMapper,
            @Value("${ingestion.seoul.base-url:http://openapi.seoul.go.kr:8088}") String baseUrl,
            @Value("${SEOUL_OPEN_API_KEY:}") String apiKey
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey;
    }

    public List<SeoulCulturalEventRecord> fetchAll() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("SEOUL_OPEN_API_KEY must be set to use Seoul Open API.");
        }

        List<SeoulCulturalEventRecord> events = new ArrayList<>();
        int start = 1;
        int totalCount;
        do {
            int end = start + PAGE_SIZE - 1;
            JsonNode service = requestPage(start, end);
            JsonNode total = service.path("list_total_count");
            if (!total.canConvertToInt()) {
                throw new IllegalStateException("Seoul Open API response did not include list_total_count.");
            }
            totalCount = total.asInt();
            JsonNode rows = service.path("row");
            if (rows.isArray()) {
                rows.forEach(row -> events.add(toRecord(row)));
            }
            start += PAGE_SIZE;
        } while (start <= totalCount);

        return List.copyOf(events);
    }

    private JsonNode requestPage(int start, int end) {
        URI uri = URI.create(baseUrl + "/" + apiKey + "/json/" + SERVICE_NAME + "/" + start + "/" + end + "/");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Seoul Open API returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode service = root.path(SERVICE_NAME);
            if (service.isMissingNode()) {
                JsonNode result = root.path("RESULT");
                throw new IllegalStateException("Seoul Open API error: "
                        + result.path("CODE").asText("UNKNOWN") + " "
                        + result.path("MESSAGE").asText("Missing service response."));
            }
            JsonNode result = service.path("RESULT");
            if (!result.isMissingNode() && !"INFO-000".equals(result.path("CODE").asText())) {
                throw new IllegalStateException("Seoul Open API error: "
                        + result.path("CODE").asText() + " " + result.path("MESSAGE").asText());
            }
            return service;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read Seoul Open API response.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Seoul Open API request was interrupted.", e);
        }
    }

    private SeoulCulturalEventRecord toRecord(JsonNode row) {
        return new SeoulCulturalEventRecord(
                text(row, "CODENAME"), text(row, "GUNAME"), text(row, "TITLE"),
                text(row, "DATE"), text(row, "PLACE"), text(row, "ORG_NAME"),
                text(row, "USE_TRGT"), text(row, "USE_FEE"), text(row, "INQUIRY"),
                text(row, "PLAYER"), text(row, "PROGRAM"), text(row, "ETC_DESC"),
                text(row, "ORG_LINK"), text(row, "MAIN_IMG"), text(row, "RGSTDATE"),
                text(row, "TICKET"), text(row, "STRTDATE"), text(row, "END_DATE"),
                text(row, "THEMECODE"), decimal(row, "LOT"), decimal(row, "LAT"),
                text(row, "IS_FREE"), text(row, "HMPG_ADDR"), text(row, "PRO_TIME"), row.toString()
        );
    }

    private String text(JsonNode row, String field) {
        JsonNode value = row.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText().trim();
    }

    private BigDecimal decimal(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
