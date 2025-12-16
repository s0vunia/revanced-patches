@echo off
setlocal

REM TikMod Native Library Build and Copy Script
REM Builds the native library and copies it to the patches resources directory

echo ============================================
echo   TikMod Native Library Build Script
echo ============================================
echo.

REM Find NDK
if defined ANDROID_NDK_HOME (
    set NDK_PATH=%ANDROID_NDK_HOME%
    goto :found_ndk
)

if defined NDK_ROOT (
    set NDK_PATH=%NDK_ROOT%
    goto :found_ndk
)

REM Common NDK locations
if exist "%LOCALAPPDATA%\Android\Sdk\ndk" (
    for /f "delims=" %%i in ('dir /b /ad /o-n "%LOCALAPPDATA%\Android\Sdk\ndk"') do (
        set NDK_PATH=%LOCALAPPDATA%\Android\Sdk\ndk\%%i
        goto :found_ndk
    )
)

if exist "C:\Android\ndk" (
    for /f "delims=" %%i in ('dir /b /ad /o-n "C:\Android\ndk"') do (
        set NDK_PATH=C:\Android\ndk\%%i
        goto :found_ndk
    )
)

echo ERROR: Android NDK not found!
echo Please set ANDROID_NDK_HOME environment variable
echo Or install NDK via Android Studio SDK Manager
exit /b 1

:found_ndk
echo Using NDK: %NDK_PATH%
echo.

REM Set paths
set SCRIPT_DIR=%~dp0
set PATCHES_RES=%SCRIPT_DIR%..\patches\src\main\resources\tiktok\nativelib

REM Clean previous build
echo [1/4] Cleaning previous build...
if exist "%SCRIPT_DIR%libs" rmdir /s /q "%SCRIPT_DIR%libs"
if exist "%SCRIPT_DIR%obj" rmdir /s /q "%SCRIPT_DIR%obj"

REM Build using ndk-build
echo [2/4] Building native library...
call "%NDK_PATH%\ndk-build.cmd" -C "%SCRIPT_DIR%" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=jni/Android.mk NDK_APPLICATION_MK=jni/Application.mk

if errorlevel 1 (
    echo ERROR: Build failed!
    exit /b 1
)

echo       Build successful!
echo.

REM Create output directories
echo [3/4] Creating output directories...
if not exist "%PATCHES_RES%\arm64-v8a" mkdir "%PATCHES_RES%\arm64-v8a"
if not exist "%PATCHES_RES%\armeabi-v7a" mkdir "%PATCHES_RES%\armeabi-v7a"

REM Copy and rename libraries
echo [4/4] Copying libraries to patches resources...

REM arm64-v8a
if exist "%SCRIPT_DIR%libs\arm64-v8a\libtikmod.so" (
    copy /y "%SCRIPT_DIR%libs\arm64-v8a\libtikmod.so" "%PATCHES_RES%\arm64-v8a\libtigrik.so" >nul
    echo       arm64-v8a: OK
) else (
    echo       arm64-v8a: NOT FOUND
)

REM armeabi-v7a
if exist "%SCRIPT_DIR%libs\armeabi-v7a\libtikmod.so" (
    copy /y "%SCRIPT_DIR%libs\armeabi-v7a\libtikmod.so" "%PATCHES_RES%\armeabi-v7a\libtigrik.so" >nul
    echo       armeabi-v7a: OK
) else (
    echo       armeabi-v7a: NOT FOUND
)

echo.
echo ============================================
echo   BUILD COMPLETE
echo ============================================
echo.
echo Native libraries copied to:
echo   %PATCHES_RES%
echo.
echo Files:
dir /b "%PATCHES_RES%\arm64-v8a\*.so" 2>nul
dir /b "%PATCHES_RES%\armeabi-v7a\*.so" 2>nul
echo.
echo The patches are now ready to be built with:
echo   gradlew :patches:build
echo.

endlocal
