
param(
    [string]$ProjectDir = ".",
    [string]$Name
)

./java/run-performance-tests.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name

exit $LASTEXITCODE
