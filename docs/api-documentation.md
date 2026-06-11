---
title: "RDAS - API Documentation"
subtitle: "Reference Data Aggregation Service - REST/JSON API"
author: "Stephen Muema - LOOP DFS Backend Engineer"
---

# RDAS API Documentation

Base URL: `http://localhost:8080` (local) - all endpoints are under `/api/v1`.
All responses are JSON. No authentication is required for the read APIs in this
assessment build; the admin endpoints are flagged for auth in production.

## Conventions

- **Pagination:** `page` (0-based, default 0), `size` (default 20, max 100).
- **Sorting:** `sort=field,dir` where `dir` is `asc` or `desc`. Repeatable.
  Sortable fields: `name`, `isoCode`, `capitalCity`, `continentCode`,
  `continentName`, `currencyCode`, `currencyName`. An unknown field returns 400.
- **Errors:** a consistent envelope (see end of document).

---

## GET /api/v1/countries

Search and filter the country catalog. All filters are optional and combine with
logical AND.

| Query param | Description | Example |
|---|---|---|
| `name` | Partial, case-insensitive match on country name | `name=ken` |
| `continent` | Continent code or name | `continent=AF` or `continent=Africa` |
| `currency` | Currency ISO code or name | `currency=USD` |
| `language` | Language code or name | `language=French` |
| `page`, `size` | Pagination | `page=0&size=10` |
| `sort` | Sorting | `sort=name,desc` |

**Request**

```bash
curl "http://localhost:8080/api/v1/countries?continent=AF&language=French&page=0&size=2&sort=name,asc"
```

**Response `200 OK`**

```json
{
  "content": [
    {
      "isoCode": "BJ",
      "name": "Benin",
      "capitalCity": "Porto Novo",
      "continentCode": "AF",
      "continentName": "Africa",
      "currencyCode": "XOF",
      "currencyName": "Francs",
      "flagUrl": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Benin.jpg"
    },
    {
      "isoCode": "BF",
      "name": "Burkina Faso",
      "capitalCity": "Ouagadougou",
      "continentCode": "AF",
      "continentName": "Africa",
      "currencyCode": "XOF",
      "currencyName": "Francs",
      "flagUrl": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Burkina_Faso.jpg"
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 16,
  "totalPages": 8,
  "first": true,
  "last": false,
  "sort": "name: ASC"
}
```

---

## GET /api/v1/countries/{isoCode}

Full detail for a single country, including the languages spoken.

`isoCode` is a 2-3 letter country code (case-insensitive).

**Request**

```bash
curl http://localhost:8080/api/v1/countries/KE
```

**Response `200 OK`**

```json
{
  "isoCode": "KE",
  "name": "Kenya",
  "capitalCity": "Nairobi",
  "phoneCode": "254",
  "continentCode": "AF",
  "continentName": "Africa",
  "currencyCode": "KES",
  "currencyName": "Shillings",
  "flagUrl": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg",
  "languages": [ { "code": "swa", "name": "Swahili" } ],
  "sourceRefreshedAt": "2026-06-11T11:40:12.308374Z"
}
```

**Errors:** `404` if no such country; `400` if `isoCode` is not a 2-3 letter code.

---

## GET /api/v1/countries/{isoCode}/currency-peers

Countries that use the same currency as the given country (the given country
included), sorted by name.

**Request**

```bash
curl http://localhost:8080/api/v1/countries/US/currency-peers
```

**Response `200 OK`**

```json
[
  { "isoCode": "EC", "name": "Ecuador", "capitalCity": "Quito",
    "continentCode": "AM", "continentName": "The Americas",
    "currencyCode": "USD", "currencyName": "Dollars", "flagUrl": "http://.../Ecuador.jpg" },
  { "isoCode": "US", "name": "United States", "capitalCity": "Washington",
    "continentCode": "AM", "continentName": "The Americas",
    "currencyCode": "USD", "currencyName": "Dollars", "flagUrl": "http://.../United_States.jpg" }
]
```

---

## Reference lists

### GET /api/v1/continents

```bash
curl http://localhost:8080/api/v1/continents
```

```json
[ { "code": "AF", "name": "Africa" }, { "code": "AS", "name": "Asia" } ]
```

### GET /api/v1/currencies

```json
[ { "code": "KES", "name": "Shillings" }, { "code": "USD", "name": "Dollars" } ]
```

### GET /api/v1/languages

```json
[ { "code": "swa", "name": "Swahili" }, { "code": "eng", "name": "English" } ]
```

---

## Operations / admin

### GET /api/v1/admin/catalog/status

```json
{
  "countryCount": 244,
  "continentCount": 6,
  "currencyCount": 174,
  "languageCount": 407,
  "lastRefreshStartedAt": "2026-06-11T11:39:57.263Z",
  "lastRefreshCompletedAt": "2026-06-11T11:42:26.696Z",
  "lastRefreshSuccessful": true,
  "lastError": null,
  "refreshInProgress": false,
  "stale": false
}
```

### POST /api/v1/admin/catalog/refresh

Triggers an asynchronous full refresh. Returns `202 Accepted` with the current
status. Concurrent triggers are coalesced (single-flight).

```bash
curl -X POST http://localhost:8080/api/v1/admin/catalog/refresh
```

---

## Health and metrics

| Endpoint | Purpose |
|---|---|
| `GET /actuator/health` | Aggregate health (catalog, db, circuit breaker). |
| `GET /actuator/health/readiness` | Kubernetes readiness (db + readinessState; SOAP excluded). |
| `GET /actuator/health/liveness` | Kubernetes liveness. |
| `GET /actuator/metrics` | Micrometer metrics. |
| `GET /actuator/prometheus` | Prometheus scrape (HTTP + Resilience4j metrics). |

---

## Error envelope

Every handled error returns this structure with the appropriate HTTP status:

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid sort property 'hack'. Allowed: [name, isoCode, ...]",
  "path": "/api/v1/countries",
  "fieldErrors": [ { "field": "name", "message": "name filter must be at most 100 characters" } ]
}
```

| Status | When |
|---|---|
| 400 | Validation failure, bad sort field, bad parameter type, malformed ISO code |
| 404 | Country not found |
| 502 | SOAP integration failure (only reachable via the admin/harvest path) |
| 500 | Unexpected server error (no stack trace leaked) |
