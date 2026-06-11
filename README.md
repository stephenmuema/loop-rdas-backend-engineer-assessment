# Reference Data Aggregation Service (RDAS)

LOOP DFS Backend Engineer assessment. RDAS is the single source of truth for
country, currency, language and geographical reference data. It exposes clean
REST/JSON APIs to every channel (mobile, web, partner APIs, ops portals) while
consuming the third-party CountryInfo SOAP service internally - so no channel
ever talks SOAP, sees inconsistent responses, or holds SOAP credentials.

- Source SOAP WSDL: `http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?WSDL`
- Stack: Java 17, Spring Boot 3.2, Maven, Spring-WS, Spring Data JPA, Caffeine, Resilience4j, Micrometer/Prometheus.

---

## 1. The core idea: a materialized catalog

The SOAP service has no single operation that can search, filter, paginate and
sort countries, and calling it per request is slow, rate-limited and fragile.
RDAS therefore **materializes a complete local catalog of all countries** and
serves every read from it.

```
                       request path (zero SOAP calls)
  channels ---> REST API ---> Caffeine cache ---> MySQL catalog
                                                       ^
                                                       | upserts
                       refresh path (the only SOAP)    |
  scheduler / startup ---> Harvester ---> CountryInfo SOAP service
                           (rate-limited, circuit-broken, retried)
```

A background **Harvester** is the only component that calls SOAP. It runs on a
schedule (daily) and once on startup if the catalog is empty or stale. Every
read API is served from the catalog plus an in-process cache, so the request
path makes **zero SOAP calls**. This single decision answers the performance,
rate-limit, consistency and resilience requirements at once.

How the catalog is built in one pass (about 244 countries):

```
ListOfContinentsByName        -> 6 continents      (reference list + code->name map)
ListOfCurrenciesByName        -> 174 currencies    (reference list + code->name map)
ListOfLanguagesByName         -> 407 languages     (reference list)
ListOfCountryNamesGroupedByContinent -> the full set of country ISO codes (seed)
FullCountryInfo(iso) x N       -> capital, phone, continent, currency, flag, languages
```

---

## 2. Architecture and design decisions (Part 1)

| Decision | Rationale / trade-off |
|---|---|
| Materialized catalog in a database, reads never hit SOAP | Turns a slow, rate-limited, single-record SOAP API into a fast, filterable, paginated local dataset. Trade-off: data is eventually consistent with the source (refreshed on a schedule) - acceptable because country reference data changes very rarely. |
| Background harvester is the only SOAP caller | Confines the SOAP dependency to one place, decouples request latency from upstream latency, and lets a SOAP outage degrade gracefully (reads keep working). |
| `FullCountryInfo` per country for enrichment | One call returns capital, phone, continent, currency and languages together - far fewer calls than the granular per-attribute operations. |
| Two-layer caching: DB catalog + Caffeine | The DB is the durable cache of SOAP data; Caffeine absorbs repeated identical queries in-process. Caffeine is evicted wholesale after each successful harvest. |
| Resilience4j rate limiter on the SOAP client | Hard guarantee that RDAS stays under the provider's 100 req/min quota, independent of how the harvest is paced. |
| ISO code as the public identifier | Country reference data is public and non-sensitive, so a readable natural key is the right choice (no IDOR concern - unlike user-owned records). The numeric primary key stays internal. |
| Spring-WS `WebServiceTemplate` + hand-written JAXB | Strongly-typed SOAP payloads with no code-generation step in the build. |
| Stateless app, externalised config | Every replica is interchangeable behind a Service/HPA; 12-factor config via env vars; same image across environments. |
| SOAP health excluded from the readiness probe | A SOAP outage must not eject pods that can still serve the catalog. |

A fuller write-up with the architecture diagram is in
[`docs/architecture.md`](docs/architecture.md).

---

## 3. API design (Part 2)

Base path `/api/v1`. Full reference with examples: [`docs/api-documentation.md`](docs/api-documentation.md).

| Method | Path | Purpose |
|---|---|---|
| GET | `/countries` | Search by `name`; filter by `continent`, `currency`, `language`; `page`, `size`, `sort`. Returns a paged envelope. |
| GET | `/countries/{isoCode}` | Full detail for one country (incl. languages). |
| GET | `/countries/{isoCode}/currency-peers` | Countries sharing that country's currency. |
| GET | `/continents` | Reference list of continents. |
| GET | `/currencies` | Reference list of currencies. |
| GET | `/languages` | Reference list of languages. |
| GET | `/admin/catalog/status` | Catalog counts, last refresh, staleness. |
| POST | `/admin/catalog/refresh` | Trigger an out-of-band refresh (202, async, single-flight). |

Example:

```bash
# Search Africa + French-speaking, page 1, 10 per page, sorted by name desc
curl "http://localhost:8080/api/v1/countries?continent=AF&language=French&page=0&size=10&sort=name,desc"

# One country
curl http://localhost:8080/api/v1/countries/KE

# Who else uses Kenya's currency
curl http://localhost:8080/api/v1/countries/KE/currency-peers
```

