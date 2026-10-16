package com.hangoutbrochure.command.ingestion.source;

/** A source-specific ingestion task selectable by the one-shot ingest command. */
public interface IngestionSource {

    String sourceName();

    void ingest();
}
