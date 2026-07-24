param(
	[string]$BaseUrl = 'http://127.0.0.1:9999/admin',
	[string]$Username = 'admin',
	[string]$Password = $env:PIG_ADMIN_PASSWORD,
	[string]$EncryptedPassword
)

$ErrorActionPreference = 'Stop'

function Assert-True([bool]$Condition, [string]$Message) {
	if (-not $Condition) { throw "Assertion failed: $Message" }
}

Assert-True (-not [string]::IsNullOrWhiteSpace($Password) -or -not [string]::IsNullOrWhiteSpace($EncryptedPassword)) `
	'provide -Password, -EncryptedPassword, or set the PIG_ADMIN_PASSWORD environment variable'

function Invoke-WorkflowApi([string]$Method, [string]$Path, $Body = $null) {
	$params = @{
		Method = $Method
		Uri = "$BaseUrl$Path"
		Headers = @{ Authorization = "Bearer $script:AccessToken" }
		ContentType = 'application/json; charset=utf-8'
	}
	if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress) }
	try { $result = Invoke-RestMethod @params }
	catch { throw "API $Method $Path failed: $($_.Exception.Message)" }
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
$uiImageFile = Join-Path $runDirectory 'project-smoke-ui.png'
$frontendZipFile = Join-Path $runDirectory 'project-smoke-frontend.zip'
$encryptScript = (Resolve-Path 'scripts\encrypt-password.cjs').Path

try {
	if ([string]::IsNullOrWhiteSpace($EncryptedPassword)) {
		$nodeProcess = Start-Process -FilePath $nodeExe -ArgumentList @($encryptScript, $Password) `
			-Wait -PassThru -NoNewWindow -RedirectStandardOutput $encryptedPasswordFile
		Assert-True ($nodeProcess.ExitCode -eq 0) 'password encryption process failed'
		$EncryptedPassword = Get-Content $encryptedPasswordFile -Raw
	}
	$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('test:test'))
	$tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" `
		-Headers @{ Authorization = "Basic $basic"; 'TENANT-ID' = '1' } `
		-ContentType 'application/x-www-form-urlencoded' `
		-Body @{ username = $Username; password = $EncryptedPassword; grant_type = 'password'; scope = 'server' }
	$script:AccessToken = $tokenResponse.access_token
	Assert-True (-not [string]::IsNullOrWhiteSpace($script:AccessToken)) 'login did not return an access token'

	$currentUser = Invoke-WorkflowApi Get '/user/info'
	Assert-True ($currentUser.permissions -contains 'workflow_material_upload') 'login authority lacks material upload permission'
	Assert-True ($currentUser.permissions -contains 'workflow_feature_review') 'login authority lacks feature review permission'
	Assert-True ($currentUser.permissions -contains 'workflow_prototype_generate') 'login authority lacks prototype generation permission'
	Assert-True ($currentUser.permissions -contains 'workflow_prototype_review') 'login authority lacks prototype review permission'
	Assert-True ($currentUser.permissions -contains 'workflow_ui_generate') 'login authority lacks UI generation permission'
	Assert-True ($currentUser.permissions -contains 'workflow_ui_upload') 'login authority lacks UI upload permission'
	Assert-True ($currentUser.permissions -contains 'workflow_ui_review') 'login authority lacks UI review permission'
	Assert-True ($currentUser.permissions -contains 'workflow_frontend_edit') 'login authority lacks frontend logic edit permission'
	Assert-True ($currentUser.permissions -contains 'workflow_frontend_generate') 'login authority lacks frontend generation permission'
	Assert-True ($currentUser.permissions -contains 'workflow_frontend_review') 'login authority lacks frontend review permission'
	$menuJson = Invoke-WorkflowApi Get '/menu' | ConvertTo-Json -Depth 20 -Compress
	Assert-True ($menuJson -like '*"name":"工作流管理"*') 'workflow menu name is missing or has an invalid charset'
	Assert-True ($menuJson -like '*"name":"研发项目"*') 'project menu name is missing or has an invalid charset'

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

	$modules = @($completed.modules)
	$firstPrototype = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/prototypes"
	Assert-True ($firstPrototype.versionNo -eq 'V1') "first prototype version is $($firstPrototype.versionNo)"
	$prototypeDetail = Invoke-WorkflowApi Get "/workflow/prototypes/$($firstPrototype.versionId)"
	Assert-True ($prototypeDetail.html -like '*<!doctype html>*') 'prototype detail does not contain HTML'
	[void](Invoke-WorkflowApi Post "/workflow/prototypes/$($firstPrototype.versionId)/reviews" @{
		action = 'REJECT'
		comment = 'smoke requires a second version'
	})
	$secondPrototype = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/prototypes"
	Assert-True ($secondPrototype.versionNo -eq 'V2') "regenerated prototype version is $($secondPrototype.versionNo)"
	[void](Invoke-WorkflowApi Post "/workflow/prototypes/$($secondPrototype.versionId)/reviews" @{
		action = 'APPROVE'
		comment = 'smoke approved V2'
	})
	for ($index = 1; $index -lt $modules.Count; $index++) {
		$prototype = Invoke-WorkflowApi Post "/workflow/modules/$($modules[$index].id)/prototypes"
		[void](Invoke-WorkflowApi Post "/workflow/prototypes/$($prototype.versionId)/reviews" @{
			action = 'APPROVE'
			comment = 'smoke approved'
		})
	}
	$prototypeCompleted = Invoke-WorkflowApi Get "/workflow/projects/$projectId/workspace"
	Assert-True ($prototypeCompleted.project.currentStage -eq 'UI_READY') `
		"project stage is $($prototypeCompleted.project.currentStage), expected UI_READY"
	Assert-True ($prototypeCompleted.frozenSpecVersion -eq 'REQ-V1') 'frozen requirement spec is missing'
	Assert-True ($prototypeCompleted.prototypes.Count -eq $modules.Count) 'module prototype count mismatch'
	Assert-True (($prototypeCompleted.prototypes | Where-Object status -ne 'APPROVED').Count -eq 0) `
		'not all module prototypes reached APPROVED'

	$firstUi = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/ui-designs/generate"
	Assert-True ($firstUi.versionNo -eq 'V1') "first UI design version is $($firstUi.versionNo)"
	$uiDetail = Invoke-WorkflowApi Get "/workflow/ui-designs/$($firstUi.versionId)"
	Assert-True ($uiDetail.contentKind -eq 'HTML' -and $uiDetail.html -like '*<!doctype html>*') 'UI design detail does not contain HTML'
	[void](Invoke-WorkflowApi Post "/workflow/ui-designs/$($firstUi.versionId)/reviews" @{
		action = 'REJECT'
		comment = 'smoke requires a revised UI design'
	})
	$secondUi = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/ui-designs/generate"
	Assert-True ($secondUi.versionNo -eq 'V2') "regenerated UI design version is $($secondUi.versionNo)"
	[void](Invoke-WorkflowApi Post "/workflow/ui-designs/$($secondUi.versionId)/reviews" @{ action = 'APPROVE'; comment = 'smoke approved UI V2' })

	# 1x1 transparent PNG, sufficient to exercise the authenticated object-storage path.
	[IO.File]::WriteAllBytes($uiImageFile, [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='))
	$uploadUi = Invoke-RestMethod -Method Post -Uri "$BaseUrl/workflow/modules/$($modules[1].id)/ui-designs" `
		-Headers @{ Authorization = "Bearer $script:AccessToken" } -Form @{ file = Get-Item $uiImageFile }
	Assert-True ($uploadUi.code -eq 0 -and $uploadUi.data.contentKind -eq 'IMAGE') "UI design upload failed: $($uploadUi.msg)"
	[void](Invoke-WorkflowApi Post "/workflow/ui-designs/$($uploadUi.data.versionId)/reviews" @{ action = 'APPROVE'; comment = 'smoke approved uploaded image' })
	$uiCompleted = Invoke-WorkflowApi Get "/workflow/projects/$projectId/workspace"
	Assert-True ($uiCompleted.project.currentStage -eq 'FRONTEND_READY') `
		"project stage is $($uiCompleted.project.currentStage), expected FRONTEND_READY"
	Assert-True ($uiCompleted.uiDesigns.Count -eq $modules.Count) 'module UI design count mismatch'
	Assert-True (($uiCompleted.uiDesigns | Where-Object status -ne 'APPROVED').Count -eq 0) 'not all module UI designs reached APPROVED'

	[void](Invoke-WorkflowApi Put "/workflow/modules/$($modules[0].id)/frontend-spec" @{
		logic = '加载列表后支持筛选；保存失败时保留表单数据并展示错误信息。'
	})
	$firstFrontend = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/frontend-codes/generate"
	Assert-True ($firstFrontend.versionNo -eq 'V1') "first frontend code version is $($firstFrontend.versionNo)"
	Assert-True ($firstFrontend.generator -eq 'RULE_BASED_VUE3_V1') "unexpected frontend generator $($firstFrontend.generator)"
	Assert-True ($firstFrontend.fileCount -eq 4) "frontend V1 file count is $($firstFrontend.fileCount)"
	[void](Invoke-WorkflowApi Post "/workflow/frontend-codes/$($firstFrontend.versionId)/reviews" @{
		action = 'REJECT'
		comment = 'smoke requires revised interaction feedback'
	})
	[void](Invoke-WorkflowApi Put "/workflow/modules/$($modules[0].id)/frontend-spec" @{
		logic = '加载列表后支持筛选；保存成功或失败都要给出明确反馈，失败时保留表单数据。'
	})
	$secondFrontend = Invoke-WorkflowApi Post "/workflow/modules/$($modules[0].id)/frontend-codes/generate"
	Assert-True ($secondFrontend.versionNo -eq 'V2') "regenerated frontend code version is $($secondFrontend.versionNo)"
	$frontendDetail = Invoke-WorkflowApi Get "/workflow/frontend-codes/$($secondFrontend.versionId)"
	Assert-True ($frontendDetail.generator -eq 'RULE_BASED_VUE3_V1') 'frontend detail generator mismatch'
	Assert-True ($frontendDetail.files.Count -eq 4) 'frontend detail file count mismatch'
	Assert-True ($frontendDetail.previewHtml -like '*<!doctype html>*') 'frontend detail does not contain preview HTML'
	Assert-True (($frontendDetail.files.path | Where-Object { $_ -like 'src/views/workflow/*/index.vue' }).Count -eq 1) `
		'frontend package does not contain a Vue page'
	Invoke-WebRequest -Method Get -Uri "$BaseUrl/workflow/frontend-codes/$($secondFrontend.versionId)/download" `
		-Headers @{ Authorization = "Bearer $script:AccessToken" } -OutFile $frontendZipFile
	Assert-True ((Get-Item $frontendZipFile).Length -gt 20) 'frontend ZIP download is empty'
	[void](Invoke-WorkflowApi Post "/workflow/frontend-codes/$($secondFrontend.versionId)/reviews" @{
		action = 'APPROVE'
		comment = 'smoke approved frontend V2'
	})
	for ($index = 1; $index -lt $modules.Count; $index++) {
		[void](Invoke-WorkflowApi Put "/workflow/modules/$($modules[$index].id)/frontend-spec" @{
			logic = '按照已审核 UI 与功能点验收标准实现页面交互。'
		})
		$frontend = Invoke-WorkflowApi Post "/workflow/modules/$($modules[$index].id)/frontend-codes/generate"
		[void](Invoke-WorkflowApi Post "/workflow/frontend-codes/$($frontend.versionId)/reviews" @{
			action = 'APPROVE'
			comment = 'smoke approved frontend code'
		})
	}
	$frontendCompleted = Invoke-WorkflowApi Get "/workflow/projects/$projectId/workspace"
	Assert-True ($frontendCompleted.project.currentStage -eq 'BACKEND_READY') `
		"project stage is $($frontendCompleted.project.currentStage), expected BACKEND_READY"
	Assert-True ($frontendCompleted.frontendCodes.Count -eq $modules.Count) 'module frontend code count mismatch'
	Assert-True (($frontendCompleted.frontendCodes | Where-Object status -ne 'APPROVED').Count -eq 0) `
		'not all module frontend code versions reached APPROVED'

	Write-Host "Project intake through frontend code review regression passed for project $projectId."
}
finally {
	Remove-Item $encryptedPasswordFile, $materialFile, $uiImageFile, $frontendZipFile -Force -ErrorAction SilentlyContinue
}
