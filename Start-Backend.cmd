@echo off
setlocal
cd /d "%~dp0"
title EAC Backend - Local
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-local.ps1"
if errorlevel 1 (
  echo.
  echo Backend did not start. Read the error above.
  pause
)
endlocal
