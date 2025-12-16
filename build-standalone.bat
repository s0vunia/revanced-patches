@echo off
setlocal enabledelayedexpansion

:: TikTok ReVanced - Standalone Build Script
:: Builds a complete modded TikTok APK from original TikTok

echo ========================================
echo   TikTok ReVanced - Standalone Build
echo ========================================
echo.

:: Configuration
set "ANDROID_SDK=%USERPROFILE%\AppData\Local\Android\Sdk"
set "BUILD_TOOLS=%ANDROID_SDK%\build-tools\34.0.0"
set "NDK_PATH=%ANDROID_SDK%\ndk\27.2.12479018"
set "OUTPUT_DIR=output"
set "TEMP_DIR=%OUTPUT_DIR%\temp"
set "TIKTOK_APK=%~1"
set "MOD_VERSION=1.0.0"

:: Check if TikTok APK is provided
if "%TIKTOK_APK%"=="" (
    echo Usage: build-standalone.bat ^<tiktok-apk-path^>
    echo.
    echo Example: build-standalone.bat "C:\Downloads\TikTok_42.8.3.apk"
    echo.
    echo You can download the original TikTok APK from:
    echo   - APKMirror: https://www.apkmirror.com/apk/tiktok-pte-ltd/tiktok/
    echo   - APKPure: https://apkpure.com/tiktok/com.zhiliaoapp.musically
    exit /b 1
)

:: Check if APK exists
if not exist "%TIKTOK_APK%" (
    echo ERROR: APK file not found: %TIKTOK_APK%
    exit /b 1
)

:: Check for required tools
echo [1/8] Checking prerequisites...

if not exist "%BUILD_TOOLS%\zipalign.exe" (
    echo ERROR: Android build-tools not found at %BUILD_TOOLS%
    echo Please install Android SDK build-tools 34.0.0
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found in PATH
    echo Please install JDK 17 or later
    exit /b 1
)

echo       Prerequisites OK

:: Create output directories
echo [2/8] Setting up directories...
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
mkdir "%OUTPUT_DIR%" 2>nul
mkdir "%TEMP_DIR%"
echo       Done

:: Build native library (if NDK available)
echo [3/8] Building native library...
if exist "%NDK_PATH%\ndk-build.cmd" (
    pushd native
    call "%NDK_PATH%\ndk-build.cmd" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=jni/Android.mk NDK_APPLICATION_MK=jni/Application.mk
    if errorlevel 1 (
        echo WARNING: Native build failed, continuing without native library
    ) else (
        echo       Native library built successfully
    )
    popd
) else (
    echo       NDK not found, skipping native build
    echo       Using pre-built libraries if available
)

:: Build patches JAR
echo [4/8] Building patches...
call gradlew.bat :patches:build --no-daemon -q
if errorlevel 1 (
    echo ERROR: Patches build failed
    exit /b 1
)
echo       Patches built successfully

:: Build extensions
echo [5/8] Building extensions...
call gradlew.bat :extensions:assembleRelease --no-daemon -q
if errorlevel 1 (
    echo ERROR: Extensions build failed
    exit /b 1
)
echo       Extensions built successfully

:: Copy patches JAR location
set "PATCHES_JAR=patches\build\libs\patches-1.0.0.jar"
if not exist "%PATCHES_JAR%" (
    for %%f in (patches\build\libs\*.jar) do set "PATCHES_JAR=%%f"
)

:: Check for ReVanced CLI
echo [6/8] Preparing ReVanced CLI...
set "REVANCED_CLI="
for %%f in (revanced-cli*.jar) do set "REVANCED_CLI=%%f"

if "%REVANCED_CLI%"=="" (
    echo.
    echo ReVanced CLI not found. Downloading...
    echo Please download revanced-cli.jar from:
    echo   https://github.com/ReVanced/revanced-cli/releases
    echo.
    echo Place it in: %CD%
    echo.
    echo Alternatively, falling back to manual patching...
    goto :manual_patch
)

:: Patch with ReVanced CLI
echo [7/8] Patching APK with ReVanced CLI...
java -jar "%REVANCED_CLI%" patch ^
    --patches "%PATCHES_JAR%" ^
    --out "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-unaligned.apk" ^
    "%TIKTOK_APK%"

if errorlevel 1 (
    echo WARNING: ReVanced CLI patching failed, trying manual method...
    goto :manual_patch
)
goto :post_patch

