# TikTok 43.x Reverse Engineering Investigation

## Problem
Patched TikTok 43.1.4 APK crashes a few seconds after startup, even with minimal patches that don't use extension hooks.

## Environment
- **Original APK**: `C:\Users\s0vunia\Downloads\43.1.4_universal_fix.apk`
- **Decompiled smali**: `C:\Users\s0vunia\Downloads\tiktok_43_smali/`
- **Patches source**: `C:\Users\s0vunia\work\tiktok-revanced-patches\patches\src\main\kotlin\app\revanced\patches\tiktok\`

## Crash Investigation Tasks

### 1. Check Anti-Tampering Mechanisms
TikTok likely has integrity checks. Search for:

```bash
# Search for signature verification
grep -r "getPackageInfo" tiktok_43_smali/ --include="*.smali" | head -20
grep -r "signatures" tiktok_43_smali/ --include="*.smali" | head -20
grep -r "checkSignature" tiktok_43_smali/ --include="*.smali" | head -20

# Search for hash/checksum verification
grep -r "MessageDigest" tiktok_43_smali/ --include="*.smali" | head -20
grep -r "SHA" tiktok_43_smali/ --include="*.smali" | head -20

# Search for native integrity checks
grep -r "System.exit" tiktok_43_smali/ --include="*.smali" | head -20
grep -r "killProcess" tiktok_43_smali/ --include="*.smali" | head -20
```

### 2. Check Extension Hook Point
We changed the extension hook from `MainActivity.onCreate()` (native) to `AwemeHostApplication.onCreate()`.

Verify the hook point:
```
File: tiktok_43_smali/smali_classes30/com/ss/android/ugc/aweme/app/host/AwemeHostApplication.smali
```

Check if:
- The method has correct signature: `onCreate()V`
- It's not native
- It has enough registers for our injection

### 3. Analyze Patches Applied

**Minimal patches that still crash:**
1. `Disable login requirement` - patches `checkLogin` methods
2. `Fix Google login` - patches Google sign-in flow
3. `Show seekbar` - UI modification
4. `Remember clear display` - preference storage

Look at fingerprints in:
- `patches/src/main/kotlin/app/revanced/patches/tiktok/misc/login/`
- `patches/src/main/kotlin/app/revanced/patches/tiktok/interaction/seekbar/`
- `patches/src/main/kotlin/app/revanced/patches/tiktok/interaction/cleardisplay/`

### 4. Find Crash Point

Search for crash-related code:
```bash
# Look for UncaughtExceptionHandler
grep -r "UncaughtExceptionHandler" tiktok_43_smali/ --include="*.smali" | head -10

# Look for crash reporting
grep -r "Crashlytics\|crashlytics" tiktok_43_smali/ --include="*.smali" | head -10
grep -r "ACRA\|BugSnag\|Sentry" tiktok_43_smali/ --include="*.smali" | head -10
```

### 5. Key Classes to Investigate

1. **AwemeHostApplication** - App entry point
   - `smali_classes30/com/ss/android/ugc/aweme/app/host/AwemeHostApplication.smali`

2. **MainActivity** - Now has native onCreate
   - `smali_classes30/com/ss/android/ugc/aweme/main/MainActivity.smali`

3. **Security/Integrity checks**
   - Search for classes with "Security", "Verify", "Check", "Integrity" in name

4. **Startup tasks**
   - `JatoInitTask` - `smali_classes30/com/ss/android/ugc/aweme/legoImp/task/JatoInitTask.smali`
   - `StoreRegionInitTask` - `smali_classes16/com/ss/android/ugc/aweme/legoImp/task/StoreRegionInitTask.smali`

### 6. Compare with Working Version

TikTok 42.8.3 worked. Compare:
1. Class structure differences
2. Native method changes
3. Security mechanism differences

### 7. Possible Solutions

1. **Disable anti-tampering**: Find and patch signature/integrity checks
2. **Fix hook point**: Find alternative early initialization hook
3. **Use different approach**: Hook later in lifecycle (Activity.onResume, etc.)
4. **Native library bypass**: Check if native libs verify APK integrity

### 8. Useful Commands

```bash
# Decompile with apktool (already done)
apktool d 43.1.4_universal_fix.apk -o tiktok_43_smali

# Search for specific patterns
grep -rn "PATTERN" tiktok_43_smali/ --include="*.smali"

# Find all methods in a class
grep "\.method" path/to/Class.smali

# Count classes
find tiktok_43_smali/ -name "*.smali" | wc -l
```

### 9. Expected Deliverables

1. Identify the crash cause
2. Find where signature/integrity verification happens
3. Propose patches to bypass anti-tampering
4. Find working hook points for extension initialization
5. Test fixes and confirm app launches successfully

## Notes

- The original APK (`43.1.4_universal_fix.apk`) should be tested to confirm it works unmodified
- Even minimal patches cause crash, suggesting anti-tampering detection
- Native methods cannot be hooked with bytecode patches
- TikTok 43.x moved many methods to native code for obfuscation/performance
