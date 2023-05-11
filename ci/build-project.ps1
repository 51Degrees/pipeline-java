param(
    [string]$ProjectDir = ".",
    [string]$Name
)

Write-Output "JAVA_HOME :$ENV:JAVA_HOME"

./java/build-project.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name

exit $LASTEXITCODE
