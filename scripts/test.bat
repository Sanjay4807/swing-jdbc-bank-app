@echo off
setlocal
cd /d "%~dp0\.."

set "JAVA_CMD=java"
if exist "C:\Program Files\Java\jdk-26.0.2\bin\java.exe" (
    set "JAVA_CMD=C:\Program Files\Java\jdk-26.0.2\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

echo Running Automated Banking System Smoke and Integration Tests with %JAVA_CMD%...
"%JAVA_CMD%" -Xmx128m -cp "bin;lib\h2.jar;lib\ojdbc11.jar" test.IntegrationTest
pause
endlocal
