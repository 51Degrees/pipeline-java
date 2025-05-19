param(
    [Parameter(Mandatory)][string]$RepoName,
    [string]$Name
)

./java/run-performance-tests.ps1 @PSBoundParameters -TestName "ShareUsageOverheadTests"


$RepoPath = [IO.Path]::Combine($pwd, $RepoName)

Write-Output "Entering '$RepoPath'"
Push-Location $RepoPath

try{

    
    $PerfResultsFile = [IO.Path]::Combine($RepoPath, "test-results", "performance-summary", "fiftyone.pipeline.engines.fiftyone.performance.ShareUsageOverheadTests-output.txt")
    $outputFile = [IO.Path]::Combine($RepoPath, "test-results", "performance-summary","results_$Name.json")

    Get-Content $PerfResultsFile | ForEach-Object {
        if($_ -match "ShareUsageOverhead_SingleEvidence: ([\d.]+)ms per call") {

            $Overhead_SingleEvidence = [double]$Matches[1]
        }
        elseif($_ -match "ShareUsageOverhead_HundredEvidence: ([\d.]+)ms per call") {
            $Overhead_HundredEvidence = [double]$Matches[1]
        }
    }

    Write-Output "{
        'HigherIsBetter': {

        },
        'LowerIsBetter': {
            'ShareUsageOverhead_SingleEvidence_ms': $($Overhead_SingleEvidence),
            'ShareUsageOverhead_HundredEvidence_ms' : $($Overhead_HundredEvidence)
        }
    }" > $OutputFile
}
finally{
    Write-Output "Leaving '$RepoPath'"
    Pop-Location
}


exit $LASTEXITCODE
