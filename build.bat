@echo off
cd /d "%~dp0"
py -3 tools\build.py %*
if errorlevel 1 (
  echo Build failed. See the error above.
  pause
  exit /b 1
)
pause
