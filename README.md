# TikTok ReVanced Patches

Custom ReVanced patches for TikTok that:
1. **Remove ALL ads** from the feed
2. **Redirect download** to your Telegram bot

## Features

### Feed Filter Patch
- Removes all ads from feed
- Removes promoted/sponsored content
- Removes live streams (optional)
- Removes shop/e-commerce content
- Blocks Pangle/ByteDance ad SDK

### Telegram Redirect Patch
- Intercepts download button clicks
- Opens your Telegram bot instead of downloading
- Passes video ID to bot via start parameter
- Supports custom deep link formats

## Configuration

Edit `extensions/src/main/java/app/revanced/extension/tiktok/settings/Settings.java`:

```java
// Your Telegram bot username (without @)
public static String TELEGRAM_BOT_USERNAME = "YOUR_BOT_USERNAME";

// Include video ID in the bot link
public static boolean INCLUDE_VIDEO_ID = true;

// Enable/disable Telegram redirect
public static boolean TELEGRAM_REDIRECT_ENABLED = true;
```

## Building

### Prerequisites
- JDK 17 or higher
- Gradle 8.x

### Build Steps

1. Configure your bot username in `Settings.java`

2. Build the patches JAR:
```bash
./gradlew build
```

3. The patches JAR will be at `build/libs/tiktok-revanced-patches-1.0.0.jar`

## Using with ReVanced CLI

### Method 1: Using ReVanced CLI

```bash
# Download TikTok APK (version 36.5.4 recommended)
# Get ReVanced CLI and integrations

java -jar revanced-cli.jar patch \
    --patch-bundle tiktok-revanced-patches-1.0.0.jar \
    --include "Feed filter" \
    --include "Telegram redirect" \
    input-tiktok.apk
```

### Method 2: Using ReVanced Manager

1. Copy `tiktok-revanced-patches-1.0.0.jar` to your device
2. Open ReVanced Manager
3. Go to Settings → Sources
4. Add local patch bundle
5. Select TikTok and apply patches

## Compatible TikTok Versions

- `com.ss.android.ugc.trill` (TikTok Lite): 36.5.4, 37.x, 38.x
- `com.zhiliaoapp.musically` (TikTok): 36.5.4, 37.x, 38.x

## Telegram Bot Setup

Create a Telegram bot that handles the video ID:

```python
from telegram import Update
from telegram.ext import Application, CommandHandler, ContextTypes

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    # Get video ID from start parameter
    if context.args:
        video_id = context.args[0]
        # video_id format: "VIDEO_ID" or "VIDEO_ID_USERNAME"
        await update.message.reply_text(f"Processing video: {video_id}")
        # Download and send video here
    else:
        await update.message.reply_text("Welcome! Share a TikTok video to download.")

app = Application.builder().token("YOUR_BOT_TOKEN").build()
app.add_handler(CommandHandler("start", start))
app.run_polling()
```

## How It Works

### Ad Removal
1. Hooks TikTok's feed list processing
2. Filters items before display using reflection
3. Identifies ads by checking type fields and class names
4. Blocks Pangle ad SDK initialization

### Telegram Redirect
1. Hooks download button click handler
2. Extracts current video info (ID, author)
3. Builds Telegram URL with video ID as start param
4. Opens Telegram app with deep link

## Troubleshooting

### Fingerprints not found
TikTok obfuscates code differently per version. If patches don't work:
1. Use JADX to analyze your TikTok APK
2. Find the actual method names/signatures
3. Update fingerprints in `Fingerprints.kt`

### Download still works normally
1. Check `TELEGRAM_REDIRECT_ENABLED` is true
2. Verify bot username is correct
3. Check logs for errors

### Ads still showing
1. Enable `BLOCK_AD_SDK` in settings
2. Some ads may use different delivery methods
3. Check logcat for filter messages

## Project Structure

```
tiktok-revanced-patches/
├── patches/src/main/kotlin/
│   └── app/revanced/patches/tiktok/
│       ├── feedfilter/
│       │   ├── Fingerprints.kt      # Method signatures for ad filtering
│       │   └── FeedFilterPatch.kt   # Ad removal patch
│       ├── download/
│       │   ├── Fingerprints.kt      # Method signatures for download
│       │   └── TelegramRedirectPatch.kt  # Telegram redirect patch
│       └── misc/
│           ├── SharedExtensionPatch.kt
│           └── SettingsPatch.kt
├── extensions/src/main/java/
│   └── app/revanced/extension/tiktok/
│       ├── feedfilter/
│       │   └── FeedItemsFilter.java  # Ad filtering logic
│       ├── download/
│       │   └── TelegramRedirect.java # Telegram redirect logic
│       └── settings/
│           └── Settings.java         # Configuration
├── build.gradle.kts
└── README.md
```

## License

GPL-3.0 License
