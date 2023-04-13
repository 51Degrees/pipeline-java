param (
    [Parameter(Mandatory=$true)]
    [hashtable]$Options
)

$Version = $Options.Version

./java/build-package-maven.ps1 -RepoName "pipeline-java-test" -Version $Version


exit $LASTEXITCODE