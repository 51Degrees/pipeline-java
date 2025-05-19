param(
    [Parameter(Mandatory)][string]$RepoName,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][Hashtable]$Keys,
    [string]$Name
)

./java/build-package.ps1 `
    -RepoName $RepoName `
    -Name $Name `
    -Version $Version `
    -ExtraArgs "-DskipNativeBuild=true" `
    -JavaGpgKeyPassphrase $Keys['JavaGpgKeyPassphrase'] `
    -JavaPGP $Keys['JavaPGP'] `
    -CodeSigningKeyVaultName: $Keys['CodeSigningKeyVaultName'] `
    -CodeSigningKeyVaultUrl $Keys['CodeSigningKeyVaultUrl'] `
    -CodeSigningKeyVaultClientId $Keys['CodeSigningKeyVaultClientId'] `
    -CodeSigningKeyVaultTenantId $Keys['CodeSigningKeyVaultTenantId'] `
    -CodeSigningKeyVaultClientSecret $Keys['CodeSigningKeyVaultClientSecret'] `
    -CodeSigningKeyVaultCertificateName $Keys['CodeSigningKeyVaultCertificateName'] `
    -CodeSigningKeyVaultCertificateData $Keys['CodeSigningKeyVaultCertificateData']
