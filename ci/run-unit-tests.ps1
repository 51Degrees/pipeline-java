
param(
    [string]$ProjectDir = ".",
    [string]$Name
)

./java/run-unit-tests.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name

exit $LASTEXITCODE
