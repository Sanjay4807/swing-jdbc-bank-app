@echo off
setlocal
cd /d "%~dp0"

echo =========================================================
echo    APEX TRUST BANK - JAVA SWING AND JDBC MINI PROJECT
echo =========================================================

set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-26.0.2\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

echo Starting application using: %JAVA_CMD%
"%JAVA_CMD%" -Xmx128m -cp "bin;lib\h2.jar;lib\ojdbc11.jar" Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%. Running auto-rebuild...
    call scripts\build.bat
    echo Retrying application start...
    "%JAVA_CMD%" -Xmx128m -cp "bin;lib\h2.jar;lib\ojdbc11.jar" Main
)
endlocal
