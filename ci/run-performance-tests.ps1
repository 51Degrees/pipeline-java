param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/run-performance-tests.ps1 @PSBoundParameters -TestName "ShareUsageOverheadTests"

exit $LASTEXITCODE
