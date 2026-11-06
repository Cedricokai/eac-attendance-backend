param([switch]$PrepareOnly)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$docker=Get-Command docker.exe -ErrorAction SilentlyContinue
if(-not $docker){$candidate='C:\Program Files\Docker\Docker\resources\bin\docker.exe';if(Test-Path -LiteralPath $candidate){$docker=Get-Item -LiteralPath $candidate}else{throw 'Install Docker Desktop with the WSL 2 backend, complete Windows administrator approval, and restart Windows if requested.'}}
$dockerPath=if($docker.Source){$docker.Source}else{$docker.FullName}
$envPath=Join-Path $PSScriptRoot '.env.local'
if(-not(Test-Path -LiteralPath $envPath)){throw 'Create the existing backend .env.local first.'}
function Set-OfficeValue([string]$name,[string]$value){
 $text=[IO.File]::ReadAllText($envPath)
 $pattern='(?m)^'+[regex]::Escape($name)+'=.*$'
 if([regex]::IsMatch($text,$pattern)){$text=[regex]::Replace($text,$pattern,[System.Text.RegularExpressions.MatchEvaluator]{param($m)$name+'='+$value})}else{$text=$text.TrimEnd()+[Environment]::NewLine+$name+'='+$value+[Environment]::NewLine}
 [IO.File]::WriteAllText($envPath,$text,[Text.UTF8Encoding]::new($false))
}
$existing=@{}
foreach($line in Get-Content -LiteralPath $envPath){$pair=$line.Split('=',2);if($pair.Length -eq 2 -and $pair[0].StartsWith('ONLYOFFICE_')){$existing[$pair[0]]=$pair[1]}}
if(-not $existing.ONLYOFFICE_JWT_SECRET){$random=New-Object byte[] 48;[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($random);Set-OfficeValue 'ONLYOFFICE_JWT_SECRET' ([Convert]::ToBase64String($random))}
Set-OfficeValue 'ONLYOFFICE_BROWSER_URL' 'http://localhost:8082'
Set-OfficeValue 'ONLYOFFICE_SERVER_URL' 'http://localhost:8082'
Set-OfficeValue 'ONLYOFFICE_CONNECTOR_URL' 'http://host.docker.internal:8080'
Set-OfficeValue 'ONLYOFFICE_ENABLED' 'false'
if(-not $existing.ContainsKey('ONLYOFFICE_PROTECTED_RANGES_VERIFIED')){Set-OfficeValue 'ONLYOFFICE_PROTECTED_RANGES_VERIFIED' 'false'}
if($PrepareOnly){Write-Output 'Private local ONLYOFFICE configuration prepared. Editing remains disabled until the server passes its health check.';exit 0}
& $dockerPath info --format '{{.ServerVersion}}'
if($LASTEXITCODE -ne 0){
 $desktop='C:\Program Files\Docker\Docker\Docker Desktop.exe'
 if(Test-Path -LiteralPath $desktop){Start-Process -FilePath $desktop -WindowStyle Hidden}
 Write-Output 'Waiting for Docker Desktop. Complete its first-run screen if one appears.'
 $engineReady=$false
 for($engineAttempt=0;$engineAttempt -lt 12;$engineAttempt++){Start-Sleep -Seconds 10;& $dockerPath info --format '{{.ServerVersion}}';if($LASTEXITCODE -eq 0){$engineReady=$true;break}}
 if(-not $engineReady){throw 'Docker engine cannot start. Complete the required Windows restart and Docker first-run setup, then run this script again.'}
}
$drive=Get-PSDrive -Name (Split-Path -Qualifier $PSScriptRoot).TrimEnd(':')
$composePath=Join-Path $PSScriptRoot 'deploy/onlyoffice/compose.local.yml'
$imageLine=Get-Content -LiteralPath $composePath | Where-Object {$_ -match '^\s+image:\s+'} | Select-Object -First 1
if(-not $imageLine){throw 'The local Document Server image is missing from compose configuration.'}
$imageName=($imageLine -replace '^\s+image:\s+','').Trim()
& $dockerPath image inspect $imageName --format '{{.Id}}' *> $null
$imageInstalled=$LASTEXITCODE -eq 0
if(-not $imageInstalled -and $drive.Free -lt 6GB){throw 'At least 6 GB of free space is required before pulling the local document server image.'}
# Resolve the exact host-gateway address. Do not allow all private or metadata addresses.
$resolved=@(& $dockerPath run --rm --add-host host.docker.internal:host-gateway busybox:1.37.0 sh -c 'grep -w host.docker.internal /etc/hosts')
if($LASTEXITCODE -ne 0){throw 'Could not resolve the backend host from Docker.'}
$ip=$null
foreach($entry in $resolved){
 $candidate=($entry.Trim() -split '\s+')[0]
 $parsed=$null
 if([Net.IPAddress]::TryParse($candidate,[ref]$parsed) -and $parsed.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork){$ip=$candidate;break}
}
if(-not $ip){throw 'Docker returned no valid IPv4 backend gateway address.'}
Set-OfficeValue 'ONLYOFFICE_BACKEND_IP' $ip
& $dockerPath compose --env-file $envPath -f deploy/onlyoffice/compose.local.yml up -d
if($LASTEXITCODE -ne 0){throw 'Document Server did not start; editing remains disabled and existing originals are preserved.'}
Write-Output 'Document Server container started. Waiting up to five minutes for its health check.'
$ready=$false
for($attempt=0;$attempt -lt 30;$attempt++){
 try{$health=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8082/healthcheck' -TimeoutSec 5;if($health.StatusCode -eq 200 -and $health.Content.Trim() -eq 'true'){$ready=$true;break}}catch{}
 Start-Sleep -Seconds 10
}
if(-not $ready){throw 'Document Server is not healthy yet. Review docker compose logs. Backend editing remains disabled.'}
$containerId=& $dockerPath compose --env-file $envPath -f deploy/onlyoffice/compose.local.yml ps -q onlyoffice
if(-not $containerId){throw 'Cannot locate the Document Server container for the sheet-access extension.'}
$pluginSource=Join-Path $PSScriptRoot 'deploy/onlyoffice/sheet-access'
& $dockerPath exec $containerId.Trim() mkdir -p '/var/www/onlyoffice/documentserver/sdkjs-plugins/{B785C60D-E5F1-48D1-A624-2BF8EAC50001}'
if($LASTEXITCODE -ne 0){throw 'Could not prepare the sheet-access extension folder.'}
& $dockerPath cp ($pluginSource+'/.') ($containerId.Trim()+':/var/www/onlyoffice/documentserver/sdkjs-plugins/{B785C60D-E5F1-48D1-A624-2BF8EAC50001}')
if($LASTEXITCODE -ne 0){throw 'Could not install the local sheet-access extension.'}
& $dockerPath exec $containerId.Trim() sh -c 'for asset in "$1"/*.js "$1"/*.json "$1"/*.html "$1"/*.css "$1"/*.svg; do [ ! -f "$asset" ] || gzip -kf "$asset" || exit 1; done' eac-plugin-gzip '/var/www/onlyoffice/documentserver/sdkjs-plugins/{B785C60D-E5F1-48D1-A624-2BF8EAC50001}'
if($LASTEXITCODE -ne 0){throw 'Could not refresh compressed Office plugin assets.'}
Set-OfficeValue 'ONLYOFFICE_ENABLED' 'true'
Write-Output 'ONLYOFFICE is healthy at http://localhost:8082. Restart the EAC backend to activate it, then open a DOCX or XLSX in Files. Protected-range verification is still pending two-user testing.'
