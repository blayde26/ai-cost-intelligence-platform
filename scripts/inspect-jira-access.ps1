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
    "Content-Type" = "application/json"
}

Write-Output "Project lookup: KAN"
try {
    $project = Invoke-RestMethod `
        -Method Get `
        -Uri "$env:JIRA_BASE_URL/rest/api/3/project/KAN" `
        -Headers $headers
    Write-Output "$($project.key) | $($project.name) | $($project.projectTypeKey)"
} catch {
    Write-Output "Project KAN lookup failed: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "Visible projects:"
$projects = Invoke-RestMethod `
    -Method Get `
    -Uri "$env:JIRA_BASE_URL/rest/api/3/project/search?maxResults=20" `
    -Headers $headers
@($projects.values) | ForEach-Object {
    Write-Output "$($_.key) | $($_.name) | $($_.projectTypeKey)"
}

$jqls = @(
    "project = KAN ORDER BY updated DESC",
    "created >= -365d ORDER BY updated DESC"
)

foreach ($jql in $jqls) {
    $body = @{
        jql = $jql
        maxResults = 10
        fields = @("summary", "issuetype", "project", "status", "parent")
    } | ConvertTo-Json -Depth 5

    $response = Invoke-RestMethod `
        -Method Post `
        -Uri "$env:JIRA_BASE_URL/rest/api/3/search/jql" `
        -Headers $headers `
        -Body $body

    Write-Output "JQL: $jql"
    Write-Output "Issue count returned: $(@($response.issues).Count)"
    @($response.issues) | ForEach-Object {
        Write-Output "$($_.key) | $($_.fields.issuetype.name) | $($_.fields.project.key) | $($_.fields.status.name)"
    }
}

Write-Output "Direct issue lookups:"
foreach ($issueKey in @("KAN-8", "KAN-9")) {
    try {
        $issue = Invoke-RestMethod `
            -Method Get `
            -Uri "$env:JIRA_BASE_URL/rest/api/3/issue/$issueKey?fields=summary,issuetype,project,status,parent" `
            -Headers $headers
        Write-Output "$($issue.key) | $($issue.fields.issuetype.name) | $($issue.fields.project.key) | $($issue.fields.status.name)"
    } catch {
        $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "no-status" }
        Write-Output "$issueKey lookup failed: $status"
    }
}
