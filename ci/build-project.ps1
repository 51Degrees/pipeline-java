param(
    [string]$ProjectDir = ".",
    [string]$Name
)

./java/build-project.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name

exit $LASTEXITCODE