
param(
    [string]$ProjectDir = ".",
    [string]$Name,
    [string]$Version,
    [Parameter(Mandatory=$true)]
    [string]$JavaGpgKeyPassphrase,
    [Parameter(Mandatory=$true)]
    [string]$CodeSigningCert,
    [Parameter(Mandatory=$true)]
    [string]$JavaPGP,
    [Parameter(Mandatory=$true)]
    [string]$CodeSigningCertAlias,
    [Parameter(Mandatory=$true)]
    [string]$CodeSigningCertPassword,
    [Parameter(Mandatory=$true)]
    [string]$MavenSettings
)


./java/build-package.ps1 -RepoName "pipeline-java" -ProjectDir $ProjectDir -Name $Name -Version $Version -JavaGpgKeyPassphrase $JavaGpgKeyPassphrase -CodeSigningCert $CodeSigningCert -JavaPGP $JavaPGP -CodeSigningCertAlias $CodeSigningCertAlias -CodeSigningCertPassword $CodeSigningCertPassword -MavenSettings $MavenSettings


exit $LASTEXITCODE
