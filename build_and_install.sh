#!/usr/bin/env bash
# ==============================================================================
# Assembly Academy - Android Build & Installation Script
# Package: com.aistudio.assemblytutor.axpkqd
# Main Activity: com.example.MainActivity
# ==============================================================================

set -e

# ANSI Color Codes
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

APP_ID="com.aistudio.assemblytutor.axpkqd"
MAIN_ACTIVITY="com.example.MainActivity"
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

# Determine Gradle Command (use ./gradlew if available, otherwise fallback to system gradle)
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
else
    GRADLE_CMD=""
fi

print_banner() {
    echo -e "${CYAN}${BOLD}"
    echo "================================================================="
    echo "   ASSEMBLY ACADEMY - ANDROID BUILD & INSTALLATION WIZARD        "
    echo "================================================================="
    echo -e "${NC}"
}

print_help() {
    print_banner
    echo -e "${BOLD}Usage:${NC} ./build_and_install.sh [OPTION]"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --build           Build the debug APK without installing"
    echo "  --install         Build and install to connected Android device"
    echo "  --launch          Build, install, and launch the app on device"
    echo "  --release         Build release APK"
    echo "  --devices         List currently connected ADB devices"
    echo "  --wireless IP:PORT Connect to Android device wirelessly"
    echo "  --logcat          Stream logs from the app"
    echo "  --clean           Clean previous build artifacts"
    echo "  --help, -h        Display this guide"
    echo ""
    echo -e "${BOLD}Interactive Mode:${NC} Run without arguments for interactive menu."
}

check_prerequisites() {
    echo -e "${CYAN}[1/3] Checking Environment Prerequisites...${NC}"

    # 1. Check Java
    if command -v java &> /dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
        echo -e "  ${GREEN}✓${NC} Java Runtime detected: ${BOLD}$JAVA_VER${NC}"
    else
        echo -e "  ${RED}✗ Java is not installed or not in PATH!${NC}"
        echo -e "    Please install JDK 17 or higher: https://adoptium.net/"
        exit 1
    fi

    # 2. Check Gradle
    if [ -n "$GRADLE_CMD" ]; then
        echo -e "  ${GREEN}✓${NC} Gradle build tool detected: ${BOLD}$GRADLE_CMD${NC}"
    else
        echo -e "  ${RED}✗ Neither ./gradlew nor 'gradle' command was found in PATH!${NC}"
        echo -e "    Please install Gradle or generate Gradle wrapper using 'gradle wrapper'."
        exit 1
    fi

    # 3. Check ADB (Optional for build, required for install)
    if command -v adb &> /dev/null; then
        ADB_VER=$(adb --version | head -n 1)
        echo -e "  ${GREEN}✓${NC} Android Debug Bridge (ADB) detected: ${BOLD}$ADB_VER${NC}"
    else
        echo -e "  ${YELLOW}! ADB is not in PATH. Builds will work, but device install will require ADB.${NC}"
    fi

    # 4. Check & Auto-Configure Android SDK Location
    configure_android_sdk

    echo ""
}

