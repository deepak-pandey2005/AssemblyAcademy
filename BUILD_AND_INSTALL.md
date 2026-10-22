# Assembly Academy — Android Build & Installation Guide

This guide provides step-by-step instructions to compile, build, and install **Assembly Academy** onto any physical Android device (smartphone, tablet, or foldable) or emulator.

---

## Quick Reference

| Attribute | Value |
| :--- | :--- |
| **Application ID** | `com.aistudio.assemblytutor.axpkqd` |
| **Main Activity** | `com.example.MainActivity` |
| **Minimum Android Version** | Android 7.0 (API 24 - Nougat) |
| **Target Android Version** | Android 15 (API 36) |
| **Build Artifact Output** | `app/build/outputs/apk/debug/app-debug.apk` |

---

## Prerequisites

Before building locally on your PC (macOS, Linux, or Windows):

1. **Java Development Kit (JDK 17+)**:
   * Verify: `java -version`
   * Download: [Eclipse Temurin JDK 17+](https://adoptium.net/) or use your package manager:
     * Ubuntu/Debian: `sudo apt install openjdk-17-jdk`
     * macOS: `brew install openjdk@17`
     * Windows: Download installer from adoptium.net
2. **Android SDK & Platform-Tools (`adb`)**:
   * Verify: `adb --version`
   * Download standalone platform-tools: [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools)
   * Or install **Android Studio Iguana / Jellyfish / Ladybug+**: [developer.android.com/studio](https://developer.android.com/studio)

---

## Method 1: Automated Script (Recommended)

An all-in-one build and installation wizard is provided in the project root:

```bash
# Make executable (Linux/macOS)
chmod +x build_and_install.sh

# 1. Build and install directly to connected phone
./build_and_install.sh --install

# 2. Build, install, and immediately launch the app
./build_and_install.sh --launch

# 3. Interactive Menu (prompts for options)
./build_and_install.sh
```

### CLI Command Options:
* `./build_and_install.sh --build` — Compiles the debug APK only.
* `./build_and_install.sh --install` — Builds and installs APK over USB/Wi-Fi ADB.
* `./build_and_install.sh --launch` — Installs and starts the app on screen.
* `./build_and_install.sh --devices` — Lists connected Android devices.
* `./build_and_install.sh --wireless <IP:PORT>` — Connects to a device via Wi-Fi ADB.
* `./build_and_install.sh --logcat` — Live streams app debug logs.

---

## Method 2: Manual Terminal Commands (Gradle + ADB)

### Step 1: Build the Debug APK

Run Gradle from the workspace root:

```bash
# Using Gradle wrapper (if available):
./gradlew :app:assembleDebug

# Or using system Gradle:
gradle :app:assembleDebug
```

Upon completion, your APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Connect Phone & Enable USB Debugging

1. Connect your Android device to your computer via USB cable.
2. Enable **Developer Options**:
   * Open phone **Settings** > **About phone**.
   * Tap **Build number** 7 times until you see *"You are now a developer!"*.
3. Enable **USB Debugging**:
   * Go to **Settings** > **System** (or **Additional settings**) > **Developer options**.
   * Turn on **USB debugging**.
   * *(Xiaomi / HyperOS / MIUI users)*: Also turn on **Install via USB**.
4. Check connection on your PC:
   ```bash
   adb devices
   ```
   *If prompted on your phone screen, tap **"Always allow from this computer"**.*

### Step 3: Install APK onto Phone

```bash
# Install with replacement of existing version (-r) and grant permissions (-g):
adb install -r -d -g app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Launch the App

```bash
adb shell am start -n com.aistudio.assemblytutor.axpkqd/com.example.MainActivity
```

---

## Method 3: Wireless ADB Installation (No Cable)

For Android 11+:

1. Connect your PC and Android phone to the **same Wi-Fi network**.
2. On your phone: **Settings** > **Developer options** > **Wireless debugging** (turn on).
3. Tap **"Pair device with pairing code"**. Note the IP address, Port, and 6-digit Code.
4. On your PC:
   ```bash
   # Pair device:
   adb pair <IP>:<PORT>
   # (Enter pairing code when prompted)

   # Connect device:
   adb connect <IP>:<PORT>

   # Verify:
   adb devices

   # Install:
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Method 4: Import into Android Studio (1-Click Run)

1. Launch **Android Studio**.
2. Select **Open** and choose the project root folder.
3. Wait for Gradle sync to complete.
4. Select your connected Android phone in the top device dropdown.
5. Click the green **Run (▶)** button (or press `Shift + F10`).
   * Android Studio will build, deploy, and launch Assembly Academy automatically.

---

## Method 5: Exporting & Direct Sideloading from AI Studio

If you are using Google AI Studio in the browser:

1. Tap the project **Settings** icon in the upper right.
2. Select **Export Project as ZIP** or **Generate APK**.
3. Transfer the resulting `.apk` file to your phone via:
   * Google Drive / Dropbox
   * USB file transfer (copy to `Downloads` folder)
   * Local Bluetooth / Quick Share
4. On your phone, tap the `.apk` file in **Files** or **Downloads**.
5. If prompted, toggle **"Allow from this source"** in security settings, then tap **Install**.

---

## Troubleshooting & Common Solutions

### 1. `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
* **Cause**: A previous version of the app with a different debug signature is already installed.
* **Fix**: Uninstall the app from your phone first:
  ```bash
  adb uninstall com.aistudio.assemblytutor.axpkqd
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### 2. `error: device unauthorized`
* **Cause**: Your phone has not accepted the computer's RSA debugging key.
* **Fix**:
  1. Unplug and reconnect the USB cable.
  2. Unlock your phone screen.
  3. Look for the *"Allow USB debugging?"* dialog.
  4. Check *"Always allow"* and tap **OK**.

### 3. Xiaomi / HyperOS / MIUI Error: `INSTALL_FAILED_USER_RESTRICTED`
* **Cause**: MIUI blocks ADB installation by default.
* **Fix**:
  1. Insert a SIM card (required by MIUI to toggle security setting).
  2. In **Developer Options**, enable **Install via USB**.
  3. Turn on **USB debugging (Security settings)**.

### 4. Google Play Protect Warning ("Blocked by Play Protect")
* **Cause**: The app is compiled in debug mode with a development certificate.
* **Fix**: Tap **"More details"** on the phone prompt, then tap **"Install anyway"**.