Paged response envelope:

```json
{
  "content": [ { "isoCode": "KE", "name": "Kenya", "capitalCity": "Nairobi",
                 "continentCode": "AF", "continentName": "Africa",
                 "currencyCode": "KES", "currencyName": "Shillings",
                 "flagUrl": "http://.../Kenya.jpg" } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1,
  "first": true, "last": true, "sort": "name: ASC"
}
```

---

## 4. Running the application

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **17** | Required. Lombok used here does not build on newer JDKs (24+). |
| Maven | 3.9+ | Wrapper-free; uses local `mvn`. |
| Docker | recent | For the container / compose path. |
| Outbound internet | - | The harvester calls the public SOAP service. |

> JDK 17 on macOS Homebrew: `export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home` (path varies; the Cellar path also works).

### Option A - Maven, embedded H2 (zero infrastructure)

The default profile uses a file-backed H2 database, so the catalog survives
restarts with nothing to install.

```bash
mvn spring-boot:run
# App on http://localhost:8080
# On first start the catalog is empty and a background harvest begins; it takes
# about 2-3 minutes to load ~244 countries (rate-limited under 100 req/min).
```

Watch it fill, then query:

```bash
curl http://localhost:8080/api/v1/admin/catalog/status
curl "http://localhost:8080/api/v1/countries?continent=Africa&size=5"
```

### Option B - runnable jar

```bash
mvn clean package
java -jar target/reference-data-aggregation-service-1.0.0.jar
```

### Option C - Docker Compose (app + MySQL)

```bash
cp .env.example .env
docker compose up --build -d
docker compose logs -f rdas        # watch the harvest
curl http://localhost:8080/actuator/health
```

### Option D - Kubernetes