configure_android_sdk() {
    local detected_sdk=""

    # 1. Check if ANDROID_HOME is already defined and directory exists
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        detected_sdk="$ANDROID_HOME"
    # 2. Check if ANDROID_SDK_ROOT is defined and directory exists
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        detected_sdk="$ANDROID_SDK_ROOT"
    fi

    # 3. Check if local.properties already exists with sdk.dir
    if [ -z "$detected_sdk" ] && [ -f "local.properties" ]; then
        local prop_dir
        prop_dir=$(grep -E "^sdk\.dir=" local.properties 2>/dev/null | cut -d'=' -f2- | tr -d '\r' | sed 's/\\:/:/g' | sed 's/\\\\/\//g')
        if [ -n "$prop_dir" ] && [ -d "$prop_dir" ]; then
            detected_sdk="$prop_dir"
        fi
    fi

    # 4. Auto-probe standard system paths
    if [ -z "$detected_sdk" ]; then
        # Standard Linux Android Studio SDK directory (e.g. /home/user/Android/Sdk)
        if [ -d "$HOME/Android/Sdk" ]; then
            detected_sdk="$HOME/Android/Sdk"
        # Standard macOS Android Studio SDK directory
        elif [ -d "$HOME/Library/Android/sdk" ]; then
            detected_sdk="$HOME/Library/Android/sdk"
        # Derive from adb location if it's in an SDK platform-tools folder
        elif command -v adb &> /dev/null; then
            local adb_bin
            adb_bin=$(command -v adb)
            local candidate
            candidate=$(dirname "$(dirname "$adb_bin")")
            if [ -d "$candidate/platforms" ] || [ -d "$candidate/build-tools" ]; then
                detected_sdk="$candidate"
            fi
        fi

        # Standard Linux distro package locations
        if [ -z "$detected_sdk" ] && [ -d "/usr/lib/android-sdk" ]; then
            detected_sdk="/usr/lib/android-sdk"
        fi
        if [ -z "$detected_sdk" ] && [ -d "/opt/android-sdk" ]; then
            detected_sdk="/opt/android-sdk"
        fi
    fi

    # 5. Apply detected SDK or prompt
    if [ -n "$detected_sdk" ]; then
        export ANDROID_HOME="$detected_sdk"
        export ANDROID_SDK_ROOT="$detected_sdk"
        echo -e "  ${GREEN}✓${NC} Android SDK located: ${BOLD}$detected_sdk${NC}"

        # Write to local.properties if missing or without sdk.dir
        if [ ! -f "local.properties" ] || ! grep -q "^sdk\.dir=" local.properties 2>/dev/null; then
            echo "# Auto-generated by build_and_install.sh" > local.properties
            echo "sdk.dir=$detected_sdk" >> local.properties
            echo -e "  ${GREEN}✓${NC} Auto-configured sdk.dir in local.properties"
        fi
    else
        echo -e "  ${YELLOW}! Android SDK was not automatically found in standard locations.${NC}"
        echo ""
        echo "  Please provide your Android SDK directory."
        echo "  (Tip: In Android Studio, check: Tools > SDK Manager > Android SDK Location)"
        echo "  Default on Linux is usually: $HOME/Android/Sdk"
        echo ""
        echo -n "  Enter Android SDK directory path: "
        read -r user_sdk
        if [ -n "$user_sdk" ] && [ -d "$user_sdk" ]; then
            export ANDROID_HOME="$user_sdk"
            export ANDROID_SDK_ROOT="$user_sdk"
            echo "# Auto-generated by build_and_install.sh" > local.properties
            echo "sdk.dir=$user_sdk" >> local.properties
            echo -e "  ${GREEN}✓${NC} Android SDK configured: ${BOLD}$user_sdk${NC}"
        else
            echo -e "  ${RED}✗ Invalid directory: '$user_sdk'. Gradle cannot build without Android SDK!${NC}"
            echo "  You can also set it in your shell: export ANDROID_HOME=\$HOME/Android/Sdk"
            exit 1
        fi
    fi
}

check_adb_device() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}[ERROR] 'adb' command not found!${NC}"
        echo "Please install Android SDK Platform-Tools or Android Studio."
        echo "Download platform-tools: https://developer.android.com/tools/releases/platform-tools"
        exit 1
    fi

    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
    if [ -z "$DEVICES" ]; then
        echo -e "${YELLOW}[!] No authorized Android device connected via ADB.${NC}"
        echo ""
        echo "Troubleshooting Checklist:"
        echo "  1. Connect your phone via USB cable."
        echo "  2. On your phone, enable 'Developer Options':"
        echo "     Settings > About Phone > Tap 'Build Number' 7 times."
        echo "  3. In Developer Options, enable 'USB Debugging' (and 'Install via USB' if on Xiaomi/MIUI/HyperOS)."
        echo "  4. Look at your phone screen and tap 'Always allow from this computer'."
        echo ""
        echo -n "Would you like to retry device detection now? (y/n): "
        read -r retry
        if [ "$retry" = "y" ] || [ "$retry" = "Y" ]; then
            check_adb_device
        else
            exit 1
        fi
    else
        echo -e "${GREEN}✓ Connected Device(s):${NC}"
        echo "$DEVICES" | while read -r dev; do
            MODEL=$(adb -s "$dev" shell getprop ro.product.model 2>/dev/null || echo "Android Device")
            echo -e "  • ${BOLD}$dev${NC} ($MODEL)"
        done
        echo ""
    fi
}

