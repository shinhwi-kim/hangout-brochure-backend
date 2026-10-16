# Hangout Brochure

서비스 구조와 향후 행사 수집 아키텍처는 [docs/architecture.md](docs/architecture.md)를 참고한다.

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

## Hangout list API

`GET /api/hangouts` returns a cursor-paginated list of hangouts from PostgreSQL.

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `categories` | string list | no | Filter by one or more stable category codes, such as `music` and `sport`; repeat the parameter for each code |
| `from` | ISO 8601 datetime | yes | Start of the required time window; include hangouts that continue until or beyond this time |
| `to` | ISO 8601 datetime | yes | End of the required time window; include hangouts that begin by this time |
| `cursor` | UUID | no | `nextCursor` returned by the previous response |
| `size` | integer | no | Items per response (default `300`, maximum `500`) |

When multiple categories are supplied, a hangout is included if it has any of them. The required time filter matches hangouts whose duration overlaps the requested window: `hangout.startsAt <= to` and `hangout.endsAt >= from`. Results are ordered by `id` ascending. Pass the returned hangout ID in `nextCursor` to fetch the next batch; it is `null` when there are no more results.

Example: `/api/hangouts?categories=music&categories=sport&from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:59Z&size=20`

Response shape:

```json
{
  "items": [
    {
      "id": "hangout-123",
      "title": "Neighborhood music day",
      "categories": ["music", "sport"],
      "startsAt": "2026-11-02T10:00:00+09:00",
      "endsAt": "2026-11-02T18:00:00+09:00",
      "locationName": "Hangang Park",
      "address": "Seoul ...",
      "latitude": 37.528,
      "longitude": 126.934
    }
  ],
  "size": 20,
  "nextCursor": "opaque-cursor-value",
  "hasNext": true
}
```

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
