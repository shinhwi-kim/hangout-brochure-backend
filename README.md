# Hangout Brochure

서비스 구조와 향후 행사 수집 아키텍처는 [docs/architecture.md](docs/architecture.md)를 참고한다.

## Configuration

`application.yaml` lists every setting the application reads and holds no secrets; a secret appears there only as a placeholder such as `${SEOUL_OPEN_API_KEY:}`.

```bash
./gradlew bootRun   # reads .env, then .env.local on top of it
```

Values are layered, each source overriding the ones below it:

| Priority | Source | Committed |
| --- | --- | --- |
| 1 | Shell environment variables | — |
| 2 | `.env.{profile}` — `local` unless `--spring.profiles.active` says otherwise | no |
| 3 | `.env` | yes |
| 4 | `application.yaml` | yes |

`.env` is the committed template: it names every secret with a blank value. Copy it and fill in the real values.

```bash
cp .env .env.local   # .env.local is gitignored
```

It is parsed as a Java properties file, so write `KEY=value` with no `export` prefix and no quotes — quotes are kept as part of the value. List only secrets that have no default, because a blank value overrides the `application.yaml` default with an empty string.

## Local PostgreSQL

```bash
docker compose up -d   # Start PostgreSQL in the background
docker compose stop    # Stop PostgreSQL and keep the container and data
docker compose down    # Remove the container; keep database data
docker compose down -v # Remove the container and database data
```

On first start with an empty database volume, `docker/postgres/init/init.sql` creates the schemas and inserts the eight categories and Seoul cultural events source definition. Docker then runs `insert-test-mock-data.sql` to add 100 local sample hangouts. These initialization scripts run only when the database volume is empty.

The default local connection is `localhost:5432`, database `hangout_brochure`, user `hangout_dev`, password `hangout_dev`. Override these with `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` environment variables. These defaults are for local development only.

`hangout_categories` has explicitly seeded integer IDs, a stable unique `code` (such as `music`), and a display `name`. `hangout_category_assignments` connects each UUIDv7 hangout ID to one or more categories by ID.

## Ingestion and transform

Ingestion stores a source's rows as they arrive; transform promotes them into the public `hangouts` table. Both run per source and are safe to repeat.

```bash
./gradlew bootRun --args='--spring.main.web-application-type=none --app.command=ingest --source=seoul-cultural-events'
./gradlew bootRun --args='--spring.main.web-application-type=none --app.command=transform --source=seoul-cultural-events'
```

Transform picks one canonical row per `content_hash`, because a source may list the same event under several ids. It then promotes rows that can satisfy the table's constraints, maps the source's own category labels through `ingestion_category_mappings`, and leaves anything it could not promote as `REVIEW` on the raw row rather than discarding it.

See [docs/architecture.md](docs/architecture.md) for the full pipeline.

## Hangout list API

`GET /api/hangouts` returns a cursor-paginated list of hangouts from PostgreSQL.

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `categories` | string list | no | Filter by one or more stable category codes, such as `music` and `sport`; repeat the parameter for each code |
| `from` | ISO 8601 date (`YYYY-MM-DD`) | yes | Start of the required window; include hangouts that run until or beyond this day |
| `to` | ISO 8601 date (`YYYY-MM-DD`) | yes | End of the required window; include hangouts that begin by this day |
| `cursor` | UUID | no | `nextCursor` returned by the previous response |
| `size` | integer | no | Items per response (default `300`, maximum `500`) |

When multiple categories are supplied, a hangout is included if it has any of them. The required date filter matches hangouts whose run overlaps the requested window: `hangout.startDate <= to` and `hangout.endDate >= from`. Only hangouts with status `PUBLISHED` are returned. Results are ordered by `id` ascending. Pass the returned hangout ID in `nextCursor` to fetch the next batch; it is `null` when there are no more results.

Example: `/api/hangouts?categories=music&categories=sport&from=2026-11-01&to=2026-11-30&size=20`

Response shape:

```json
{
  "items": [
    {
      "id": "hangout-123",
      "title": "Neighborhood music day",
      "categories": ["music", "sport"],
      "startDate": "2026-11-02",
      "endDate": "2026-11-02",
      "eventTimeDesc": "10:00 ~ 18:00",
      "locationName": "Hangang Park",
      "address": "Seoul ...",
      "district": "Yeongdeungpo-gu",
      "latitude": 37.528,
      "longitude": 126.934,
      "organizer": "Seoul Foundation for Arts and Culture",
      "imageUrl": "https://.../poster.jpg",
      "detailUrl": "https://culture.seoul.go.kr/...",
      "isFree": true,
      "feeDesc": null,
      "contact": "02-000-0000"
    }
  ],
  "size": 20,
  "nextCursor": "opaque-cursor-value",
  "hasNext": true
}
```

A hangout carries a day range, never a clock time: sources publish days, and anything finer such as `19:30` or "programme varies" arrives as free text in `eventTimeDesc`, which is for display only. Fields the source did not supply come back as `null`.

Invalid requests return `400 Bad Request` with a consistent error body:

```json
{
  "status": 400,
  "code": "INVALID_PARAMETER",
  "message": "Request parameters are invalid.",
  "errors": [
    {
      "field": "size",
      "reason": "must be greater than or equal to 1"
    }
  ],
  "path": "/api/hangouts"
}
```
