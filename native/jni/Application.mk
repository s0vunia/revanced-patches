# Application configuration for TikMod native library

# Target ABIs - arm64-v8a is primary, armeabi-v7a for older devices
APP_ABI := arm64-v8a armeabi-v7a

# Minimum API level (TikTok requires Android 5.0+)
APP_PLATFORM := android-21

# Use clang (default in modern NDK)
APP_STL := c++_static

# Optimization
APP_OPTIM := release

# C++ standard
APP_CPPFLAGS := -std=c++17
