
param(
    [string]$ProjectDir = ".",
    [string]$Name
)

./java/build-package-requirements.ps1 -RepoName "pipeline-java-test" -ProjectDir $ProjectDir -Name $Name 


exit $LASTEXITCODE
