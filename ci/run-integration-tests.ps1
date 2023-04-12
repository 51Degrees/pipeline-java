
param(
    [string]$ProjectDir = ".",
    [string]$Name
)

./java/run-integration-tests.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name

exit $LASTEXITCODE