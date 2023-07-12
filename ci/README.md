# API Specific CI/CD Approach
This API complies with the `common-ci` approach.

The following secrets are required:
* `ACCESS_TOKEN` - GitHub [access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#about-personal-access-tokens) for cloning repos, creating PRs, etc.
    * Example: `github_pat_l0ng_r4nd0m_s7r1ng`

The following secrets are required for publishing releases (this should only be needed by 51Degrees):
* `JAVA_STAGING_SETTINGS_BASE64` - Base64 encoded settings.xml file which points to the Sonatype staging repo.
* `JAVA_GPG_KEY_PASSPHRASE` - Passphrase string for the 51Degrees GPG key used to sign releases
* `CODE_SIGNING_CERT` - String containing the 51Degrees code signing certificate in PFX format
* `JAVA_KEY_PGP_FILE` - String containing the 51Degrees PGP key
* `CODE_SIGNING_CERT_ALIAS` - Name of the 51Degrees code signing certificate alias
* `CODE_SIGNING_CERT_PASSWORD` - Password for the `CODE_SIGNING_CERT`
