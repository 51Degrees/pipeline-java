param(
    [string]$ProjectDir = "."
)

./java/run-unit-tests.ps1 -RepoName "pipeline-java" -ProjectDir $ProjectDir -Name ""

exit $LASTEXITCODE