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

Write-Output "===== GET $env:JIRA_BASE_URL/_edge/tenant_info ====="
try {
    $tenant = Invoke-RestMethod -Method Get -Uri "$env:JIRA_BASE_URL/_edge/tenant_info"
    $cloudId = $tenant.cloudId
    Write-Output "cloudId: $cloudId"
} catch {
    $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "no-status" }
    Write-Output "Status: $status"
    Write-Output $_.Exception.Message
    exit 1
}

$headers = @{
    Authorization = "Bearer $env:JIRA_API_TOKEN"
    Accept = "application/json"
}

$paths = @(
    "/rest/api/3/myself",
    "/rest/api/3/project/search",
    "/rest/api/3/project/KAN",
    "/rest/api/3/issue/KAN-9"
)

foreach ($path in $paths) {
    $url = "https://api.atlassian.com/ex/jira/$cloudId$path"
    Write-Output "===== GET $url ====="
    try {
        $response = Invoke-WebRequest -Method Get -Uri $url -Headers $headers -UseBasicParsing
        Write-Output "Status: $($response.StatusCode)"
        Write-Output $response.Content
    } catch {
        $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "no-status" }
        Write-Output "Status: $status"
        if ($_.ErrorDetails.Message) {
            Write-Output $_.ErrorDetails.Message
        } else {
            Write-Output $_.Exception.Message
        }
    }
}
