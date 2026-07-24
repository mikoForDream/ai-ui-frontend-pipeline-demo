param(
	[string]$BaseUrl = 'http://127.0.0.1:9999/admin',
	[string]$Username = 'admin',
	[string]$Password = $env:PIG_ADMIN_PASSWORD
)

$ErrorActionPreference = 'Stop'

function Assert-True([bool]$Condition, [string]$Message) {
	if (-not $Condition) { throw "Assertion failed: $Message" }
}

Assert-True (-not [string]::IsNullOrWhiteSpace($Password)) `
	'provide -Password or set the PIG_ADMIN_PASSWORD environment variable'

function Invoke-WorkflowApi([string]$Method, [string]$Path, $Body = $null) {
	$params = @{
		Method = $Method
		Uri = "$BaseUrl$Path"
		Headers = @{ Authorization = "Bearer $script:AccessToken" }
		ContentType = 'application/json; charset=utf-8'
	}
	if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress) }
	$result = Invoke-RestMethod @params
	Assert-True ($result.code -eq 0) "API $Method $Path returned code $($result.code): $($result.msg)"
	return $result.data
}

$nodeExe = @(
	'C:\Program Files\nodejs\node.exe',
	"$env:APPDATA\nvm\v18.20.8\node.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
Assert-True ($null -ne $nodeExe) 'Node.js executable was not found'
$runDirectory = (Resolve-Path '.tools\run').Path
$encryptedPasswordFile = Join-Path $runDirectory 'project-smoke-encrypted-password.txt'
$materialFile = Join-Path $runDirectory 'project-smoke-material.md'
$encryptScript = (Resolve-Path 'scripts\encrypt-password.cjs').Path

try {
	$nodeProcess = Start-Process -FilePath $nodeExe -ArgumentList @($encryptScript, $Password) `
		-Wait -PassThru -NoNewWindow -RedirectStandardOutput $encryptedPasswordFile
	Assert-True ($nodeProcess.ExitCode -eq 0) 'password encryption process failed'
	$encryptedPassword = Get-Content $encryptedPasswordFile -Raw
	$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('test:test'))
	$tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" `
		-Headers @{ Authorization = "Basic $basic"; 'TENANT-ID' = '1' } `
		-ContentType 'application/x-www-form-urlencoded' `
		-Body @{ username = $Username; password = $encryptedPassword; grant_type = 'password'; scope = 'server' }
	$script:AccessToken = $tokenResponse.access_token
	Assert-True (-not [string]::IsNullOrWhiteSpace($script:AccessToken)) 'login did not return an access token'

	$currentUser = Invoke-WorkflowApi Get '/user/info'
	Assert-True ($currentUser.permissions -contains 'workflow_material_upload') 'login authority lacks material upload permission'
	Assert-True ($currentUser.permissions -contains 'workflow_feature_review') 'login authority lacks feature review permission'

	$stamp = Get-Date -Format 'yyyyMMddHHmmss'
	$project = Invoke-WorkflowApi Post '/workflow/projects' @{
		projectCode = "INTAKE_$stamp"
		name = "项目资料回归 $stamp"
		description = 'project intake smoke test'
		techStack = 'Vue 3 + Spring Boot + MySQL'
	}
	$projectId = [string]$project.id

	Set-Content -Path $materialFile -Encoding utf8 -Value @'
模块：用户管理
- 管理员创建用户
- 管理员停用用户
模块：订单管理
1. 用户提交订单
2. 用户查看订单状态
'@
	$upload = Invoke-RestMethod -Method Post -Uri "$BaseUrl/workflow/projects/$projectId/materials" `
		-Headers @{ Authorization = "Bearer $script:AccessToken" } -Form @{ file = Get-Item $materialFile }
	Assert-True ($upload.code -eq 0) "material upload failed: $($upload.msg)"
	Assert-True ($upload.data.parseStatus -eq 'PARSED') "material status is $($upload.data.parseStatus)"

	$analysis = Invoke-WorkflowApi Post "/workflow/projects/$projectId/analysis"
	Assert-True ($analysis.moduleCount -eq 2) "expected 2 modules, got $($analysis.moduleCount)"
	Assert-True ($analysis.featureCount -eq 4) "expected 4 features, got $($analysis.featureCount)"

	$workspace = Invoke-WorkflowApi Get "/workflow/projects/$projectId/workspace"
	Assert-True ($workspace.features.Count -eq 4) 'workspace feature count mismatch'
	foreach ($feature in $workspace.features) {
		[void](Invoke-WorkflowApi Post "/workflow/features/$($feature.id)/reviews" @{ action = 'APPROVE'; comment = 'smoke approved' })
	}
	$completed = Invoke-WorkflowApi Get "/workflow/projects/$projectId/workspace"
	Assert-True ($completed.project.currentStage -eq 'PROTOTYPE_READY') `
		"project stage is $($completed.project.currentStage), expected PROTOTYPE_READY"
	Assert-True (($completed.modules | Where-Object status -ne 'REQUIREMENT_APPROVED').Count -eq 0) `
		'not all modules reached REQUIREMENT_APPROVED'

	Write-Host "Project intake smoke regression passed for project $projectId."
}
finally {
	Remove-Item $encryptedPasswordFile, $materialFile -Force -ErrorAction SilentlyContinue
}
