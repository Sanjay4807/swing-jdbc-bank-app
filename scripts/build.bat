@echo off
setlocal
cd /d "%~dp0\.."

echo ===================================================
echo   Compiling Apex Trust Bank Application
echo ===================================================

if not exist bin mkdir bin

set "JAVAC_CMD=javac"
if exist "C:\Program Files\Java\jdk-26.0.2\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-26.0.2\bin\javac.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
)

echo Using compiler: %JAVAC_CMD%
"%JAVAC_CMD%" -J-Xmx128m --release 8 -d bin -cp "lib\h2.jar;lib\ojdbc11.jar;src" src\model\*.java src\config\*.java src\dao\*.java src\ui\*.java src\Main.java test\*.java

if %ERRORLEVEL% equ 0 (
    echo [BUILD SUCCESS] All classes compiled to bin\ (Java 8 compatible bytecode)
) else (
    echo [BUILD ERROR] Compilation failed with error code %ERRORLEVEL%
)
pause
endlocal