:manual_patch
echo [7/8] Manual patching (without ReVanced CLI)...

:: Extract APK
echo       Extracting APK...
pushd "%TEMP_DIR%"
tar -xf "%TIKTOK_APK%" 2>nul || (
    powershell -command "Expand-Archive -Path '%TIKTOK_APK%' -DestinationPath '.' -Force"
)
popd

:: Copy native libraries if available
if exist "native\libs\arm64-v8a\libtikmod.so" (
    echo       Copying native libraries...
    if exist "%TEMP_DIR%\lib\arm64-v8a" (
        copy /y "native\libs\arm64-v8a\libtikmod.so" "%TEMP_DIR%\lib\arm64-v8a\libtikmod.so" >nul
    )
    if exist "%TEMP_DIR%\lib\armeabi-v7a" (
        copy /y "native\libs\armeabi-v7a\libtikmod.so" "%TEMP_DIR%\lib\armeabi-v7a\libtikmod.so" >nul
    )
)

:: Copy extension DEX
echo       Copying extensions...
set "EXTENSION_DEX=extensions\build\outputs\apk\release\extensions-release-unsigned.apk"
if exist "%EXTENSION_DEX%" (
    :: Extract classes.dex from extension APK and add to main APK
    mkdir "%TEMP_DIR%\ext_temp" 2>nul
    pushd "%TEMP_DIR%\ext_temp"
    tar -xf "%~dp0%EXTENSION_DEX%" 2>nul || (
        powershell -command "Expand-Archive -Path '%~dp0%EXTENSION_DEX%' -DestinationPath '.' -Force"
    )
    popd

    :: Find next available classes dex slot
    set "DEX_NUM=2"
    :find_dex_slot
    if exist "%TEMP_DIR%\classes!DEX_NUM!.dex" (
        set /a DEX_NUM+=1
        goto :find_dex_slot
    )

    if exist "%TEMP_DIR%\ext_temp\classes.dex" (
        copy /y "%TEMP_DIR%\ext_temp\classes.dex" "%TEMP_DIR%\classes!DEX_NUM!.dex" >nul
        echo       Extension DEX added as classes!DEX_NUM!.dex
    )
    rmdir /s /q "%TEMP_DIR%\ext_temp" 2>nul
)

:: Repack APK
echo       Repacking APK...
pushd "%TEMP_DIR%"
del /q "META-INF\*.SF" "META-INF\*.RSA" "META-INF\*.MF" 2>nul
jar -cfM "..\TikTok-ReVanced-%MOD_VERSION%-unaligned.apk" .
popd

echo       Done

:post_patch
:: Align APK
echo [8/8] Aligning and signing APK...
"%BUILD_TOOLS%\zipalign.exe" -f 4 "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-unaligned.apk" "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-aligned.apk"

:: Sign APK
set "KEYSTORE=%USERPROFILE%\.android\debug.keystore"
if not exist "%KEYSTORE%" (
    echo       Creating debug keystore...
    keytool -genkey -v -keystore "%KEYSTORE%" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>nul
)

"%BUILD_TOOLS%\apksigner.bat" sign --ks "%KEYSTORE%" --ks-pass pass:android --key-pass pass:android --out "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%.apk" "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-aligned.apk"

:: Cleanup
del /q "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-unaligned.apk" 2>nul
del /q "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%-aligned.apk" 2>nul
rmdir /s /q "%TEMP_DIR%" 2>nul

:: Get file size
for %%A in ("%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%.apk") do set "APK_SIZE=%%~zA"
set /a APK_SIZE_MB=%APK_SIZE%/1024/1024

:: Success message
echo.
echo ========================================
echo   BUILD COMPLETE
echo ========================================
echo.
echo Output: %OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%.apk
echo Size:   ~%APK_SIZE_MB% MB
echo.
echo Features included:
echo   - Watermark removal
echo   - Ad blocking
echo   - Feed filtering
echo   - Download enhancements
echo   - Region spoofing
echo   - SSL bypass (disabled by default)
echo   - Emulator bypass (disabled by default)
echo   - Remote config (user-configurable URL)
echo.
echo To install:
echo   adb install -r "%OUTPUT_DIR%\TikTok-ReVanced-%MOD_VERSION%.apk"
echo.
echo Or transfer to your device and install manually.
echo.

endlocal
