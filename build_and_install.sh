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

    # 1. Check & Validate Java Version (AGP requires JDK 17 or 21)
    check_java_version

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

    # 5. Check & Auto-Restore debug.keystore
    ensure_debug_keystore

    echo ""
}

ensure_debug_keystore() {
    if [ ! -f "debug.keystore" ]; then
        if [ -f "debug.keystore.base64" ]; then
            echo -e "  ${CYAN}ℹ Restoring debug.keystore from debug.keystore.base64...${NC}"
            base64 -d debug.keystore.base64 > debug.keystore 2>/dev/null || base64 --decode debug.keystore.base64 > debug.keystore
            echo -e "  ${GREEN}✓${NC} debug.keystore restored successfully."
        elif command -v keytool &> /dev/null; then
            echo -e "  ${CYAN}ℹ Generating debug.keystore with keytool...${NC}"
            keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null || true
            echo -e "  ${GREEN}✓${NC} Generated debug.keystore."
        else
            echo -e "  ${YELLOW}! Warning: debug.keystore not found.${NC}"
        fi
    else
        echo -e "  ${GREEN}✓${NC} Debug signing keystore detected: ${BOLD}debug.keystore${NC}"
    fi
}

check_java_version() {
    local java_bin=""
    local java_major=0

    # Function to extract major version from a java binary
    get_java_major() {
        local jbin="$1"
        "$jbin" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | awk -F '.' '{
            if ($1 == "1") { print $2 } else { print $1 }
        }'
    }

    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        java_bin="$JAVA_HOME/bin/java"
    elif command -v java &> /dev/null; then
        java_bin=$(command -v java)
    fi

    if [ -n "$java_bin" ]; then
        java_major=$(get_java_major "$java_bin")
    else
        echo -e "  ${RED}✗ Java is not installed or not in PATH!${NC}"
        echo -e "    Please install JDK 17 or JDK 21: https://adoptium.net/"
        exit 1
    fi

    # Android Gradle Plugin officially supports JDK 17 and JDK 21.
    # Versions like Java 25, 26, 27+ break AGP's JdkImageTransform jlink execution!
    if [ -z "$java_major" ] || [ "$java_major" -gt 21 ] || [ "$java_major" -lt 17 ]; then
        echo -e "  ${YELLOW}! Current Java version ($java_major) is incompatible with Android Gradle Plugin.${NC}"
        echo -e "    (Java 27 causes jlink module transformation errors. AGP requires JDK 17 or JDK 21)."
        echo -e "    Searching system for installed JDK 17 or JDK 21..."

        local candidate_dirs=(
            "/usr/lib/jvm/java-17-openjdk"
            "/usr/lib/jvm/java-17-openjdk-amd64"
            "/usr/lib/jvm/java-21-openjdk"
            "/usr/lib/jvm/java-21-openjdk-amd64"
            "/usr/lib/jvm/temurin-17-jdk"
            "/usr/lib/jvm/temurin-21-jdk"
            "/opt/android-studio/jbr"
            "/usr/local/android-studio/jbr"
            "/snap/android-studio/current/jbr"
            "$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr"
            "$HOME/Android/Sdk/jbr"
        )

        local found_compatible=""
        for cdir in "${candidate_dirs[@]}"; do
            if [ -x "$cdir/bin/java" ]; then
                local cmaj
                cmaj=$(get_java_major "$cdir/bin/java")
                if [ -n "$cmaj" ] && [ "$cmaj" -ge 17 ] && [ "$cmaj" -le 21 ]; then
                    found_compatible="$cdir"
                    break
                fi
            fi
        done

        # Also search in /usr/lib/jvm/*
        if [ -z "$found_compatible" ] && [ -d "/usr/lib/jvm" ]; then
            for cdir in /usr/lib/jvm/*17* /usr/lib/jvm/*21*; do
                if [ -d "$cdir" ] && [ -x "$cdir/bin/java" ]; then
                    local cmaj
                    cmaj=$(get_java_major "$cdir/bin/java")
                    if [ -n "$cmaj" ] && [ "$cmaj" -ge 17 ] && [ "$cmaj" -le 21 ]; then
                        found_compatible="$cdir"
                        break
                    fi
                fi
            done
        fi

        if [ -n "$found_compatible" ]; then
            export JAVA_HOME="$found_compatible"
            export PATH="$JAVA_HOME/bin:$PATH"
            local new_ver
            new_ver=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
            echo -e "  ${GREEN}✓${NC} Auto-switched JAVA_HOME to compatible JDK: ${BOLD}$found_compatible${NC} ($new_ver)"
        else
            echo -e "  ${RED}✗ No installed JDK 17 or JDK 21 found on this machine!${NC}"
            echo ""
            echo -e "  ${BOLD}To fix this, install OpenJDK 17 on your Linux system:${NC}"
            echo "    Ubuntu/Debian:  sudo apt install openjdk-17-jdk"
            echo "    Fedora:         sudo dnf install java-17-openjdk-devel"
            echo "    Arch Linux:     sudo pacman -S jdk17-openjdk"
            echo ""
            echo "  Then re-run this script: ./build_and_install.sh"
            exit 1
        fi
    else
        local full_ver
        full_ver=$("$java_bin" -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
        echo -e "  ${GREEN}✓${NC} Java Runtime detected: ${BOLD}$full_ver${NC} (Java $java_major)"
    fi
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
    $GRADLE_CMD :app:assembleDebug --no-configuration-cache

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
    $GRADLE_CMD :app:assembleRelease --no-configuration-cache

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
        $GRADLE_CMD clean --no-configuration-cache
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
