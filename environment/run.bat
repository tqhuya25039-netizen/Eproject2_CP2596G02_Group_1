@echo off
setlocal
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"

if /i "%~1"=="/setup" (
    powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-pharmacy.ps1" -Setup
) else (
    powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-pharmacy.ps1"
)

set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo The application could not be started. See the message above.
    pause
)
exit /b %EXIT_CODE%
