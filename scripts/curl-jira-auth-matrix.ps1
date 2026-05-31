param(
    [string] $EnvFile = ".env.jira.local"
)

$ErrorActionPreference = "Stop"

Get-Content -LiteralPath $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith("#")) {
        return
    }

    $parts = $line.Split("=", 2)
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
}

$tenant = Invoke-RestMethod -Method Get -Uri "$env:JIRA_BASE_URL/_edge/tenant_info"
$cloudId = $tenant.cloudId

Write-Output "HTTP client: curl.exe"
Write-Output "Site base URL: $env:JIRA_BASE_URL"
Write-Output "Cloud ID: $cloudId"
Write-Output "Service account email: $env:JIRA_EMAIL"
Write-Output "Token source: .env.jira.local"
Write-Output ""

Write-Output "===== Direct site route: Basic auth ====="
curl.exe -i -sS -u "$env:JIRA_EMAIL`:$env:JIRA_API_TOKEN" "$env:JIRA_BASE_URL/rest/api/3/myself"
Write-Output ""

Write-Output "===== Scoped service-account route: Bearer /myself ====="
curl.exe -i -sS -H "Authorization: Bearer $env:JIRA_API_TOKEN" "https://api.atlassian.com/ex/jira/$cloudId/rest/api/3/myself"
Write-Output ""

Write-Output "===== Scoped service-account route: Bearer /project/KAN ====="
curl.exe -i -sS -H "Authorization: Bearer $env:JIRA_API_TOKEN" "https://api.atlassian.com/ex/jira/$cloudId/rest/api/3/project/KAN"
Write-Output ""

Write-Output "===== Scoped service-account route: Bearer /issue/KAN-9 ====="
curl.exe -i -sS -H "Authorization: Bearer $env:JIRA_API_TOKEN" "https://api.atlassian.com/ex/jira/$cloudId/rest/api/3/issue/KAN-9"
