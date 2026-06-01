param(
    [string] $ApiBaseUrl = "http://localhost:8080",
    [string] $Model = "mock-gpt-4o-mini"
)

$ErrorActionPreference = "Stop"

function Invoke-AcipCheck {
    param(
        [string] $Name,
        [scriptblock] $Check
    )

    Write-Host "Checking $Name..." -ForegroundColor Cyan
    try {
        & $Check | Out-Null
        Write-Host "PASS $Name" -ForegroundColor Green
    } catch {
        Write-Host "FAIL $Name" -ForegroundColor Red
        throw
    }
}

Invoke-AcipCheck "setup health" {
    $health = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/setup/health"
    if (-not $health.overallStatus) {
        throw "Setup health response did not include overallStatus."
    }
}

Invoke-AcipCheck "Jira connection test endpoint" {
    $jira = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/jira/connection-test"
    if (-not $jira.status) {
        throw "Jira connection test response did not include status."
    }
}

Invoke-AcipCheck "pilot readiness" {
    $readiness = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/setup/pilot-readiness"
    if ($null -eq $readiness.score) {
        throw "Pilot readiness response did not include score."
    }
}

Invoke-AcipCheck "source-control diagnostics" {
    $sourceControl = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/source-control/diagnostics"
    if (-not $sourceControl.provider) {
        throw "Source-control diagnostics response did not include provider."
    }
}

Invoke-AcipCheck "proxy chat completions" {
    $body = @{
        attribution = @{
            storyKey = "PAY-1002"
            teamKey = "payments"
            userKey = "smoke-test"
            branch = "feature/PAY-1002-payment-retry"
        }
        request = @{
            model = $Model
            messages = @(
                @{
                    role = "user"
                    content = "Smoke test ACIP."
                }
            )
        }
    } | ConvertTo-Json -Depth 8

    $response = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/api/v1/proxy/openai/chat/completions" -ContentType "application/json" -Body $body
    if (-not $response.usage.total_tokens) {
        throw "Proxy response did not include usage.total_tokens."
    }
}

Invoke-AcipCheck "usage events" {
    $events = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/usage/events?limit=5"
    if ($null -eq $events) {
        throw "Usage events response was null."
    }
}

Invoke-AcipCheck "usage CSV export" {
    $csv = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/usage/events.csv?limit=5"
    if (-not ($csv -match "id,provider,model")) {
        throw "Usage CSV export did not include expected headers."
    }
}

Invoke-AcipCheck "sample CSV download" {
    $csv = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/usage/imports/samples/minimal"
    if (-not ($csv -match "provider,model,teamKey")) {
        throw "Sample CSV did not include expected headers."
    }
}

Invoke-AcipCheck "CSV import preview" {
    $csv = @"
provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp,branch
OLLAMA,llama3.2,payments,smoke-test,4200,0.00033600,2026-05-31T12:00:00Z,feature/PAY-1002-payment-retry
"@
    $preview = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/api/v1/usage/imports/csv/preview" -ContentType "text/csv" -Body $csv
    if ($preview.importedCount -lt 1) {
        throw "CSV import preview did not identify a valid row."
    }
}

Invoke-AcipCheck "attribution coverage" {
    $coverage = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/reports/attribution-coverage"
    if ($null -eq $coverage.coveragePercent) {
        throw "Coverage response did not include coveragePercent."
    }
}

Invoke-AcipCheck "spend CSV export" {
    $csv = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/reports/spend/by-story.csv"
    if (-not ($csv -match "storyKey,storyName")) {
        throw "Spend CSV export did not include expected headers."
    }
}

Invoke-AcipCheck "outcome correlations" {
    $correlations = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/analytics/correlations"
    if (-not $correlations.interpretation) {
        throw "Correlation response did not include interpretation."
    }
}

Invoke-AcipCheck "model utilization" {
    $providers = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/analytics/model-utilization/providers"
    $models = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/api/v1/analytics/model-utilization/models"
    if ($null -eq $providers -or $null -eq $models) {
        throw "Model utilization endpoints returned null."
    }
}

Write-Host "ACIP smoke test completed successfully against $ApiBaseUrl." -ForegroundColor Green
