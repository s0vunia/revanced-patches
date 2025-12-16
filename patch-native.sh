#!/bin/bash
# TikMod Native Library Patcher
# Replaces libtigrik.so with our cloud-control-free version

set -e

# Configuration
INPUT_APK="${1:-/c/Users/s0vunia/Downloads/42.8.3_universal.apk}"
OUTPUT_DIR="./output"
NATIVE_LIB_ARM64="./native/libs/arm64-v8a/libtikmod.so"
NATIVE_LIB_ARM="./native/libs/armeabi-v7a/libtikmod.so"
BUILD_TOOLS="/c/Users/s0vunia/AppData/Local/Android/Sdk/build-tools/34.0.0"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  TikMod Native Library Patcher${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check input APK
if [ ! -f "$INPUT_APK" ]; then
    echo -e "${RED}ERROR: Input APK not found: $INPUT_APK${NC}"
    exit 1
fi

# Check native libraries
if [ ! -f "$NATIVE_LIB_ARM64" ]; then
    echo -e "${RED}ERROR: Native library not found: $NATIVE_LIB_ARM64${NC}"
    echo "Run 'cd native && ./build-and-copy.bat' first"
    exit 1
fi

echo -e "${YELLOW}Input APK:${NC} $INPUT_APK"
echo -e "${YELLOW}Output:${NC} $OUTPUT_DIR"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"
rm -rf "$OUTPUT_DIR/temp"
mkdir -p "$OUTPUT_DIR/temp"

# Step 1: Extract APK
echo -e "${GREEN}[1/5]${NC} Extracting APK..."
cd "$OUTPUT_DIR/temp"
unzip -q "$INPUT_APK"
echo "      Done"

# Step 2: Replace native libraries
echo -e "${GREEN}[2/5]${NC} Replacing native libraries..."

if [ -d "lib/arm64-v8a" ] && [ -f "lib/arm64-v8a/libtigrik.so" ]; then
    cp "../../$NATIVE_LIB_ARM64" "lib/arm64-v8a/libtigrik.so"
    echo "      arm64-v8a: replaced"
else
    echo "      arm64-v8a: not found in APK (skipping)"
fi

if [ -d "lib/armeabi-v7a" ] && [ -f "lib/armeabi-v7a/libtigrik.so" ]; then
    cp "../../$NATIVE_LIB_ARM" "lib/armeabi-v7a/libtigrik.so"
    echo "      armeabi-v7a: replaced"
else
    echo "      armeabi-v7a: not found in APK (skipping)"
fi

# Step 3: Repack APK
echo -e "${GREEN}[3/5]${NC} Repacking APK..."
zip -q -r "../tiktok-tikmod-unaligned.apk" .
cd ..
echo "      Done"

# Step 4: Align APK
echo -e "${GREEN}[4/5]${NC} Aligning APK..."
"$BUILD_TOOLS/zipalign" -f 4 "tiktok-tikmod-unaligned.apk" "tiktok-tikmod-aligned.apk"
echo "      Done"

# Step 5: Sign APK
echo -e "${GREEN}[5/5]${NC} Signing APK..."

# Generate debug keystore if not exists
KEYSTORE="$HOME/.android/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    echo "      Creating debug keystore..."
    keytool -genkey -v -keystore "$KEYSTORE" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || true
fi

"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --key-pass pass:android --out "tiktok-tikmod-signed.apk" "tiktok-tikmod-aligned.apk"
echo "      Done"

# Cleanup
rm -rf temp
rm -f "tiktok-tikmod-unaligned.apk" "tiktok-tikmod-aligned.apk"

# Final output
cd ..
FINAL_APK="$OUTPUT_DIR/tiktok-tikmod-signed.apk"
SIZE=$(ls -lh "$FINAL_APK" | awk '{print $5}')

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  BUILD COMPLETE${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Output: ${YELLOW}$FINAL_APK${NC}"
echo -e "Size:   ${YELLOW}$SIZE${NC}"
echo ""
echo -e "${GREEN}Changes:${NC}"
echo "  - libtigrik.so replaced with TikMod"
echo "  - NO remote kill switch"
echo "  - NO cloud banner ads"
echo "  - Local-only settings"
echo ""
