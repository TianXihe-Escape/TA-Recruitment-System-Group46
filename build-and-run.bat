@echo off
setlocal EnableDelayedExpansion

cd /d "%~dp0"

if not exist "src\main\java\app\Main.java" (
    echo Cannot find src\main\java\app\Main.java.
    echo Please run this script from the project root folder.
    pause
    exit /b 1
)

set "JAVAC_CMD="
if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
if not defined JAVAC_CMD (
    where javac >nul 2>nul
    if not errorlevel 1 set "JAVAC_CMD=javac"
)
if not defined JAVAC_CMD (
    echo javac was not found.
    echo Please install JDK 17 or later, or set JAVA_HOME correctly.
    pause
    exit /b 1
)

set "JAVA_CMD="
if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_CMD (
    where java >nul 2>nul
    if not errorlevel 1 set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    echo java was not found.
    echo Please install JDK 17 or later, or set JAVA_HOME correctly.
    pause
    exit /b 1
)

if not exist "target" mkdir "target"
if not exist "target\classes" mkdir "target\classes"

set "SOURCE_LIST=target\sources-main.txt"
type nul > "%SOURCE_LIST%"
for /r "src\main\java" %%F in (*.java) do (
    set "SOURCE_FILE=%%F"
    set "SOURCE_FILE=!SOURCE_FILE:\=/!"
    echo "!SOURCE_FILE!">>"%SOURCE_LIST%"
)

echo Compiling Java source files with UTF-8 encoding...
"%JAVAC_CMD%" -encoding UTF-8 -d "target\classes" @"%SOURCE_LIST%"
if errorlevel 1 (
    echo.
    echo Compilation failed. Please fix the Java errors shown above, then run this script again.
    pause
    exit /b 1
)

echo.
echo Compilation succeeded. Starting the TA Recruitment System...
"%JAVA_CMD%" -cp "target\classes" app.Main
if errorlevel 1 (
    echo.
    echo The application exited with an error.
    pause
    exit /b 1
)

exit /b 0
