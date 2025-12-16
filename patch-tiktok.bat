@echo off
setlocal enabledelayedexpansion

echo ===================================
echo TikTok ReVanced Patcher
echo ===================================
echo.

set "REVANCED_CLI_VERSION=4.6.0"
set "REVANCED_CLI_JAR=revanced-cli-%REVANCED_CLI_VERSION%-all.jar"
set "TIKTOK_APK=%1"
set "OUTPUT_APK=tiktok-revanced-patched.apk"

REM Check if TikTok APK is provided
if "%TIKTOK_APK%"=="" (
    echo Usage: patch-tiktok.bat ^<tiktok.apk^>
    echo.
    echo Example: patch-tiktok.bat com.zhiliaoapp.musically_38.0.0.apk
    echo.
    echo Download TikTok 38.0.0 from APKMirror:
    echo https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/
    exit /b 1
)

REM Check if APK exists
if not exist "%TIKTOK_APK%" (
    echo ERROR: File not found: %TIKTOK_APK%
    exit /b 1
)

REM Download revanced-cli if not present
if not exist "%REVANCED_CLI_JAR%" (
    echo Downloading ReVanced CLI v%REVANCED_CLI_VERSION%...
    curl -L -o "%REVANCED_CLI_JAR%" "https://github.com/ReVanced/revanced-cli/releases/download/v%REVANCED_CLI_VERSION%/revanced-cli-%REVANCED_CLI_VERSION%-all.jar"
    if errorlevel 1 (
        echo ERROR: Failed to download ReVanced CLI
        exit /b 1
    )
)

REM Build patches if jar doesn't exist
if not exist "patches\build\libs\tiktok-patches-1.0.0.jar" (
    echo Building patches...
    if exist gradlew.bat (
        call gradlew.bat :patches:jar
    ) else (
        echo ERROR: Gradle wrapper not found. Please install Gradle and run: gradle wrapper
        exit /b 1
    )
)

REM Build extensions if needed
if not exist "extensions\build\outputs\aar\extensions-release.aar" (
    echo Building extensions...
    if exist gradlew.bat (
        call gradlew.bat :extensions:assembleRelease
    )
)

echo.
echo Patching TikTok...
echo.

java -jar "%REVANCED_CLI_JAR%" patch ^
    --patches "patches\build\libs\tiktok-patches-1.0.0.jar" ^
    --out "%OUTPUT_APK%" ^
    "%TIKTOK_APK%"

if errorlevel 1 (
    echo.
    echo ERROR: Patching failed!
    exit /b 1
)

echo.
echo ===================================
echo SUCCESS! Patched APK: %OUTPUT_APK%
echo ===================================
echo.
echo To install on device:
echo   adb install -r %OUTPUT_APK%
echo.
echo Or transfer %OUTPUT_APK% to your phone and install manually.
echo.

endlocal
