[CmdletBinding()]
param(
	[switch]$Rebuild,
	[switch]$SkipInfrastructure,
	[int]$TimeoutSeconds = 120
)

. (Join-Path $PSScriptRoot 'dev-common.ps1')

$root = Get-ProjectRoot
$backend = Join-Path $root 'backend'
$frontend = Join-Path $root 'frontend'
$runDirectory = Join-Path $root '.tools\run'
$backendJar = Join-Path $backend 'pig-boot\target\pig-boot.jar'
$backendPid = Join-Path $runDirectory 'pig-boot.pid'
$frontendPid = Join-Path $runDirectory 'pig-ui.pid'
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

if (-not $SkipInfrastructure) {
	docker info --format '{{.ServerVersion}}' | Out-Null
	if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop is not running.' }
	docker compose -f (Join-Path $backend 'docker-compose.yml') up -d pig-mysql pig-redis
	if ($LASTEXITCODE -ne 0) { throw 'MySQL or Redis failed to start.' }
}

Wait-Condition { Test-TcpPort 33306 } 'MySQL on port 33306' $TimeoutSeconds
Wait-Condition { Test-TcpPort 36379 } 'Redis on port 36379' $TimeoutSeconds
Wait-Condition {
	docker exec -e MYSQL_PWD=root pig-mysql mysqladmin ping -h 127.0.0.1 -uroot --silent 2>$null | Out-Null
	return $LASTEXITCODE -eq 0
} 'MySQL accepting connections' $TimeoutSeconds

if ($Rebuild -or -not (Test-Path $backendJar)) {
	$maven = Resolve-ProjectMaven
	Push-Location $backend
	try {
		& $maven -Pboot -DskipTests package
		if ($LASTEXITCODE -ne 0) { throw 'Backend build failed.' }
	}
	finally { Pop-Location }
}

if (-not (Test-HttpEndpoint 'http://127.0.0.1:9999/admin/actuator/health')) {
	$existingBackend = Get-ManagedProcess $backendPid
	if ($existingBackend) { throw "Managed backend process $($existingBackend.Id) is running but not healthy. Run dev-stop.ps1 first." }
	$java = Resolve-ProjectJava
	$process = Start-Process -FilePath $java -ArgumentList @('-jar', $backendJar, '--spring.profiles.active=dev') `
		-WorkingDirectory $backend -RedirectStandardOutput (Join-Path $runDirectory 'pig-boot.out.log') `
		-RedirectStandardError (Join-Path $runDirectory 'pig-boot.err.log') -WindowStyle Hidden -PassThru
	Set-Content -Path $backendPid -Value $process.Id
}

$viteEntry = Join-Path $frontend 'node_modules\vite\bin\vite.js'
if (-not (Test-Path $viteEntry)) {
	$npm = Resolve-ProjectNpm
	Push-Location $frontend
	try {
		& $npm ci
		if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
	}
	finally { Pop-Location }
}

if (-not (Test-HttpEndpoint 'http://127.0.0.1:8888')) {
	$existingFrontend = Get-ManagedProcess $frontendPid
	if ($existingFrontend) { throw "Managed frontend process $($existingFrontend.Id) is running but not healthy. Run dev-stop.ps1 first." }
	$node = Resolve-ProjectNode
	$process = Start-Process -FilePath $node -ArgumentList @($viteEntry, '--force') -WorkingDirectory $frontend `
		-RedirectStandardOutput (Join-Path $runDirectory 'pig-ui.out.log') `
		-RedirectStandardError (Join-Path $runDirectory 'pig-ui.err.log') -WindowStyle Hidden -PassThru
	Set-Content -Path $frontendPid -Value $process.Id
}

Wait-Condition {
	$backendProcess = Get-ManagedProcess $backendPid
	if (-not $backendProcess) { throw "Pig Boot exited before becoming healthy. Check .tools/run/pig-boot.out.log." }
	return Test-HttpEndpoint 'http://127.0.0.1:9999/admin/actuator/health'
} 'Pig Boot health endpoint' $TimeoutSeconds
Wait-Condition {
	$frontendProcess = Get-ManagedProcess $frontendPid
	if (-not $frontendProcess) { throw "Pig UI exited before becoming healthy. Check .tools/run/pig-ui.err.log." }
	return Test-HttpEndpoint 'http://127.0.0.1:8888'
} 'Pig UI' $TimeoutSeconds

Write-Host 'Pig development environment is ready.'
Write-Host 'Backend: http://127.0.0.1:9999/admin'
Write-Host 'Frontend: http://127.0.0.1:8888'
