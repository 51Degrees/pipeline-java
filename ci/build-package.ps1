
param(
    [Parameter(Mandatory=$true)]
    [string]$RepoName,
    [string]$ProjectDir = ".",
    [string]$Name,
    [Parameter(Mandatory=$true)]
    [string]$Version,
    [Parameter(Mandatory=$true)]
    [Hashtable]$Keys
)


./java/build-package.ps1 -RepoName $RepoName -ProjectDir $ProjectDir -Name $Name -Version $Version -ExtraArgs "-DskipNativeBuild=true" -JavaGpgKeyPassphrase $Keys['JavaGpgKeyPassphrase'] -CodeSigningCert $Keys['CodeSigningCert'] -JavaPGP $Keys['JavaPGP'] -CodeSigningCertAlias $Keys['CodeSigningCertAlias'] -CodeSigningCertPassword $Keys['CodeSigningCertPassword'] -MavenSettings $Keys['MavenSettings'] 


exit $LASTEXITCODE
