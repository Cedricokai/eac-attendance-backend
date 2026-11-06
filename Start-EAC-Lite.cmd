@echo off
setlocal
cd /d "%~dp0"
title EAC lightweight local mode
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-eac-office-local.ps1" -WithoutOffice
if errorlevel 1 echo Local startup failed. Review the message above.
pause
endlocal
