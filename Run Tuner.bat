@echo off
echo.
echo ================================================
echo           Harmonic Tuner - Launcher
echo ================================================
echo.
echo This launcher will compile and run the tuner.
echo.
pause

cd /d "%~dp0"
set "PROJECT_DIR=%cd%"
set "SRC_DIR=%PROJECT_DIR%\src"

echo.
echo Setting up Java environment...
echo.

REM Check if we need to use bundled Java or system Java
if exist "%PROJECT_DIR%\java-8\bin\java.exe" (
    set "JAVA_HOME=%PROJECT_DIR%\java-8"
    echo Using bundled Java installation...
) else (
    echo Looking for Java in the system...
    java -version >nul 2>&1
    if %errorlevel% neq 0 (
        echo.
        echo ERROR: Java not found!
        echo Please install Java 8 or higher from: https://www.oracle.com/java/technologies/javase-downloads.html
        echo.
        pause
        exit /b 1
    )
    set "JAVA_HOME="
    echo Using system Java installation...
)

REM Set up PATH
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo.
echo Compiling Java files...
cd "%SRC_DIR%"
javac com\harmonic\tuner\*.java
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation failed!
    echo.
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo.
echo Starting Harmonic Tuner...
echo.

java com.harmonic.tuner.Main

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to run the tuner!
    echo.
)

echo.
echo Tuner closed.
pause