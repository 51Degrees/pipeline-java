param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/run-update-dependencies.ps1 @PSBoundParameters

