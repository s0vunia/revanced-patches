@echo off
setlocal

REM TikMod Native Library Build Script
REM Requires Android NDK to be installed

REM Try to find NDK
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

REM Clean previous build
if exist "libs" rmdir /s /q libs
if exist "obj" rmdir /s /q obj

REM Build using ndk-build
echo Building native library...
call "%NDK_PATH%\ndk-build.cmd" -C "%~dp0" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=jni/Android.mk NDK_APPLICATION_MK=jni/Application.mk

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo.
echo Build successful!
echo Output libraries:
dir /b /s libs\*.so 2>nul

REM Copy to output directory
if not exist "output" mkdir output
copy /y libs\arm64-v8a\libtikmod.so output\libtikmod_arm64.so >nul 2>&1
copy /y libs\armeabi-v7a\libtikmod.so output\libtikmod_arm.so >nul 2>&1

echo.
echo Libraries copied to output\ directory
echo.
echo To use as drop-in replacement for libtigrik.so:
echo   rename libtikmod_arm64.so to libtigrik.so
echo   place in lib/arm64-v8a/ of the APK

endlocal
