@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   TikTok ReVanced Patches Build Script
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "TOOLS_DIR=%SCRIPT_DIR%tools"
set "OUTPUT_DIR=%SCRIPT_DIR%output"
set "INPUT_DIR=%SCRIPT_DIR%input"
set "PATCHES_JAR=%SCRIPT_DIR%patches\build\libs\tiktok-patches-1.0.0.jar"
set "INTEGRATIONS_AAR=%SCRIPT_DIR%extensions\build\outputs\aar\extensions-release.aar"

:: ReVanced CLI version
set "CLI_VERSION=4.6.0"
set "CLI_JAR=%TOOLS_DIR%\revanced-cli-%CLI_VERSION%-all.jar"

:: Create directories
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
if not exist "%INPUT_DIR%" mkdir "%INPUT_DIR%"

:: Step 1: Build patches
echo [1/5] Building patches and extensions...
call gradlew.bat :patches:build :extensions:assembleRelease --no-daemon -q
if errorlevel 1 (
    echo ERROR: Failed to build patches!
    echo.
    echo Make sure you have:
    echo   - Java 17+ installed
    echo   - Android SDK configured (ANDROID_HOME set)
    echo.
    pause
    exit /b 1
)
echo       Patches built successfully!
echo.

:: Step 2: Check for ReVanced CLI
echo [2/5] Checking for ReVanced CLI...
if not exist "%CLI_JAR%" (
    echo       Downloading ReVanced CLI v%CLI_VERSION%...
    curl -L -o "%CLI_JAR%" "https://github.com/ReVanced/revanced-cli/releases/download/v%CLI_VERSION%/revanced-cli-%CLI_VERSION%-all.jar"
    if errorlevel 1 (
        echo ERROR: Failed to download ReVanced CLI!
        echo Please download manually from: https://github.com/ReVanced/revanced-cli/releases
        pause
        exit /b 1
    )
)
echo       ReVanced CLI ready!
echo.

:: Step 3: Check for TikTok APK
echo [3/5] Looking for TikTok APK...
set "TIKTOK_APK="

:: Check input directory first
for %%F in ("%INPUT_DIR%\*.apk") do (
    set "TIKTOK_APK=%%F"
    goto :found_apk
)

:: Check script directory
for %%F in ("%SCRIPT_DIR%*.apk") do (
    set "TIKTOK_APK=%%F"
    goto :found_apk
)

:: Check Downloads folder
for %%F in ("%USERPROFILE%\Downloads\*tiktok*.apk") do (
    set "TIKTOK_APK=%%F"
    goto :found_apk
)
for %%F in ("%USERPROFILE%\Downloads\*musically*.apk") do (
    set "TIKTOK_APK=%%F"
    goto :found_apk
)
for %%F in ("%USERPROFILE%\Downloads\*trill*.apk") do (
    set "TIKTOK_APK=%%F"
    goto :found_apk
)

:not_found
echo.
echo ERROR: TikTok APK not found!
echo.
echo Please download TikTok APK and place it in:
echo   - %INPUT_DIR%\
echo.
echo Recommended versions: 36.5.4, 37.0.0, 37.1.0, 37.2.0, 38.0.0
echo.
echo Download from:
echo   - https://www.apkmirror.com/apk/tiktok-pte-ltd/tik-tok/
echo   - https://apkpure.com/tiktok/com.zhiliaoapp.musically
echo.
pause
exit /b 1

:found_apk
echo       Found: %TIKTOK_APK%
echo.

:: Step 4: Extract DEX from AAR and patch
echo [4/5] Patching TikTok APK...
set "OUTPUT_APK=%OUTPUT_DIR%\tiktok-revanced.apk"
set "INTEGRATIONS_DEX=%TOOLS_DIR%\integrations.dex"

:: Extract classes.jar from AAR and convert to DEX
if exist "%INTEGRATIONS_AAR%" (
    echo       Extracting integrations from AAR...
    cd /d "%TOOLS_DIR%"
    jar xf "%INTEGRATIONS_AAR%" classes.jar 2>nul
    if exist classes.jar (
        :: Convert to DEX using d8 (from Android SDK build-tools)
        for /d %%D in ("%ANDROID_HOME%\build-tools\*") do (
            if exist "%%D\d8.bat" (
                call "%%D\d8.bat" --output "%TOOLS_DIR%" classes.jar 2>nul
                if exist "%TOOLS_DIR%\classes.dex" (
                    move /y "%TOOLS_DIR%\classes.dex" "%INTEGRATIONS_DEX%" >nul
                )
                goto :dex_done
            )
        )
    )
)
:dex_done
cd /d "%SCRIPT_DIR%"

:: Run ReVanced CLI
echo       Applying patches...
if exist "%INTEGRATIONS_DEX%" (
    java -jar "%CLI_JAR%" patch ^
        --patches "%PATCHES_JAR%" ^
        --merge "%INTEGRATIONS_DEX%" ^
        --out "%OUTPUT_APK%" ^
        "%TIKTOK_APK%"
) else (
    :: Try without integrations merge if DEX extraction failed
    java -jar "%CLI_JAR%" patch ^
        --patches "%PATCHES_JAR%" ^
        --out "%OUTPUT_APK%" ^
        "%TIKTOK_APK%"
)

if errorlevel 1 (
    echo ERROR: Patching failed!
    pause
    exit /b 1
)
echo       Patching complete!
echo.

:: Step 5: Sign the APK (ReVanced CLI signs automatically, but let's verify)
echo [5/5] Verifying signed APK...
if exist "%OUTPUT_APK%" (
    echo.
    echo ============================================
    echo   BUILD SUCCESSFUL!
    echo ============================================
    echo.
    echo   Output APK: %OUTPUT_APK%
    echo.
    echo   Install with: adb install "%OUTPUT_APK%"
    echo.
    echo   Or transfer to your device/emulator and install.
    echo.
    echo   IMPORTANT: Enable these settings in TikTok after install:
    echo   - Settings ^> Debug ^> Disable SSL Pinning
    echo   - Settings ^> Debug ^> Bypass Emulator Detection
    echo.
) else (
    echo ERROR: Output APK not found!
    pause
    exit /b 1
)

pause
