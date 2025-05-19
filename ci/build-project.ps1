param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

Write-Host "JAVA_HOME: $ENV:JAVA_HOME"

./java/build-project.ps1 @PSBoundParameters
