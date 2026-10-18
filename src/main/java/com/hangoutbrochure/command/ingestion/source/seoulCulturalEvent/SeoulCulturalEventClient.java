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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SeoulCulturalEventClient {

    private static final int PAGE_SIZE = 1000;
    private static final String SERVICE_NAME = "culturalEventInfo";
    private static final Pattern CULTCODE = Pattern.compile("cultcode=(\\d+)");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;

    public SeoulCulturalEventClient(
            ObjectMapper objectMapper,
            @Value("${ingestion.seoul.base-url}") String baseUrl,
            @Value("${ingestion.seoul.api-key}") String apiKey
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
        String detailUrl = text(row, "HMPG_ADDR");
        return new SeoulCulturalEventRecord(
                cultcode(detailUrl),
                detailUrl,
                text(row, "TITLE"),
                text(row, "CODENAME"),
                text(row, "THEMECODE"),
                text(row, "GUNAME"),
                text(row, "PLACE"),
                text(row, "ORG_NAME"),
                text(row, "ORG_LINK"),
                date(row, "STRTDATE"),
                date(row, "END_DATE"),
                text(row, "PRO_TIME"),
                date(row, "RGSTDATE"),
                text(row, "USE_TRGT"),
                text(row, "USE_FEE"),
                isFree(row),
                text(row, "TICKET"),
                text(row, "INQUIRY"),
                text(row, "PLAYER"),
                text(row, "PROGRAM"),
                text(row, "ETC_DESC"),
                text(row, "MAIN_IMG"),
                decimal(row, "LAT"),
                decimal(row, "LOT")
        );
    }

    /** The detail URL carries the only stable event id the dataset exposes. */
    private long cultcode(String detailUrl) {
        Matcher matcher = detailUrl == null ? null : CULTCODE.matcher(detailUrl);
        if (matcher == null || !matcher.find()) {
            throw new IllegalStateException("Seoul Open API row has no cultcode in HMPG_ADDR: " + detailUrl);
        }
        return Long.parseLong(matcher.group(1));
    }

    private Boolean isFree(JsonNode row) {
        return switch (String.valueOf(text(row, "IS_FREE"))) {
            case "무료" -> Boolean.TRUE;
            case "유료" -> Boolean.FALSE;
            default -> null;
        };
    }

    private LocalDate date(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException ignored) {
            return null;
        }
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
