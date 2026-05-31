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

$pair = "$env:JIRA_EMAIL`:$env:JIRA_API_TOKEN"
$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
$headers = @{
    Authorization = "Basic $auth"
    Accept = "application/json"
}

$paths = @(
    "/rest/api/3/myself",
    "/rest/api/3/project/search",
    "/rest/api/3/project/KAN",
    "/rest/api/3/issue/KAN-9"
)

foreach ($path in $paths) {
    $url = "$env:JIRA_BASE_URL$path"
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
