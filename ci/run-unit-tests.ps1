param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/run-unit-tests.ps1 @PSBoundParameters
