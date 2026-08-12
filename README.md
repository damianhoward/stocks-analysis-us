# US Equity Universe Pipeline

[![CI](https://github.com/damianhoward/stocks-analysis-us/actions/workflows/ci.yml/badge.svg)](https://github.com/damianhoward/stocks-analysis-us/actions/workflows/ci.yml)
[![CodeQL](https://github.com/damianhoward/stocks-analysis-us/actions/workflows/codeql.yml/badge.svg)](https://github.com/damianhoward/stocks-analysis-us/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/damianhoward/stocks-analysis-us/graph/badge.svg)](https://codecov.io/gh/damianhoward/stocks-analysis-us)

A Spring Boot 4 / Java 25 service that builds a **ranked US equity universe from public fundamentals**. It walks Zacks sector/industry classifications and Yahoo Finance fundamentals, applies a configurable PEG-style fundamental ranking, and exports the result as a desk-friendly Excel report.

The pipeline is structured so the signal (currently PEG) is one stage of six — swap `analysis` for a different scoring rule (e.g. EV/EBITDA decile rank, momentum, quality) and the rest of the pipeline doesn't change.

## Pipeline architecture

```
   ┌─────────────────┐   ┌───────────────────┐   ┌──────────────────┐
   │ 1. Sector       │──▶│ 2. Zacks industry │──▶│ 3. Zacks-code    │
   │    mapping      │   │    universe       │   │    enrichment    │
   └─────────────────┘   └───────────────────┘   └────────┬─────────┘
                                                            │
                                                            ▼
   ┌──────────────────┐   ┌───────────────────┐   ┌──────────────────┐
   │ 6. Excel export  │◀──│ 5. Ranking        │◀──│ 4. Fundamentals  │
   │ (JXLS template)  │   │ (PEG today)       │   │ (Yahoo + FX)     │
   └──────────────────┘   └───────────────────┘   └──────────────────┘
```

Stages talk via Spring `ApplicationEvent`s — never direct method calls. Each stage package under `src/main/java/com/damianhoward/stocks/analysis/us/` (`sectormapping`, `zacksindustry`, `zackscode`, `stocklookup`, `analysis`, `export`) owns one stage, its DTOs, its repository, and its event publisher. Adding a stage is a new package, not a rewrite.

## Pipeline design and test discipline

- **Spring event orchestration** — stages communicate via in-process `ApplicationEvent`s (synchronous, not durable messaging — see `EventManager`), so any stage can be skipped, re-run, or replaced without touching the others
- **External-data discipline** — throttled HTTP, retry-on-failure, Tika-based HTML parsing for Zacks pages, all behind a single boundary that's mocked in tests
- **Deterministic tests, guarded integrations** — the pipeline tests mock the HTTP boundary and run the same on every push; one live smoke test per external provider (Yahoo, Frankfurter) proves the real auth and response contracts still hold, with env-var off-switches (`YAHOO_LIVE_SKIP`, `FX_LIVE_SKIP`) for offline runs
- **Configurable ranking stage** — the scoring rule (currently PEG) is one Spring bean. Replacing it doesn't disturb the universe-build or the export
- **Schema-managed DB** — Flyway migrations against PostgreSQL 17; the schema is the source of truth, not the JPA entities
- **Externalised config** — per-profile YAML + env-var placeholders; nothing sensitive committed

## Prerequisites

- JDK 25 — install it yourself (e.g. `winget install Microsoft.OpenJDK.25` or `brew install openjdk@25`). The Gradle toolchain block points the build at JDK 25 if one is already on the machine but does not auto-download it.
- Docker (local Postgres via `docker compose`)

## Quick start

```bash
cp .env.example .env             # tweak if you want
docker compose up -d             # Postgres on 5432
./gradlew bootRun                # defaults to `dev` profile; runs the full pipeline
```

The app is a batch pipeline, not a web service — `bootRun` executes the stages and exits; there
is no HTTP listener.

If you have a `postgres-data` volume from before Postgres 18, the container will refuse to start:
the 18+ images keep the cluster in a version-named subdirectory and the compose file now mounts
`/var/lib/postgresql` rather than `/var/lib/postgresql/data`. The data is local scratch rebuilt by
the pipeline, so `docker compose down -v` and start again. Preserving it instead means a
`pg_upgrade`, which is not worth it for a development database.

## Running individual pipeline stages

The pipeline is event-driven: by default `bootRun` fires the first event (`ZacksSectorMappingStartEvent`) and each stage publishes the next stage's start event on completion. To start the pipeline from any other stage, pass `-Devent=<StartEventName>`:

```bash
# Full pipeline (default) — starts at sector mapping
./gradlew bootRun

# Start from a specific stage
./gradlew bootRun -Devent=ZacksListStartEvent
./gradlew bootRun -Devent=ZacksBasicStartEvent
./gradlew bootRun -Devent=StockLookupStartEvent
./gradlew bootRun -Devent=AnalysisStockStartEvent
./gradlew bootRun -Devent=ExportStartEvent

# Use a specific date for "today" (otherwise: system date)
./gradlew bootRun -Ddate=2024-06-09

# Override the Zacks reference date independently (analysis stage joins on it)
./gradlew bootRun -Devent=AnalysisStockStartEvent -Ddate=2024-06-09 -DzacksDate=2024-06-07
```

Note: stages **chain forward** from whatever event you start with, so e.g. `StockLookupStartEvent` will also run analysis + export. To run a single stage in isolation, you'd need to comment out its `@EventListener` for the completion event on the next stage.

## Configuration

| Variable                                                        | Default                                    | Purpose                                                   |
| --------------------------------------------------------------- | ------------------------------------------ | --------------------------------------------------------- |
| `DB_URL`                                                        | `jdbc:postgresql://localhost:5432/trading` | Postgres JDBC URL                                         |
| `DB_USERNAME`                                                   | `postgres`                                 |                                                           |
| `DB_PASSWORD`                                                   | `postgres`                                 |                                                           |
| `FX_PROVIDER_URL`                                               | `https://api.frankfurter.dev/v2/rates`     | FX rate source (keyless; rates quoted against EUR)        |
| `EMAIL_ENABLED`                                                 | `false`                                    | Set `true` to email the export at the end of the pipeline |
| `EMAIL_HOST` / `EMAIL_PORT`                                     | _empty_ / `587`                            | SMTP relay (only used if enabled)                         |
| `EMAIL_USERNAME` / `EMAIL_PASSWORD`                             | _empty_                                    | SMTP credentials (only used if enabled)                   |
| `EMAIL_FROM` / `EMAIL_FROM_NAME` / `EMAIL_TO` / `EMAIL_TO_NAME` | _empty_                                    | Email addresses (only used if enabled)                    |
| `SERVER_PORT`                                                   | `9000`                                     | HTTP port                                                 |
| `SPRING_PROFILES_ACTIVE`                                        | `dev`                                      | Spring profile                                            |

## Tests

```bash
./gradlew test
```

Unit tests are deterministic with the HTTP boundary mocked. The repository
integration test (`AnalysisRepositoryIntegrationTest`) spins up Postgres 17
via Testcontainers and runs the real Flyway migrations against it — it's
auto-skipped when Docker isn't reachable, so CI without Docker still passes.

## Stack

- Java 25, Spring Boot 4.1 (Data JPA + Flyway starter)
- Flyway 12.x, PostgreSQL 17 via Docker
- Apache POI + JXLS (Excel export)
- Apache Tika (HTML parsing)
- Slf4j; Lombok on the JPA-mapped types only (Hibernate needs a no-arg constructor and field
  population, so those cannot be records)
- JUnit Jupiter 6 + Mockito + Hamcrest + Testcontainers (PostgreSQL)
- Gradle 9.6

## Notes

- FX conversion uses the keyless [Frankfurter](https://frankfurter.dev) API; rates are quoted against EUR and the two legs are divided to convert any pair
- Zacks data is parsed from public pages; be respectful with throttling (`stocks.analysis.us.stocklookup.sleeptime`)
- Historical Excel templates live under `src/main/resources/template/`

## License

Apache 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
