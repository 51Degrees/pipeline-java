param (
    [Parameter(Mandatory=$true)]
    [string]$Version
)

./java/build-package-maven.ps1 -RepoName "pipeline-java-test" -Version $Version


exit $LASTEXITCODE