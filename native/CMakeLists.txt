cmake_minimum_required(VERSION 3.18.1)

project("tikmod")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build the native library
add_library(tikmod SHARED
    src/tikmod.cpp
    src/settings.cpp
    src/feed_processor.cpp
    src/watermark.cpp
    src/hooks.cpp
    src/special_clinit.cpp
)

# Find required packages
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
target_link_libraries(tikmod
    ${log-lib}
    ${android-lib}
)

# Include directories
target_include_directories(tikmod PRIVATE
    ${CMAKE_SOURCE_DIR}/jni
)

# Optimization flags
target_compile_options(tikmod PRIVATE
    -O2
    -fvisibility=hidden
    -ffunction-sections
    -fdata-sections
)

target_link_options(tikmod PRIVATE
    -Wl,--gc-sections
    -Wl,--strip-all
)
