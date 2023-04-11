
param(
    [Parameter(Mandatory=$true)]
    [string]$JavaSDKEnvVar
)

Write-Host "Setting up $JavaSDKEnvVar"

# Get the current PATH and remove the old JAVA_HOME/bin directory
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
$oldJavaBinDir = Join-Path ([Environment]::GetEnvironmentVariable("JAVA_HOME")) "bin"
$currentPath = $currentPath -replace "$oldJavaBinDir;", ""

Write-Host $currentPath

# Set the JAVA_HOME environment variable
[Environment]::SetEnvironmentVariable('JAVA_HOME', [Environment]::GetEnvironmentVariable($JavaSDKEnvVar))

# Add the Java binary directory to the system PATH
$newJavaBinDir = Join-Path ([Environment]::GetEnvironmentVariable("JAVA_HOME")) "bin"
$currentPath = $newJavaBinDir + ";" + $currentPath
[Environment]::SetEnvironmentVariable('PATH', $currentPath, "Machine")

Write-Host $currentPath

# Verify that the correct version of Java is being used
java -version
