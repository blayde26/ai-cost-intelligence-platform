# ACIP Demo Walkthrough

This walkthrough is designed for a local product demo with no external Jira or paid LLM account required.

## Start The Stack

```powershell
docker compose up --build
```

Open:

```text
http://localhost:5173
```

The default stack starts PostgreSQL, ACIP, the React dashboard, seeded demo data, and a mock OpenAI-compatible LLM.

## Demo The Core Story

1. Open the dashboard overview.
2. Show total spend, token volume, allocation buckets, attribution coverage, and potential waste.
3. Navigate to **Epic Analysis** and show spend grouped by epic and story.
4. Navigate to **Attribution Health** and show invalid events.
5. Navigate to **Usage Explorer** and open a request detail panel.

## Generate Explicitly Attributed Usage

```powershell
$Api = "http://localhost:8080"

Invoke-RestMethod `
  -Method Post `
  -Uri "$Api/api/v1/proxy/openai/chat/completions" `
  -ContentType application/json `
  -Body '{
    "attribution": {
      "storyKey": "PAY-1001",
      "teamKey": "payments",
      "userKey": "brian",
      "repository": "ai-cost-intelligence-platform",
      "branch": "feature/PAY-1001-checkout-tax",
      "commitHash": "abc123"
    },
    "request": {
      "model": "mock-gpt-4o-mini",
      "messages": [
        {
          "role": "user",
          "content": "Summarize the implementation plan."
        }
      ]
    }
  }'
```

Expected result:

- Usage is stored.
- Attribution source is `EXPLICIT`.
- Story and epic are attached.

## Generate Branch-Inferred Usage

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$Api/api/v1/proxy/openai/chat/completions" `
  -ContentType application/json `
  -Body '{
    "attribution": {
      "teamKey": "payments",
      "userKey": "brian",
      "repository": "ai-cost-intelligence-platform",
      "branch": "feature/PAY-1002-payment-retry",
      "commitHash": "def456"
    },
    "request": {
      "model": "mock-gpt-4o-mini",
      "messages": [
        {
          "role": "user",
          "content": "Draft acceptance criteria."
        }
      ]
    }
  }'
```

Expected result:

- ACIP infers `PAY-1002` from the branch name.
- Attribution source is `INFERRED_BRANCH`.
- Confidence is `HIGH`.
- Usage remains editable through manual correction.

## Generate Missing Attribution

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$Api/api/v1/proxy/openai/chat/completions" `
  -ContentType application/json `
  -Body '{
    "attribution": {
      "teamKey": "platform",
      "userKey": "brian",
      "branch": "main"
    },
    "request": {
      "model": "mock-gpt-4o-mini",
      "messages": [
        {
          "role": "user",
          "content": "Write a short summary."
        }
      ]
    }
  }'
```

Expected result:

- Usage is not blocked.
- Attribution source is `MISSING`.
- The event appears in attribution health and unattributed reporting.

## Correct Attribution In The UI

1. Open **Usage Explorer**.
2. Select a missing or unknown request.
3. In **Manual Attribution**, enter a story key such as `PAY-1001`.
4. Enter `correctedBy`.
5. Save.

Expected result:

- Event status becomes `MANUAL`.
- Coverage and spend reports treat the event as attributed.
- The correction remains visible in request detail.
