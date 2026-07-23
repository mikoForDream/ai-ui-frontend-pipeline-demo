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
	if ($null -ne $Body) {
		$params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
	}
	try {
		$result = Invoke-RestMethod @params
	} catch {
		$detail = $_.ErrorDetails.Message
		throw "API $Method $Path failed: $detail"
	}
	Assert-True ($result.code -eq 0) "API $Method $Path returned code $($result.code): $($result.msg)"
	return $result.data
}

$nodeExe = @(
	'C:\Program Files\nodejs\node.exe',
	"$env:APPDATA\nvm\v18.20.8\node.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
Assert-True ($null -ne $nodeExe) 'Node.js 18.20.8 executable was not found'
$encryptedPasswordFile = Join-Path (Resolve-Path '.tools\run').Path 'smoke-encrypted-password.txt'
$encryptScript = (Resolve-Path 'scripts\encrypt-password.cjs').Path
$nodeProcess = Start-Process -FilePath $nodeExe -ArgumentList @($encryptScript, $Password) `
	-Wait -PassThru -NoNewWindow -RedirectStandardOutput $encryptedPasswordFile
Assert-True ($nodeProcess.ExitCode -eq 0) 'password encryption process failed'
$encryptedPassword = Get-Content $encryptedPasswordFile -Raw
Assert-True (-not [string]::IsNullOrWhiteSpace($encryptedPassword)) 'password encryption failed'

$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('test:test'))
$tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" `
	-Headers @{ Authorization = "Basic $basic"; 'TENANT-ID' = '1' } `
	-ContentType 'application/x-www-form-urlencoded' `
	-Body @{ username = $Username; password = $encryptedPassword; grant_type = 'password'; scope = 'server' }
$script:AccessToken = $tokenResponse.access_token
Assert-True (-not [string]::IsNullOrWhiteSpace($script:AccessToken)) 'login did not return an access token'
$currentUser = Invoke-WorkflowApi Get '/user/info'
Assert-True ($currentUser.permissions -contains 'workflow_approval_claim') "login authority snapshot does not contain workflow_approval_claim: $($currentUser.permissions -join ',')"

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$definition = Invoke-WorkflowApi Post '/workflow/definitions' @{
	code = "transition_smoke_$stamp"
	name = "动作流转回归测试 $stamp"
	description = 'REJECT -> revise -> review -> APPROVE -> deliver'
}
$definitionId = [string]$definition.id

$nodes = @(
	@{ nodeKey = 'generate'; nodeName = '生成'; nodeType = 'SERVICE'; sortOrder = 10; startNode = $true; endNode = $false; nextNodeKey = 'review' },
	@{ nodeKey = 'review'; nodeName = '人工审核'; nodeType = 'MANUAL_REVIEW'; sortOrder = 20; startNode = $false; endNode = $false; nextNodeKey = 'deliver'; configJson = '{"candidateRoleId":1}' },
	@{ nodeKey = 'revise'; nodeName = '返工修订'; nodeType = 'AI'; sortOrder = 30; startNode = $false; endNode = $false; nextNodeKey = 'review' },
	@{ nodeKey = 'deliver'; nodeName = '交付'; nodeType = 'DELIVERY'; sortOrder = 40; startNode = $false; endNode = $true }
)
foreach ($node in $nodes) {
	[void](Invoke-WorkflowApi Post "/workflow/definitions/$definitionId/nodes" $node)
}

$transitions = @(
	@{ sourceNodeKey = 'review'; targetNodeKey = 'deliver'; action = 'APPROVE'; priority = 100; defaultTransition = $false },
	@{ sourceNodeKey = 'review'; targetNodeKey = 'revise'; action = 'REJECT'; priority = 100; defaultTransition = $false }
)
foreach ($transition in $transitions) {
	[void](Invoke-WorkflowApi Post "/workflow/definitions/$definitionId/transitions" $transition)
}

[void](Invoke-WorkflowApi Post "/workflow/definitions/$definitionId/publish")
$instance = Invoke-WorkflowApi Post '/workflow/instances/start' @{
	definitionId = $definitionId
	businessKey = "transition-smoke-$stamp"
	title = "动作路由回归 $stamp"
	inputJson = '{"source":"smoke-test"}'
}
$instanceId = [string]$instance.id

function Get-Detail { Invoke-WorkflowApi Get "/workflow/instances/$instanceId" }
function Get-ActiveTask([string]$NodeKey) {
	$task = (Get-Detail).tasks | Where-Object { $_.nodeKey -eq $NodeKey -and $_.status -in @('READY', 'RUNNING') } | Select-Object -Last 1
	Assert-True ($null -ne $task) "active task for node '$NodeKey' was not found"
	return $task
}
function Get-PendingApproval([string]$TaskId) {
	$approval = (Get-Detail).approvals | Where-Object { [string]$_.taskId -eq $TaskId -and $_.status -eq 'PENDING' } | Select-Object -Last 1
	Assert-True ($null -ne $approval) "pending approval for task '$TaskId' was not found"
	return $approval
}

$generateTask = Get-ActiveTask 'generate'
[void](Invoke-WorkflowApi Post "/workflow/tasks/$($generateTask.id)/complete" @{ outputJson = '{"generated":true}' })

$firstReview = Get-ActiveTask 'review'
$firstApproval = Get-PendingApproval ([string]$firstReview.id)
[void](Invoke-WorkflowApi Post "/workflow/approvals/$($firstApproval.id)/claim")
[void](Invoke-WorkflowApi Post "/workflow/approvals/$($firstApproval.id)/decisions" @{
	decision = 'REJECT'; operationKey = "reject-$stamp"; comment = 'needs revision'; outputJson = '{"reason":"needs-revision"}'
})

$reviseTask = Get-ActiveTask 'revise'
[void](Invoke-WorkflowApi Post "/workflow/tasks/$($reviseTask.id)/complete" @{ outputJson = '{"revised":true}' })

$secondReview = Get-ActiveTask 'review'
$secondApproval = Get-PendingApproval ([string]$secondReview.id)
[void](Invoke-WorkflowApi Post "/workflow/approvals/$($secondApproval.id)/claim")
$approveRequest = @{ decision = 'APPROVE'; operationKey = "approve-$stamp"; comment = 'accepted'; outputJson = '{"approved":true}' }
[void](Invoke-WorkflowApi Post "/workflow/approvals/$($secondApproval.id)/decisions" $approveRequest)
[void](Invoke-WorkflowApi Post "/workflow/approvals/$($secondApproval.id)/decisions" $approveRequest)

$deliverTask = Get-ActiveTask 'deliver'
[void](Invoke-WorkflowApi Post "/workflow/tasks/$($deliverTask.id)/complete" @{ outputJson = '{"delivered":true}' })

$beforeDuplicate = Get-Detail
$duplicateBlocked = $false
try {
	[void](Invoke-WorkflowApi Post "/workflow/tasks/$($secondReview.id)/actions" @{ action = 'APPROVE' })
} catch {
	$duplicateBlocked = $true
}
$afterDuplicate = Get-Detail

Assert-True ($afterDuplicate.instance.status -eq 'COMPLETED') 'instance did not finish as COMPLETED'
Assert-True ($afterDuplicate.tasks.Count -eq 5) "expected 5 tasks, got $($afterDuplicate.tasks.Count)"
Assert-True ($afterDuplicate.approvals.Count -eq 2) "expected 2 approvals, got $($afterDuplicate.approvals.Count)"
Assert-True (($afterDuplicate.approvals | Where-Object { $_.status -eq 'DECIDED' }).Count -eq 2) 'approvals were not completed'
$reviewerApprovals = Invoke-WorkflowApi Get '/workflow/approvals/page?current=1&size=20&reviewerId=1'
$roleApprovals = Invoke-WorkflowApi Get '/workflow/approvals/page?current=1&size=20&candidateRoleId=1'
Assert-True ($reviewerApprovals.records.Count -ge 2) 'reviewer filter did not return the completed approvals'
Assert-True ($roleApprovals.records.Count -ge 2) 'candidate role filter did not return the generated approvals'
Assert-True ($beforeDuplicate.tasks.Count -eq $afterDuplicate.tasks.Count) 'duplicate action created an extra task'
Assert-True $duplicateBlocked 'duplicate action was not rejected'

$sequence = ($afterDuplicate.tasks | ForEach-Object { "$($_.nodeKey):$($_.status)" }) -join ' -> '
[pscustomobject]@{
	definitionId = $definitionId
	instanceId = $instanceId
	instanceStatus = $afterDuplicate.instance.status
	taskCount = $afterDuplicate.tasks.Count
	logCount = $afterDuplicate.logs.Count
	approvalCount = $afterDuplicate.approvals.Count
	approvalDecisions = (($afterDuplicate.approvals | ForEach-Object { "$($_.decision):$($_.reviewerName)" }) -join ' -> ')
	duplicateActionBlocked = $duplicateBlocked
	taskSequence = $sequence
} | ConvertTo-Json -Compress
