@echo off
setlocal
cd /d "%~dp0"
if not exist "runtime\bin\java.exe" (
  echo Portable runtime missing. Extract the complete ZIP first.
  pause
  exit /b 1
)
echo Starting the RunePath Sync development client...
echo This is not a reviewed Plugin Hub release.
"runtime\bin\java.exe" -ea -cp "classes;lib\*" com.runepath.sync.RunePathSyncPluginTest --developer-mode --profile RunePath-Test
set "RC=%ERRORLEVEL%"
if not "%RC%"=="0" (
  echo RuneLite exited with code %RC%.
  pause
)
exit /b %RC%
