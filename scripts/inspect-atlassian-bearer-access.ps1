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

$headers = @{
    Authorization = "Bearer $env:JIRA_API_TOKEN"
    Accept = "application/json"
}

try {
    $resources = Invoke-RestMethod `
        -Method Get `
        -Uri "https://api.atlassian.com/oauth/token/accessible-resources" `
        -Headers $headers

    Write-Output "Accessible resources: $(@($resources).Count)"
    @($resources) | ForEach-Object {
        Write-Output "$($_.id) | $($_.name) | $($_.url)"
    }
} catch {
    $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "no-status" }
    Write-Output "Bearer discovery failed: $status"
}