build_debug_apk() {
    echo -e "${CYAN}[2/3] Building Debug APK with Gradle...${NC}"
    $GRADLE_CMD :app:assembleDebug

    if [ -f "$DEBUG_APK" ]; then
        APK_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo -e "${GREEN}✓ Build Successful!${NC}"
        echo -e "  Location: ${BOLD}$DEBUG_APK${NC} ($APK_SIZE)"
        echo ""
    else
        echo -e "${RED}✗ Build failed: $DEBUG_APK was not found.${NC}"
        exit 1
    fi
}

build_release_apk() {
    echo -e "${CYAN}Building Release APK...${NC}"
    $GRADLE_CMD :app:assembleRelease

    echo -e "${GREEN}✓ Release build complete! Check app/build/outputs/apk/release/${NC}"
}

install_to_device() {
    check_adb_device
    echo -e "${CYAN}[3/3] Installing APK onto Android device via ADB...${NC}"
    
    adb install -r -d -g "$DEBUG_APK"
    
    echo -e "${GREEN}${BOLD}✓ Application successfully installed on your Android device!${NC}"
}

launch_app() {
    echo -e "${CYAN}Launching Assembly Academy on device...${NC}"
    adb shell am start -n "$APP_ID/$MAIN_ACTIVITY"
    echo -e "${GREEN}✓ App launched! Enjoy learning Assembly!${NC}"
}

stream_logcat() {
    echo -e "${CYAN}Streaming logs for $APP_ID (Press Ctrl+C to stop)...${NC}"
    adb logcat -v color | grep -E "($APP_ID|AssemblySimulator|AssemblyViewModel)"
}

connect_wireless() {
    local target="$1"
    if [ -z "$target" ]; then
        echo -n "Enter device IP and Port (e.g., 192.168.1.50:5555): "
        read -r target
    fi
    echo -e "${CYAN}Connecting to $target wirelessly...${NC}"
    adb connect "$target"
}

# --- CLI Argument Dispatcher ---
case "$1" in
    --help|-h)
        print_help
        exit 0
        ;;
    --build)
        print_banner
        check_prerequisites
        build_debug_apk
        exit 0
        ;;
    --install)
        print_banner
        check_prerequisites
        build_debug_apk
        install_to_device
        exit 0
        ;;
    --launch)
        print_banner
        check_prerequisites
        build_debug_apk
        install_to_device
        launch_app
        exit 0
        ;;
    --release)
        print_banner
        check_prerequisites
        build_release_apk
        exit 0
        ;;
    --devices)
        check_adb_device
        exit 0
        ;;
    --wireless)
        connect_wireless "$2"
        exit 0
        ;;
    --logcat)
        stream_logcat
        exit 0
        ;;
    --clean)
        echo -e "${CYAN}Cleaning build caches...${NC}"
        $GRADLE_CMD clean
        echo -e "${GREEN}✓ Clean complete.${NC}"
        exit 0
        ;;
    "")
        # Interactive Menu
        print_banner
        check_prerequisites
        
        echo -e "${BOLD}Select an action:${NC}"
        echo "  1) Build Debug APK"
        echo "  2) Build & Install to Connected Android Device"
        echo "  3) Build, Install & Launch Immediately"
        echo "  4) List Connected ADB Devices"
        echo "  5) Connect to Device via Wireless ADB"
        echo "  6) Stream App Logs (Logcat)"
        echo "  7) Build Release APK"
        echo "  8) Exit"
        echo ""
        echo -n "Enter choice [1-8]: "
        read -r choice

        case "$choice" in
            1)
                build_debug_apk
                ;;
            2)
                build_debug_apk
                install_to_device
                ;;
            3)
                build_debug_apk
                install_to_device
                launch_app
                ;;
            4)
                check_adb_device
                ;;
            5)
                connect_wireless
                ;;
            6)
                stream_logcat
                ;;
            7)
                build_release_apk
                ;;
            *)
                echo "Exiting."
                exit 0
                ;;
        esac
        ;;
    *)
        echo -e "${RED}Unknown option: $1${NC}"
        print_help
        exit 1
        ;;
esac
