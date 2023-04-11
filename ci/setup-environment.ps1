param(
    [Parameter(Mandatory=$true)]
    [string]$JavaSDKEnvVar
)
Write-Host "Setting up $JavaSDKEnvVar"
# Set the JAVA_HOME environment variable

[Environment]::SetEnvironmentVariable('JAVA_HOME', [Environment]::GetEnvironmentVariable($JavaSDKEnvVar), 'Machine')

Write-Host $env:JAVA_HOME

# Add the Java binary directory to the system PATH
$env:Path += ";" + "$env:JAVA_HOME\bin"

# Verify that the correct version of Java is being used
java -version