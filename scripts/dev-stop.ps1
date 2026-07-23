[CmdletBinding()]
param([switch]$Infrastructure)

. (Join-Path $PSScriptRoot 'dev-common.ps1')

$root = Get-ProjectRoot
$runDirectory = Join-Path $root '.tools\run'

foreach ($name in @('pig-ui', 'pig-boot')) {
	$pidFile = Join-Path $runDirectory "$name.pid"
	$process = Get-ManagedProcess $pidFile
	if ($process) {
		Stop-Process -Id $process.Id -Force
		$process.WaitForExit(10000) | Out-Null
		Write-Host "Stopped $name (PID $($process.Id))."
	}
	if (Test-Path $pidFile) { Remove-Item -LiteralPath $pidFile -Force }
}

if ($Infrastructure) {
	docker compose -f (Join-Path $root 'backend\docker-compose.yml') stop pig-mysql pig-redis
	if ($LASTEXITCODE -ne 0) { throw 'Failed to stop MySQL or Redis.' }
}
