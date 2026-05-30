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

## Quick Start

Start the default local stack:

```powershell
docker compose up --build
```

This starts:

- ACIP API at `http://localhost:8080`
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

## API

### Proxy Chat Completions

`POST /api/v1/proxy/openai/chat/completions`

With the default Docker Compose stack, the full URL is `http://localhost:8080/api/v1/proxy/openai/chat/completions`.

```json
{
  "attribution": {
    "storyKey": "ACIP-123",
    "teamKey": "PLATFORM",
    "userKey": "brian"
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

### Usage Events

`GET /api/v1/usage/events?limit=100`

Returns recent usage events, newest first. `limit` is bounded to `1..500`.

## Local Verification

Run tests with Maven:

```powershell
mvn test
```

If Maven is not on `PATH`, use the installed Maven wrapper distribution:

```powershell
& "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.15\0226a00282e400185496f3b60ec5a3f029cbdc6893912937d4876d57695224e1\bin\mvn.cmd" test
```