See [`docs/kubernetes-deployment-guide.md`](docs/kubernetes-deployment-guide.md).
Quick version:

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml -f k8s/secret.yaml
kubectl apply -f k8s/mysql-deployment.yaml      # or k8s/external-mysql.yaml for an external DB
kubectl apply -f k8s/deployment.yaml -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml -f k8s/ingress.yaml
```

### Smoke test

```bash
BASE=http://localhost:8080
curl -s "$BASE/api/v1/countries?name=ken"          # search
curl -s "$BASE/api/v1/countries/KE"                # detail
curl -s "$BASE/api/v1/countries/KE/currency-peers" # currency peers
curl -s "$BASE/api/v1/continents"                  # reference list
curl -s "$BASE/actuator/health"                    # health
```

---

## 5. Tests and quality

```bash
mvn verify
```

- 49 tests (JUnit 5, Mockito, MockMvc, `@DataJpaTest`): SOAP client mapping and
  fallbacks, harvester (success / SOAP-down / single-flight / partial-failure),
  query service, specification-based filtering against H2, full web-layer
  integration, and the global exception handler.
- JaCoCo line-coverage gate at 80% (build fails below it); achieved ~97% line.
- Postman collection under [`postman/`](postman/).

---

## 6. Data processing - reducing SOAP traffic (Part 3)

Provider limit: **100 requests/minute**.

- **What reduces SOAP traffic:** the read path makes **zero** SOAP calls -
  every search/filter/detail/peer/reference query is served from the catalog
  and Caffeine. SOAP is touched only by the scheduled harvester.
- **What is cached, and where:**
  - *Durable cache (MySQL catalog):* all country records and the
    continent/currency/language reference lists. This is the serving substrate.
  - *In-process cache (Caffeine):* hot read results - search pages, country
    detail by ISO, currency peers, reference lists.
- **Expiration:** Caffeine entries expire after 30 minutes (a safety net). The
  catalog itself does not expire; it is refreshed, not invalidated by age.
- **Refresh strategy:** a full harvest runs daily (cron) and once on startup if
  the catalog is empty or older than 24h. After a successful harvest the
  Caffeine caches are evicted wholesale so the next reads pick up fresh data.
  Harvesting is paced (about 120ms between calls) and rate-limited to 90/min,
  comfortably under the 100/min quota. A full refresh is roughly 4 list calls +
  ~244 detail calls and completes in 2-3 minutes.
- **Justification:** reference data changes on the order of days/years, not
  seconds. Serving from a periodically refreshed local copy trades a small,
  bounded staleness for a massive reduction in SOAP calls, sub-millisecond reads
  and complete decoupling from upstream availability.

---

## 7. Resilience - SOAP unavailable for 6 hours (Part 4)

- **What happens when a request arrives:** nothing changes for read APIs - they
  are served from the catalog and never call SOAP. Users see normal responses.
- **How users experience the failure:** transparently. The only effect is that
  data stops being refreshed; existing data keeps serving. `/admin/catalog/status`
  and the `catalog` health component expose `stale=true` once the data ages past
  the threshold, but responses remain `200 OK`.
- **Fallback mechanisms:**
  - The catalog is persisted, so even a cold start during the outage serves the
    last harvested data (the startup harvest fails in the background without
    affecting serving).
  - The SOAP client is wrapped with Resilience4j: **retry** (3 attempts,
    exponential backoff), explicit **connect/read timeouts** (5s/10s), a
    **circuit breaker** that opens after sustained failures and fast-fails
    further attempts, and **fallbacks** that turn failures into a clean
    `SoapIntegrationException` instead of a hung thread.
  - Harvests are single-flight and idempotent (upsert by ISO), so retried or
    overlapping refreshes are safe.
- **Monitoring and alerting:** `/actuator/health` (catalog `DOWN` if empty,
  `stale=true` if old; circuit-breaker state), `/actuator/prometheus`
  (Resilience4j circuit-breaker/retry/rate-limiter metrics, HTTP metrics). Alert
  on: circuit breaker `OPEN`, `last_refresh_successful=false`, catalog age >
  threshold, and harvest failure count.

Readiness is intentionally **not** tied to SOAP, so a 6-hour outage never ejects
a serving pod.

---

## 8. Implementation checklist (Part 5)

| Requirement | Where |
|---|---|
| Pagination | Spring Data `Pageable` (`page`, `size`), capped at 100; `PagedResponse` envelope. |
| Sorting | `sort=field,dir`; whitelisted sortable fields; unknown field -> 400. |
| Filtering | `JpaSpecificationExecutor` + `CountrySpecifications` (name/continent/currency/language). |
| Caching | Spring Cache + Caffeine, evicted after each harvest. |
| Global error handling | `@RestControllerAdvice` -> structured `ErrorResponse` (400/404/502/500). |
| Input validation | Bean Validation on the search request and ISO path pattern. |

---

## 9. Engineering discussion (Part 6)

**Q1 - If the provider limit dropped to 10 requests/minute.**
The read path is unaffected (it never calls SOAP). Only the harvest must adapt:
lower the rate limiter to <=10/min and stretch the harvest over time. A full
244-country harvest at 10/min is ~25 minutes, so I would (a) run it off-peak and
less often, (b) make harvesting incremental/resumable - persist progress and
harvest in small batches across scheduled windows (e.g. a slice every few
minutes), prioritising never-seen or oldest records, and (c) keep serving stale
data meanwhile. Because data rarely changes, slower refresh is acceptable. The
ISO-keyed upsert already makes partial, resumable harvests safe.

**Q2 - Scaling to 20 million requests/day.**
That is ~230 req/s average, with peaks several times higher - all on the read
path, which is already SOAP-free. I would: run multiple stateless replicas
behind the Service/HPA (the app is built for this); add a shared cache tier
(Redis) in front of/alongside Caffeine so the hot working set is served from
memory cluster-wide and the DB is shielded; scale reads on the database with
read replicas (the catalog is tiny - a few hundred rows - so most queries are
trivially cacheable and indexed); put a CDN/edge cache on the reference lists
and popular lookups (they are effectively static between refreshes); and keep
the single harvester as a leader-elected singleton job so SOAP load stays
constant regardless of replica count. The catalog's small, static nature makes
this read-scaling straightforward.

**Q3 - With another week.**
Add Redis as a distributed cache with per-entry TTLs and cache-warming after
harvest; make the harvester incremental and resumable with per-country
checkpoints and change detection; add OpenAPI/Swagger UI generated from the
controllers; add authentication/authorisation on the admin endpoints (and an
API gateway with per-channel rate limiting); add a Testcontainers MySQL
integration suite and contract tests for the SOAP bindings; add distributed
tracing (correlation IDs through MDC into logs and traces); ship Grafana
dashboards and alert rules for the metrics above; and add a small admin UI to
inspect catalog freshness and trigger refreshes.

---

## 10. Repository layout

```
src/main/java/com/loop/rdas
  soap/        JAXB bindings for the CountryInfo operations
  client/      CountryInfoSoapClient (rate-limited, retried, circuit-broken)
  service/     CatalogHarvestService (writes), CountryQueryService (reads), CatalogStatus
  model/       Country, CountryLanguage, ReferenceItem (JPA)
  repository/  Spring Data repositories (+ JpaSpecificationExecutor)
  spec/        CountrySpecifications (dynamic filtering)
  dto/         request/response records, PagedResponse envelope
  controller/  CountryController, ReferenceController, AdminController
  exception/   GlobalExceptionHandler + error types
  config/      SOAP/cache/async wiring, health indicator, startup harvester
k8s/           Kubernetes manifests (+ external-mysql.yaml)
docs/          architecture, API docs, K8s deploy & troubleshooting guides (md + pdf)
postman/       Postman collection + environment
Dockerfile, docker-compose.yml
```
