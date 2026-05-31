# ACIP Architecture

ACIP captures AI usage, estimates cost, attaches business context, and reports spend by team, story, epic, attribution quality, and potential waste.

```mermaid
flowchart LR
    User["Developer or AI Tool"] --> Proxy["ACIP OpenAI-Compatible Proxy"]
    Proxy --> LLM["Mock LLM, Ollama, or OpenAI-Compatible Provider"]
    Proxy --> Inference["Attribution Inference"]
    Inference --> WorkTracking["Mock Jira or Jira Cloud"]
    Proxy --> Pricing["Pricing Service"]
    Pricing --> Usage["Usage Event Persistence"]
    Inference --> Usage
    WorkTracking --> Stories["Story and Epic Tables"]
    Usage --> Reports["Reporting APIs"]
    Stories --> Reports
    Reports --> Dashboard["React Dashboard"]
```

## Runtime Components

- **Proxy API** accepts OpenAI-compatible chat completion requests wrapped with ACIP attribution metadata.
- **Attribution inference** prefers explicit story keys, then parses branch names for Jira-style keys, then records missing attribution without blocking usage.
- **Work tracking provider** validates stories and epics through Mock Jira locally or Jira Cloud when configured.
- **Pricing service** calculates estimated cost from provider/model pricing and token usage.
- **Reporting APIs** aggregate spend, attribution coverage, potential waste, and request detail.
- **Dashboard** gives operators a product-style view of allocation, attribution health, waste, and manual correction.

## Attribution Flow

```mermaid
flowchart TD
    Request["Proxy Request"] --> Explicit{"storyKey supplied?"}
    Explicit -- Yes --> ExplicitSource["Source = EXPLICIT"]
    Explicit -- No --> Branch{"branch contains key?"}
    Branch -- Yes --> Inferred["Source = INFERRED_BRANCH"]
    Branch -- No --> Missing["Source = MISSING"]
    ExplicitSource --> Validate["Validate against WorkTrackingProvider"]
    Inferred --> Validate
    Missing --> Persist["Persist usage event"]
    Validate --> Status["Set VALID, UNKNOWN_STORY, or UNKNOWN_EPIC"]
    Status --> Persist
    Persist --> Manual{"User corrects later?"}
    Manual -- Yes --> ManualSource["Source = MANUAL and audit correction"]
```

Manual correction always wins after capture. Attribution failures are visibility signals only; ACIP does not block AI usage in the current product scope.

## Local Modes

- **Mock mode** runs PostgreSQL, ACIP API, React dashboard, and the mock OpenAI-compatible LLM.
- **Ollama mode** runs the same ACIP API against Ollama's OpenAI-compatible endpoint on port `8081`.
- **Jira mode** uses the same work tracking abstraction with Jira Cloud credentials supplied through environment variables or local secrets.
