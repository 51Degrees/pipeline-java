param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys

)

./java/publish-package-maven.ps1 -RepoName $RepoName -MavenSettings $Keys['MavenSettings'] -Version $Version
