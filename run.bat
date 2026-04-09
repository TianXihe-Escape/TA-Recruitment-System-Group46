@echo off
setlocal

cd /d "%~dp0"

if not exist "target\classes\app\Main.class" (
    echo Cannot find compiled application files.
    echo Please run "mvn test" or otherwise build the project first.
    pause
    exit /b 1
)

set "JAVA_CMD="
if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_CMD=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_CMD if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD (
    where javaw >nul 2>nul
    if not errorlevel 1 set "JAVA_CMD=javaw"
)
if not defined JAVA_CMD (
    where java >nul 2>nul
    if not errorlevel 1 set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    echo Java was not found.
    echo Please install Java or set JAVA_HOME correctly.
    pause
    exit /b 1
)

start "" "%JAVA_CMD%" -cp "target\classes" app.Main
exit /b 0
