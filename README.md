# Hangout Brochure

## Hangout list API

`GET /api/hangouts` returns a paginated list of hangouts. The endpoint currently returns an empty list until database persistence is added.

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `keyword` | string | no | Search term for a hangout title or description |
| `category` | string | no | Filter by stable category code, such as `music` |
| `from` | ISO 8601 datetime | no | Start of the time window; include hangouts that continue until or beyond this time |
| `to` | ISO 8601 datetime | no | End of the time window; include hangouts that begin by this time |
| `west`, `south`, `east`, `north` | number | no | Map viewport bounds; provide all four together |
| `cursor` | string | no | Opaque cursor returned by the previous response |
| `size` | integer | no | Items per response (default `300`, maximum `500`) |

The time filter matches hangouts whose duration overlaps the requested window: `hangout.startsAt <= to` and `hangout.endsAt >= from`. Results are ordered by `startsAt`, then `id`, both ascending. Pass the returned `nextCursor` to fetch the next batch.

Example: `/api/hangouts?category=music&from=2026-11-01T00:00:00Z&to=2026-11-30T23:59:59Z&west=126.8&south=37.4&east=127.2&north=37.7&size=20`

Response shape:

```json
{
  "items": [
    {
      "id": "hangout-123",
      "title": "Neighborhood music day",
      "categoryId": "music",
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
