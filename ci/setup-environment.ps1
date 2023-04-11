
param(
    [Parameter(Mandatory=$true)]
    [string]$JavaSDKEnvVar
)

Write-Host "Setting up $JavaSDKEnvVar"

# Set the JAVA_HOME environment variable
[Environment]::SetEnvironmentVariable('JAVA_HOME', [Environment]::GetEnvironmentVariable($JavaSDKEnvVar))

# Add the Java binary directory to the system PATH
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
Write-Host $RUNNER_OS
if($RUNNER_OS -eq Linux){

    sudo ln -sf $env:JAVA_HOME/bin/java /usr/bin/java
    }

# Verify that the correct version of Java is being used
java -version
