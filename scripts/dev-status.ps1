. (Join-Path $PSScriptRoot 'dev-common.ps1')

$checks = @(
	[pscustomobject]@{ Component = 'MySQL'; Address = '127.0.0.1:33306'; Ready = Test-TcpPort 33306 },
	[pscustomobject]@{ Component = 'Redis'; Address = '127.0.0.1:36379'; Ready = Test-TcpPort 36379 },
	[pscustomobject]@{ Component = 'Pig Boot'; Address = 'http://127.0.0.1:9999/admin'; Ready = Test-HttpEndpoint 'http://127.0.0.1:9999/admin/actuator/health' },
	[pscustomobject]@{ Component = 'Pig UI'; Address = 'http://127.0.0.1:8888'; Ready = Test-HttpEndpoint 'http://127.0.0.1:8888' }
)

$checks | Format-Table -AutoSize
if ($checks.Ready -contains $false) { exit 1 }
