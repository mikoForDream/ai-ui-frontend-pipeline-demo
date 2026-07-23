$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Get-ProjectRoot {
	return (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}

function Resolve-ProjectJava {
	$root = Get-ProjectRoot
	$localJava = Get-ChildItem (Join-Path $root '.tools\jdk') -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
		Select-Object -First 1 -ExpandProperty FullName
	if ($localJava) { return $localJava }

	$command = Get-Command java.exe -ErrorAction SilentlyContinue
	if ($command) { return $command.Source }
	throw 'Java 17 was not found. Install it or place the portable JDK under .tools/jdk.'
}

function Resolve-ProjectMaven {
	$root = Get-ProjectRoot
	$localMaven = Get-ChildItem (Join-Path $root '.tools\maven') -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue |
		Select-Object -First 1 -ExpandProperty FullName
	if ($localMaven) { return $localMaven }

	$command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
	if ($command) { return $command.Source }
	throw 'Maven was not found. Install it or place the portable Maven distribution under .tools/maven.'
}

function Resolve-ProjectNode {
	$candidates = @(
		$(if ($env:NVM_SYMLINK) { Join-Path $env:NVM_SYMLINK 'node.exe' }),
		'C:\Program Files\nodejs\node.exe'
	) | Where-Object { $_ }

	foreach ($candidate in $candidates) {
		if (Test-Path $candidate) { return (Resolve-Path $candidate).Path }
	}

	$command = Get-Command node.exe -ErrorAction SilentlyContinue
	if ($command) { return $command.Source }
	throw 'Node.js was not found. Select Node 18.20.8 with nvm before starting the project.'
}

function Resolve-ProjectNpm {
	$node = Resolve-ProjectNode
	$npm = Join-Path (Split-Path $node) 'npm.cmd'
	if (Test-Path $npm) { return $npm }

	$command = Get-Command npm.cmd -ErrorAction SilentlyContinue
	if ($command) { return $command.Source }
	throw 'npm was not found next to the active Node.js installation.'
}

function Test-TcpPort {
	param([Parameter(Mandatory)][int]$Port)
	$client = [System.Net.Sockets.TcpClient]::new()
	try {
		$connection = $client.ConnectAsync('127.0.0.1', $Port)
		return $connection.Wait(800) -and $client.Connected
	}
	catch { return $false }
	finally { $client.Dispose() }
}

function Test-HttpEndpoint {
	param([Parameter(Mandatory)][string]$Uri)
	try {
		$response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 3
		return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
	}
	catch { return $false }
}

function Wait-Condition {
	param(
		[Parameter(Mandatory)][scriptblock]$Condition,
		[Parameter(Mandatory)][string]$Description,
		[int]$TimeoutSeconds = 120
	)
	$deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
	do {
		if (& $Condition) { return }
		Start-Sleep -Seconds 2
	} while ([DateTime]::UtcNow -lt $deadline)
	throw "Timed out waiting for $Description."
}

function Get-ManagedProcess {
	param([Parameter(Mandatory)][string]$PidFile)
	if (-not (Test-Path $PidFile)) { return $null }
	$managedPid = (Get-Content $PidFile -Raw).Trim()
	if (-not $managedPid) { return $null }
	return Get-Process -Id ([int]$managedPid) -ErrorAction SilentlyContinue
}
