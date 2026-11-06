@echo off
setlocal
cd /d "%~dp0"
title EAC local application and ONLYOFFICE
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-eac-office-local.ps1"
if errorlevel 1 (
  echo.
  echo Local startup did not finish. Read the error above.
)
pause
endlocal
