
param(
    [Parameter(Mandatory=$true)]
    [string]$JavaSDKEnvVar
)

Write-Host "Setting up $JavaSDKEnvVar"

Write-Host $currentPath

# Set the JAVA_HOME environment variable
[Environment]::SetEnvironmentVariable('JAVA_HOME', [Environment]::GetEnvironmentVariable($JavaSDKEnvVar))

# Add the Java binary directory to the system PATH
$env:PATH += Join-Path ([Environment]::GetEnvironmentVariable("JAVA_HOME")) "bin;"

# Verify that the correct version of Java is being used
java -version
