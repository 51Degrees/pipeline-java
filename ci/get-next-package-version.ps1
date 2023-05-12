
param (
    [Parameter(Mandatory=$true)]
    [string]$VariableName
)

./java/get-next-package-version.ps1 -RepoName "pipeline-java" -VariableName $VariableName


exit $LASTEXITCODE
