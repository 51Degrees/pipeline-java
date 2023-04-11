
param(
    [Parameter(Mandatory=$true)]
    [string]$JavaSDKEnvVar
)

Write-Host "Setting up $JavaSDKEnvVar"

Write-Host $env:JAVA_HOME

# Verify that the correct version of Java is being used
java -version
