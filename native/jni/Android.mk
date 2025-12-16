LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := tikmod
LOCAL_SRC_FILES := \
    ../src/tikmod.cpp \
    ../src/settings.cpp \
    ../src/feed_processor.cpp \
    ../src/watermark.cpp \
    ../src/hooks.cpp \
    ../src/special_clinit.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_CPPFLAGS := -std=c++17 -fno-exceptions -fno-rtti -O2 -DNDEBUG
LOCAL_LDLIBS := -llog -landroid

# Strip debug info for smaller binary
LOCAL_STRIP_MODULE := true

include $(BUILD_SHARED_LIBRARY)
