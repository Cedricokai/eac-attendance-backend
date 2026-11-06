@echo off
setlocal
cd /d "%~dp0"
title EAC - ONLYOFFICE local setup
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-onlyoffice.ps1"
if errorlevel 1 (
  echo.
  echo Office setup could not finish. Read the message above.
  pause
  exit /b 1
)
echo.
echo Office is healthy. Start or restart the EAC backend using Start-Backend.cmd.
pause
endlocal
