param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/run-integration-tests.ps1 @PSBoundParameters
