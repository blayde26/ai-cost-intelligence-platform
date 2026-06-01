# AI Cost Intelligence Platform

ACIP is an AI FinOps platform that attributes AI usage and estimated spend to engineering work.

## Sprint 1 Scope

- Spring Boot backend on Java 21
- PostgreSQL schema managed by Flyway
- OpenAI-compatible chat completions proxy
- Docker Compose runtime with PostgreSQL and a mock LLM
- Optional local Ollama target through its OpenAI-compatible endpoint
- Token usage and estimated cost capture
- AI usage event persistence
- Basic usage history API

## Sprint 2 Scope

- Work tracking provider abstraction with Mock Jira for local development and Jira Cloud for configured environments
- Story and epic sync into ACIP reporting tables
- Spend reporting by story and epic
- Attribution status on every usage event
- Cost-based attribution coverage report
- Unattributed spend list and summary APIs
- Realistic local demo data for teams, epics, stories, and 2,000+ usage events

## Sprint 3 Scope

- React and TypeScript dashboard
- Material UI operational interface
- Allocation-first overview for revenue features, operations, research, unattributed spend, and potential waste
- Stable reporting DTOs for story, epic, team, coverage, allocation, and waste reports
- Date-window support for reporting APIs through `startDate` and `endDate`
- Spend tables by story, epic, and team
- Attribution health and potential waste dashboard pages
- Recent request list and request detail inspection

## Sprint 4 Scope

- Manual attribution correction from the request detail dashboard
- Correction API for assigning usage to a story, epic, team, and work type after capture
- Correction history table for auditability
- Corrected events are surfaced as `MANUAL` attribution without blocking AI usage

## Usage Capture Expansion

- Usage capture is modeled separately from usage attribution.
- Proxy traffic is recorded through a `UsageCaptureProvider` path with source `PROXY`.
- Manual CSV imports are supported for evaluating ACIP without rerouting AI traffic.
- Imported usage is normalized into the same `AIUsageEvent` model used by reports and attribution correction.

## Strategic Expansion Direction

ACIP is expanding from AI cost attribution toward AI investment intelligence. The roadmap now separates three platform layers:

- Usage capture: proxy, CSV import, OpenAI usage import, Claude Code analytics, GitHub Copilot usage, and other provider imports.
- Attribution: Jira, manual correction, branch inference, future PR/repository/user-pattern inference.
- Outcome analytics: Jira outcomes, GitHub outcomes, team snapshots, repository metrics, and correlation dashboards.

Near-term work prioritizes low-friction adoption: setup health, CSV import, and import-first evaluation workflows before deeper outcome analytics.

The first outcome analytics slices add read-only team and repository snapshots. These reports are intentionally framed as correlation signals, not proof that AI caused delivery movement.

Repository outcome metrics are provider-backed. Local development defaults to mock GitHub-style repository metrics so ACIP can show PR, commit, review, and merge-time signals without source-control credentials. A future GitHub provider can replace the mock provider through configuration without changing the reporting API.

## Sprint 5 Scope

- GitHub Actions CI for backend tests and frontend build
- Architecture documentation and local demo walkthrough
- Source metadata capture for repository, branch, and commit
- Branch-name story key inference when explicit story attribution is missing
- Attribution source visibility for explicit, inferred, manual, and missing attribution

## Quick Start

Start the default local stack:

```powershell
docker compose up --build
```

This starts:

- ACIP API at `http://localhost:8080`
- ACIP dashboard at `http://localhost:5173`
- PostgreSQL at `localhost:5432`
- Mock OpenAI-compatible LLM at `http://localhost:8090`

Send application requests to ACIP on port `8080`. Port `8090` is the mock upstream service that ACIP calls internally.

Smoke test the proxy:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/proxy/openai/chat/completions `
  -ContentType application/json `
  -Body '{
    "attribution": {
      "storyKey": "ACIP-123",
      "teamKey": "PLATFORM",
      "userKey": "brian"
    },
    "request": {
      "model": "mock-gpt-4o-mini",
      "messages": [
        {
          "role": "user",
          "content": "Summarize this story."
        }
      ]
    }
  }'
```

View persisted usage events:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/usage/events
```

## Ollama Mode

Ollama supports an OpenAI-compatible `/v1/chat/completions` endpoint, so ACIP can run against it by changing only provider configuration.

Start ACIP with the Ollama profile:

```powershell
docker compose --profile ollama up --build postgres ollama app-ollama
```

Pull a model into the Ollama container before sending requests:

```powershell
docker compose exec ollama ollama pull llama3.2
```

Send requests to the Ollama-backed ACIP instance on port `8081` and use model `llama3.2`.

## Runtime Configuration

The default runtime expects PostgreSQL:

| Variable | Default |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/acip` |
| `DATABASE_USERNAME` | `acip` |
| `DATABASE_PASSWORD` | `acip` |
| `LLM_PROVIDER` | `OPENAI` |
| `OPENAI_API_KEY` | empty |
| `OPENAI_REQUIRE_API_KEY` | `true` |
| `OPENAI_CHAT_COMPLETIONS_URL` | `https://api.openai.com/v1/chat/completions` |

Compose overrides these values so the default stack uses `MOCK_LLM` with no API key requirement.

Sprint 2 adds these configuration values:

| Variable | Default |
| --- | --- |
| `WORK_TRACKING_PROVIDER` | `mock` |
| `SOURCE_CONTROL_PROVIDER` | `mock` |
| `SOURCE_CONTROL_ORGANIZATION` | empty |
| `GITHUB_API_BASE_URL` | `https://api.github.com` |
| `GITHUB_TOKEN` | empty |
| `GITHUB_REPOSITORIES` | empty |
| `SOURCE_CONTROL_CACHE_TTL` | `5m` |
| `DEMO_DATA_ENABLED` | `false` |
| `DEMO_USAGE_EVENT_COUNT` | `2000` |
| `JIRA_BASE_URL` | empty |
| `JIRA_EMAIL` | empty |
| `JIRA_API_TOKEN` | empty |
| `JIRA_DEFAULT_JQL` | `project is not EMPTY ORDER BY updated DESC` |

Docker Compose enables demo data by default, using the mock work tracking provider.

Jira and GitHub credentials should be supplied through environment variables or a local secret manager. Do not commit API tokens to the repository.

For live GitHub repository outcome metrics, set:

```powershell
$env:SOURCE_CONTROL_PROVIDER = "github"
$env:GITHUB_TOKEN = "github_pat_or_token"
$env:GITHUB_REPOSITORIES = "owner/repo:team-key,owner/another-repo:platform"
```

`GITHUB_REPOSITORIES` accepts comma-separated `owner/repository` values with an optional `:teamKey` suffix. When GitHub is not configured, ACIP continues to run and reports source-control readiness as a warning only if the GitHub provider is explicitly selected.

## API

### Proxy Chat Completions

`POST /api/v1/proxy/openai/chat/completions`

With the default Docker Compose stack, the full URL is `http://localhost:8080/api/v1/proxy/openai/chat/completions`.

```json
{
  "attribution": {
    "storyKey": "ACIP-123",
    "teamKey": "PLATFORM",
    "userKey": "brian",
    "repository": "ai-cost-intelligence-platform",
    "branch": "feature/ACIP-123-cost-capture",
    "commitHash": "abc123"
  },
  "request": {
    "model": "gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": "Summarize this story."
      }
    ]
  }
}
```

The OpenAI response body and status are returned to the caller. ACIP stores usage metadata, token counts, estimated cost, latency, attribution, and a SHA-256 request hash.

If `storyKey` is missing, ACIP attempts to infer a Jira-style story key from `branch`, such as `feature/PAY-1001-checkout`. Inferred attribution is stored with source `INFERRED_BRANCH` and remains manually correctable.

### Usage Events

`GET /api/v1/usage/events?limit=100`

Returns recent usage events, newest first. `limit` is bounded to `1..500`.

`GET /api/v1/usage/events/{id}`

Returns one usage event for request detail inspection.

`PATCH /api/v1/usage/events/{id}/attribution`

Manually assigns or corrects a usage event after capture. At least one of `storyKey` or `teamKey` must be supplied, and `correctedBy` is required. If `storyKey` matches a known story, ACIP derives the story's epic and work type.

```json
{
  "storyKey": "PAY-1001",
  "epicKey": "PAY-1000",
  "teamKey": "payments",
  "workType": "CAPITALIZED",
  "correctedBy": "brian",
  "note": "Manual cleanup after missing attribution tag"
}
```

The response is the updated usage event with `attributionStatus` set to `MANUAL`, `attributionCorrected` set to `true`, and correction metadata populated.

### CSV Usage Import

`POST /api/v1/usage/imports/csv`

Accepts raw `text/csv` and imports rows into canonical usage events. Required headers:

- `provider`
- `model`
- `userKey`
- `requestTimestamp`

`teamKey` is required unless the row can be resolved to a known story. Optional headers include:

- `storyKey`
- `epicKey`
- `teamKey`
- `promptTokens`
- `completionTokens`
- `totalTokens`
- `estimatedCostUsd`
- `environment`
- `workType`
- `requestStatus`
- `repository`
- `branch`
- `commitHash`
- `initiativeKey`
- `initiativeName`

Example:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/usage/imports/csv `
  -ContentType 'text/csv' `
  -Body @'
provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp,branch
OLLAMA,llama3.2,payments,brian,4200,0.00033600,2026-05-31T12:00:00Z,feature/PAY-1002-payment-retry
'@
```

The import response includes `importedCount`, `skippedCount`, and row-level errors. Import failures do not block valid rows.

### Setup Health

`GET /api/v1/setup/health`

Returns readiness signals for the local database, work tracking provider, Jira configuration, LLM proxy, pricing rows, demo data, CSV imports, source-control outcome provider, and planned outcome analytics.

`GET /api/v1/source-control/diagnostics`

Returns non-secret source-control diagnostics for the selected provider, including configured repository count, whether a token is present, metric snapshot count, cache state, and per-repository PR/commit/review timing metrics. Tokens are never returned by this endpoint.

The dashboard **Setup** tab uses this endpoint and includes a CSV import panel for pilot users.

### Outcome Analytics

`GET /api/v1/analytics/outcomes`

`GET /api/v1/analytics/team-effectiveness`

`GET /api/v1/analytics/repositories`

`GET /api/v1/analytics/correlations`

Returns initial outcome analytics snapshots from persisted usage and work tracking data:

- AI spend by team.
- Story counts, completion rate, cancellation rate, and work mix by team.
- AI spend, token volume, attribution coverage, PR counts, commit counts, review counts, comments, and merge/review timing by repository.
- Mock GitHub-style repository metrics in local mode so outcome dashboards are useful before live source-control integration.
- Optional live GitHub repository metrics when `SOURCE_CONTROL_PROVIDER=github`, `GITHUB_TOKEN`, and `GITHUB_REPOSITORIES` are configured.
- Correlation signals that compare AI spend with team completion rates and repository delivery metrics without claiming causation.

The dashboard **Outcomes** tab displays these snapshots and correlation-oriented diagnostics.

### Jira Sync

`POST /api/v1/jira/sync`

Syncs epics and stories from the configured work tracking provider into reporting tables. With `WORK_TRACKING_PROVIDER=mock`, this loads the local demo stories and epics. With `WORK_TRACKING_PROVIDER=jira`, Jira credentials must be configured.

Optional request body:

```json
{
  "jql": "project = KAN ORDER BY updated DESC"
}
```

### Spend Reports

Report endpoints support optional `startDate` and `endDate` query parameters. Values may be ISO dates such as `2026-06-01` or ISO date-times such as `2026-06-01T12:00:00Z`.

`GET /api/v1/reports/overview`

`GET /api/v1/reports/spend/by-story`

`GET /api/v1/reports/spend/by-epic`

`GET /api/v1/reports/spend/by-team`

`GET /api/v1/reports/allocation`

`GET /api/v1/reports/potential-waste`

Returns persisted usage spend totals, allocation buckets, potential waste observations, and grouped spend by story, epic, or team. Aggregation happens in backend report services so the dashboard can consume stable DTOs.

### Attribution Coverage

`GET /api/v1/reports/attribution-coverage`

Returns total cost, valid attributed cost, unattributed cost, event counts, and cost-based coverage percentage.

### Unattributed Spend

`GET /api/v1/reports/unattributed`

Optional query parameters:

- `fromDate`
- `toDate`
- `teamKey`
- `userKey`
- `attributionStatus`
- `sort`: `cost_desc`, `tokens_desc`, or `recent`

`GET /api/v1/reports/unattributed/summary`

Returns total unattributed cost, tokens, event count, and cost breakdown by attribution status.

## Frontend Development

The dashboard lives in `frontend/`.

```powershell
cd frontend
npm install
npm run dev
```

The Vite dev server runs at `http://localhost:5173` and proxies `/api` calls to `http://127.0.0.1:8080`.

When testing the Ollama-backed API on port `8081`, point the dev proxy at that instance:

```powershell
$env:VITE_API_PROXY_TARGET = "http://127.0.0.1:8081"
npm run dev
```

Build the frontend:

```powershell
cd frontend
npm run build
```

## Demo And Architecture

- [Architecture](docs/architecture.md)
- [Demo walkthrough](docs/demo-walkthrough.md)

## Local Verification

Run tests with Maven:

```powershell
mvn test
```

If Maven is not on `PATH`, use the installed Maven wrapper distribution:

```powershell
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" test
```

To run the opt-in Jira Cloud integration test, provide Jira settings from your shell or secret manager first:

```powershell
$env:JIRA_BASE_URL = "https://your-site.atlassian.net"
$env:JIRA_EMAIL = "your-service-account@example.com"
$env:JIRA_API_TOKEN = "<from your secret manager>"
$env:JIRA_DEFAULT_JQL = "project = KAN ORDER BY updated DESC"
$env:JIRA_EXPECTED_STORY_KEY = "<optional known story key>"
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" -Dtest=JiraWorkTrackingProviderIntegrationTest test
```

For local-only testing, you can also create an ignored `.env.jira.local` file with the same variables and run:

```powershell
.\scripts\run-jira-integration-test.ps1
```
