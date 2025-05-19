param (
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$VariableName
)

./java/get-next-package-version.ps1 @PSBoundParameters
