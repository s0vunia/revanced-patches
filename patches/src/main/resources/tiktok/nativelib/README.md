# TikMod Native Library Resources

This directory contains the compiled native libraries that replace `libtigrik.so`.

## Directory Structure

```
nativelib/
├── arm64-v8a/
│   └── libtigrik.so    # 64-bit ARM (most modern devices)
├── armeabi-v7a/
│   └── libtigrik.so    # 32-bit ARM (older devices)
└── README.md
```

## Building the Native Libraries

1. Make sure you have Android NDK installed
2. Run the build script from the `native/` directory:

```bash
cd native
./build-and-copy.bat   # Windows
./build-and-copy.sh    # Linux/Mac
```

This will:
1. Compile `libtikmod.so` for arm64-v8a and armeabi-v7a
2. Rename them to `libtigrik.so`
3. Copy them to this resources directory

## What This Does

The original `libtigrik.so` from TikTok Mod Cloud contains:
- ✅ Watermark removal
- ✅ Ad blocking
- ✅ Feed filtering
- ✅ Download features
- ❌ **Remote kill switch** (allows mod developer to remotely disable app)
- ❌ **Cloud banner ads** (fetches ads from mod developer's server)
- ❌ **Remote config** (fetches settings from external URL)

Our replacement `libtikmod.so` (renamed to `libtigrik.so`) contains:
- ✅ Watermark removal
- ✅ Ad blocking
- ✅ Feed filtering
- ✅ Download features
- ✅ **NO remote kill switch**
- ✅ **NO cloud banner ads**
- ✅ **Local-only settings**

## Native Method Registration

The library uses a dynamic registration system:
- `tigrik0/tigrik` class loads the library
- `tigrik0/hidden/Hidden0` has 74 `special_clinit_XX_YY` methods
- Each method registers native implementations for specific class indices

Cloud control indices that we skip:
- 53-54: KillAppReceiver (kill switch)
- 63-73: Banner system (cloud ads)

## Manual Installation

If you don't want to compile, you can manually place pre-compiled `.so` files here:
1. Get the compiled `libtikmod.so` files
2. Rename them to `libtigrik.so`
3. Place in the appropriate architecture folder
