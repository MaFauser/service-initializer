@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat -q printClasspath --no-configuration-cache 2>nul
endlocal
