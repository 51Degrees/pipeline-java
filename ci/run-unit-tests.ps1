param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name,
    [hashtable]$Keys = @{}
)

# The tests that call the live cloud service read a resource key from the
# environment or from a system property. A GitHub runner has no such
# environment variable, so without this the key the workflow supplies
# never reaches Maven and every cloud test fails for want of a key.
#
# The key is passed under both the name the framework has always used and
# the name of the organisation wide key, so that either satisfies the
# lookup chain the tests use.
$extraArgs = @()
if ($Keys.TestResourceKey) {
    $extraArgs += "-DTestResourceKey=$($Keys.TestResourceKey)"
    $extraArgs += "-D_51DEGREES_RESOURCE_KEY_BESPOKE=$($Keys.TestResourceKey)"
}

./java/run-unit-tests.ps1 -RepoName $RepoName -Name $Name -ExtraArgs $extraArgs

exit $LASTEXITCODE
