@echo off
echo ============================================
echo   TikTok ReVanced Patches - Setup
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "LIBS_DIR=%SCRIPT_DIR%libs"
set "TOOLS_DIR=%SCRIPT_DIR%tools"
set "INPUT_DIR=%SCRIPT_DIR%input"

:: Create directories
if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
if not exist "%INPUT_DIR%" mkdir "%INPUT_DIR%"

:: Check GitHub Authentication
echo [1/4] Checking GitHub Authentication...
if not defined GITHUB_ACTOR (
    echo.
    echo  WARNING: GitHub authentication not configured!
    echo.
    echo  ReVanced Patcher requires a GitHub token to download.
    echo.
    echo  SETUP INSTRUCTIONS:
    echo  1. Go to: https://github.com/settings/tokens
    echo  2. Click "Generate new token (classic)"
    echo  3. Give it a name like "revanced-build"
    echo  4. Select scope: "read:packages"
    echo  5. Click "Generate token" and copy it
    echo.
    echo  6. Set environment variables:
    echo     setx GITHUB_ACTOR your_github_username
    echo     setx GITHUB_TOKEN your_token_here
    echo.
    echo  7. Restart this terminal and run setup.bat again
    echo.
    pause
    exit /b 1
)
echo       GITHUB_ACTOR: %GITHUB_ACTOR%
echo       GITHUB_TOKEN: [configured]
echo.

:: Download ReVanced CLI
echo [2/3] Downloading ReVanced CLI...
set "CLI_URL=https://github.com/ReVanced/revanced-cli/releases/download/v4.6.0/revanced-cli-4.6.0-all.jar"
set "CLI_JAR=%TOOLS_DIR%\revanced-cli-4.6.0-all.jar"

if not exist "%CLI_JAR%" (
    curl -L -o "%CLI_JAR%" "%CLI_URL%"
    if errorlevel 1 (
        echo ERROR: Failed to download ReVanced CLI
        echo Please download manually from: %CLI_URL%
        echo And place in: %TOOLS_DIR%
        pause
        exit /b 1
    )
) else (
    echo       Already exists, skipping...
)
echo       Done!
echo.

:: Check for Android SDK
echo [3/3] Checking Android SDK...
if defined ANDROID_HOME (
    echo       ANDROID_HOME: %ANDROID_HOME%
) else (
    echo       WARNING: ANDROID_HOME not set
    echo       Checking default location...
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        echo       Found: %LOCALAPPDATA%\Android\Sdk
        echo sdk.dir=%LOCALAPPDATA:\=\\%\\Android\\Sdk> "%SCRIPT_DIR%local.properties"
    ) else (
        echo       Android SDK not found!
        echo       Please install Android Studio or set ANDROID_HOME
    )
)
echo.

echo ============================================
echo   Setup Complete!
echo ============================================
echo.
echo Next steps:
echo   1. Download TikTok APK and place in: %SCRIPT_DIR%input\
echo   2. Run: build-tiktok.bat
echo.
pause
