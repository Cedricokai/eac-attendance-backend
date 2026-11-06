param([string]$FrontendPath=(Join-Path (Split-Path $PSScriptRoot -Parent) 'Eac_Attendance_Frontend\eac-attendance'),[switch]$WithoutOffice)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$FrontendPath=[IO.Path]::GetFullPath($FrontendPath)
if(-not(Test-Path -LiteralPath (Join-Path $FrontendPath 'run-local.ps1'))){throw 'The frontend launcher was not found. Pass -FrontendPath with your actual frontend checkout.'}
if(!$WithoutOffice){
 & (Join-Path $PSScriptRoot 'start-onlyoffice.ps1')
 if($LASTEXITCODE -ne 0){throw 'ONLYOFFICE startup failed.'}
}else{Write-Output 'Lightweight local mode: Docker is not started. Native Office editing can be connected to a NAS document server later.'}
$logRoot=Join-Path $PSScriptRoot '.codex-tmp\office-run'
[IO.Directory]::CreateDirectory($logRoot)|Out-Null
$shell=(Get-Command powershell.exe -ErrorAction Stop).Source
function Start-LocalServer([string]$Name,[int]$Port,[string]$Folder){
 if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue){Write-Output "$Name already has a listener on port $Port; no existing process was stopped.";return}
 $launcher=Join-Path $Folder 'run-local.ps1'
 $stamp=Get-Date -Format 'yyyyMMdd-HHmmss'
 $arguments=@('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$launcher+'"'))
 if($WithoutOffice -and $Name -eq 'backend'){$arguments+='-WithoutOffice'}
 $process=Start-Process -FilePath $shell -ArgumentList $arguments -WorkingDirectory $Folder -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot "$Name-$stamp.log") -RedirectStandardError (Join-Path $logRoot "$Name-$stamp-errors.log") -PassThru
 Write-Output "$Name starting on port $Port. Launcher process: $($process.Id). Logs: $logRoot"
}
Start-LocalServer 'backend' 8080 $PSScriptRoot
Start-LocalServer 'frontend' 5173 $FrontendPath
Write-Output 'Open http://localhost:5173/#/files after the backend finishes starting. If an older backend was already running with Office disabled, restart it using Start-Backend.cmd.'
