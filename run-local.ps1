param([switch]$NoCompile,[switch]$WithoutOffice)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$configPath = Join-Path $PSScriptRoot '.env.local'
if (-not (Test-Path -LiteralPath $configPath)) {
    throw 'Create .env.local using .env.example and set your local MySQL connection before starting.'
}
# Parse values as data. Never execute a configuration file as PowerShell code.
$allowed = @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','JWT_SECRET','SPRING_MAIL_PASSWORD','SPRING_MAIL_USERNAME','SPRING_MAIL_HOST','SPRING_MAIL_PORT','SPRING_LIQUIBASE_ENABLED','APP_FILES_STORAGE_ROOT','APP_FILES_BACKUP_ROOT','APP_FRONTEND_BASE_URL','APP_FILES_COLLABORATION_KEY','APP_FILES_PASSWORD_KEY','APP_COMMUNICATION_KEY','ONLYOFFICE_ENABLED','ONLYOFFICE_BROWSER_URL','ONLYOFFICE_SERVER_URL','ONLYOFFICE_CONNECTOR_URL','ONLYOFFICE_JWT_SECRET','ONLYOFFICE_PROTECTED_RANGES_VERIFIED','ONLYOFFICE_TEMPLATE_FOLDER_ID')
foreach ($line in Get-Content -LiteralPath $configPath) {
    if ($line.Trim().StartsWith('#') -or [string]::IsNullOrWhiteSpace($line)) { continue }
    $pair = $line.Split('=',2)
    if ($pair.Length -eq 2 -and $allowed -contains $pair[0].Trim()) {
        [Environment]::SetEnvironmentVariable($pair[0].Trim(),$pair[1].Trim(),'Process')
    }
}
foreach ($key in @('SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','JWT_SECRET')) {
    if (-not [Environment]::GetEnvironmentVariable($key,'Process')) { throw "Missing $key in .env.local" }
}
Write-Host 'Starting EAC backend at http://localhost:8080. Keep this terminal open.'
if($WithoutOffice){$env:ONLYOFFICE_ENABLED='false';Write-Host 'Lightweight local mode: native Office server disabled; originals remain downloadable.'}
$installedMaven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenLauncher = if ($installedMaven) { $installedMaven.Source } else { Join-Path $PSScriptRoot 'mvnw.cmd' }
if ($NoCompile) { & $mavenLauncher '-Dmaven.main.skip=true' '-Dmaven.test.skip=true' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' spring-boot:run }
else { & $mavenLauncher '-Dmaven.test.skip=true' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' spring-boot:run }
exit $LASTEXITCODE
