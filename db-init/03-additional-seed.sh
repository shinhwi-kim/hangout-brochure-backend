#!/bin/sh
# The sample hangouts are off unless asked for: they sit in the same tables as ingested
# data, so leaving them on makes a real ingest hard to read.
# Sourced by the Postgres entrypoint, so no `exit` here — it would end the whole run.
if [ "${LOAD_MOCK_DATA:-false}" = "true" ]; then
  echo "LOAD_MOCK_DATA=true: loading local sample hangouts"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f /seed/mock-data.sql
else
  echo "LOAD_MOCK_DATA is not true: skipping local sample hangouts"
fi
