#!/usr/bin/env python3
"""
Duke2 Scraper - Android APK Builder
Builds a Termux-compatible APK package with spider icon
Usage: python build_apk.py

Requirements:
    - Python 3.8+
    - Pillow (for icon processing)
    - buildozer (for APK compilation) - optional for full APK
"""

import os
import sys
import shutil
import subprocess
from pathlib import Path

# Configuration
APP_NAME = "Duke2Scraper"
APP_PACKAGE = "com.duke2.scraper"
APP_VERSION = "3.0.0"
APP_ICON = "spider_icon.png"

BUILD_DIR = "android_build"

def create_android_structure():
    """Create Android app structure"""
    print("[*] Creating Android build structure...")
    
    # Clean previous build
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    
    os.makedirs(BUILD_DIR, exist_ok=True)
    
    # Create directories
    dirs = [
        f"{BUILD_DIR}/app/src/main/assets",
        f"{BUILD_DIR}/app/src/main/res/mipmap-hdpi",
        f"{BUILD_DIR}/app/src/main/res/mipmap-mdpi",
        f"{BUILD_DIR}/app/src/main/res/mipmap-xhdpi",
        f"{BUILD_DIR}/app/src/main/res/mipmap-xxhdpi",
        f"{BUILD_DIR}/app/src/main/res/mipmap-xxxhdpi",
        f"{BUILD_DIR}/app/src/main/res/values",
        f"{BUILD_DIR}/app/src/main/res/xml",
        f"{BUILD_DIR}/app/src/main/java/com/duke2/scraper",
    ]
    for d in dirs:
        os.makedirs(d, exist_ok=True)
    
    print("[+] Android structure created")


def generate_icons():
    """Generate Android icon sizes from spider_icon.png"""
    print("[*] Generating Android icon sizes...")
    
    try:
        from PIL import Image
    except ImportError:
        print("[!] Pillow not installed. Installing...")
        subprocess.run([sys.executable, "-m", "pip", "install", "Pillow"], check=True)
        from PIL import Image
    
    if not os.path.exists(APP_ICON):
        print(f"[!] Icon file {APP_ICON} not found!")
        return False
    
    # Android icon sizes
    icon_sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    
    img = Image.open(APP_ICON).convert("RGBA")
    
    for folder, size in icon_sizes.items():
        resized = img.resize((size, size), Image.LANCZOS)
        # Create rounded corners (adaptive icon style)
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        radius = size // 5  # Corner radius
        draw.rounded_rectangle([0, 0, size, size], radius=radius, fill=255)
        
        rounded = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        rounded.paste(resized, (0, 0), mask)
        
        output_path = f"{BUILD_DIR}/app/src/main/res/{folder}/ic_launcher.png"
        rounded.save(output_path, "PNG")
        print(f"    [+] {folder}: {size}x{size}")
    
    print("[+] Icons generated")
    return True


def create_manifest():
    """Create AndroidManifest.xml"""
    print("[*] Creating AndroidManifest.xml...")
    
    manifest = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="{APP_PACKAGE}"
    android:versionCode="30"
    android:versionName="{APP_VERSION}">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="{APP_NAME}"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="{APP_PACKAGE}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>"""
    
    with open(f"{BUILD_DIR}/app/src/main/AndroidManifest.xml", 'w') as f:
        f.write(manifest)
    
    print("[+] AndroidManifest.xml created")


def create_resources():
    """Create resource files"""
    print("[*] Creating resource files...")
    
    # Colors
    colors = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#E94560</color>
    <color name="primary_dark">#0F3460</color>
    <color name="accent">#533483</color>
    <color name="background_dark">#0A0A0F</color>
    <color name="text_primary">#E0E0E0</color>
    <color name="text_secondary">#888888</color>
</resources>"""
    
    with open(f"{BUILD_DIR}/app/src/main/res/values/colors.xml", 'w') as f:
        f.write(colors)
    
    # Strings
    strings = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{APP_NAME}</string>
    <string name="app_description">Advanced Media Scraper with Cloudflare Bypass</string>
    <string name="start_scraper">Start Scraper</string>
    <string name="open_gallery">Open Gallery</string>
    <string name="settings">Settings</string>
    <string name="proxy_settings">Proxy Settings</string>
    <string name="cloudflare_bypass">Cloudflare Bypass</string>
    <string name="external_viewer">External Viewer</string>
    <string name="tutorial">Tutorial</string>
</resources>"""
    
    with open(f"{BUILD_DIR}/app/src/main/res/values/strings.xml", 'w') as f:
        f.write(strings)
    
    # Styles
    styles = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryDark">@color/primary_dark</item>
        <item name="colorAccent">@color/accent</item>
        <item name="android:windowBackground">@color/background_dark</item>
    </style>
</resources>"""
    
    with open(f"{BUILD_DIR}/app/src/main/res/values/styles.xml", 'w') as f:
        f.write(styles)
    
    # File paths for FileProvider
    file_paths = """<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="downloads" path="Download/Duke2/" />
    <cache-path name="cache" path="." />
    <files-path name="files" path="." />
</paths>"""
    
    with open(f"{BUILD_DIR}/app/src/main/res/xml/file_paths.xml", 'w') as f:
        f.write(file_paths)
    
    print("[+] Resource files created")


def create_main_activity():
    """Create MainActivity.java"""
    print("[*] Creating MainActivity.java...")
    
    java_code = f"""package {APP_PACKAGE};

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;

public class MainActivity extends Activity {{
    
    private static final int REQUEST_STORAGE = 100;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);
        
        // Main layout
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFF0A0A0F);
        
        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🕷️ Duke2 Scraper");
        tvTitle.setTextSize(28);
        tvTitle.setTextColor(0xFFE94560);
        tvTitle.setPadding(0, 0, 0, 20);
        layout.addView(tvTitle);
        
        // Status
        tvStatus = new TextView(this);
        tvStatus.setText("Status: Ready");
        tvStatus.setTextColor(0xFF888888);
        tvStatus.setPadding(0, 0, 0, 30);
        layout.addView(tvStatus);
        
        // Buttons
        addButton(layout, "▶️ Start Scraper", v -> startScraper());
        addButton(layout, "🖼️ Open Gallery", v -> openGallery());
        addButton(layout, "⚙️ Proxy Settings", v -> showProxySettings());
        addButton(layout, "🛡️ Cloudflare Bypass", v -> showBypassInfo());
        addButton(layout, "📖 Tutorial", v -> showTutorial());
        addButton(layout, "📁 Open Download Folder", v -> openDownloadFolder());
        
        scrollView.addView(layout);
        setContentView(scrollView);
        
        checkPermissions();
    }}
    
    private void addButton(LinearLayout layout, String text, android.view.View.OnClickListener listener) {{
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(0xFFE94560);
        btn.setTextColor(0xFFFFFFFF);
        btn.setPadding(20, 30, 20, 30);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        btn.setLayoutParams(params);
        btn.setOnClickListener(listener);
        layout.addView(btn);
    }}
    
    private void checkPermissions() {{
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {{
            requestPermissions(new String[]{{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.INTERNET
            }}, REQUEST_STORAGE);
        }}
    }}
    
    private void startScraper() {{
        Toast.makeText(this, "Run in Termux: python Duke2_Enhanced.py", Toast.LENGTH_LONG).show();
        
        // Try to launch Termux
        try {{
            Intent intent = new Intent();
            intent.setClassName("com.termux", "com.termux.app.TermuxActivity");
            startActivity(intent);
        }} catch (Exception e) {{
            showTermuxDialog();
        }}
    }}
    
    private void openGallery() {{
        File galleryDir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        File galleryFile = new File(galleryDir, "gallery.html");
        
        if (galleryFile.exists()) {{
            Uri uri = FileProvider.getUriForFile(this, "{APP_PACKAGE}.fileprovider", galleryFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/html");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }} else {{
            Toast.makeText(this, "No gallery found. Run scraper first!", Toast.LENGTH_LONG).show();
        }}
    }}
    
    private void openDownloadFolder() {{
        File dir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        if (!dir.exists()) dir.mkdirs();
        
        Uri uri = FileProvider.getUriForFile(this, "{APP_PACKAGE}.fileprovider", dir);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {{
            startActivity(intent);
        }} catch (Exception e) {{
            // Fallback - try file manager
            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            fileIntent.setDataAndType(Uri.parse(dir.getAbsolutePath()), "*/*");
            try {{
                startActivity(fileIntent);
            }} catch (Exception e2) {{
                Toast.makeText(this, "Download folder: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }}
        }}
    }}
    
    private void showProxySettings() {{
        new AlertDialog.Builder(this)
            .setTitle("⚙️ Proxy Settings")
            .setMessage("Configure proxy in Duke2_Enhanced.py:\n\n"
                + "1. HTTP Proxy: http://host:port\n"
                + "2. SOCKS5: socks5://user:pass@host:port\n"
                + "3. Rotating proxy supported\n\n"
                + "Enter proxy URL when prompted during scraper startup.")
            .setPositiveButton("OK", null)
            .show();
    }}
    
    private void showBypassInfo() {{
        new AlertDialog.Builder(this)
            .setTitle("🛡️ Cloudflare Bypass Engines")
            .setMessage("Available bypass engines:\n\n"
                + "1. curl_cffi - TLS fingerprint impersonation (recommended)\n"
                + "2. cloudscraper - JavaScript challenge solver\n"
                + "3. Standard requests (limited)\n\n"
                + "Install: pip install curl-cffi cloudscraper")
            .setPositiveButton("OK", null)
            .show();
    }}
    
    private void showTutorial() {{
        new AlertDialog.Builder(this)
            .setTitle("📖 Quick Tutorial")
            .setMessage("1. Install Termux from F-Droid\n"
                + "2. Run: pkg install python\n"
                + "3. Run: pip install requests bs4 curl-cffi cloudscraper\n"
                + "4. Run: python Duke2_Enhanced.py\n"
                + "5. Enter URL and configure options\n"
                + "6. View results in gallery.html\n\n"
                + "For full tutorial, see TUTORIAL.md")
            .setPositiveButton("OK", null)
            .show();
    }}
    
    private void showTermuxDialog() {{
        new AlertDialog.Builder(this)
            .setTitle("Termux Required")
            .setMessage("Duke2 Scraper requires Termux to run.\n\n"
                + "Install Termux from F-Droid, then:\n"
                + "1. pkg install python\n"
                + "2. pip install requests bs4 curl-cffi\n"
                + "3. python Duke2_Enhanced.py")
            .setPositiveButton("Get Termux", (d, w) -> {{
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://f-droid.org/packages/com.termux/"));
                startActivity(intent);
            }})
            .setNegativeButton("Cancel", null)
            .show();
    }}
}}
"""
    
    with open(f"{BUILD_DIR}/app/src/main/java/com/duke2/scraper/MainActivity.java", 'w') as f:
        f.write(java_code)
    
    print("[+] MainActivity.java created")


def create_gradle_files():
    """Create Gradle build files"""
    print("[*] Creating Gradle build files...")
    
    # build.gradle (project)
    project_gradle = """// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.0'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
"""
    
    with open(f"{BUILD_DIR}/build.gradle", 'w') as f:
        f.write(project_gradle)
    
    # build.gradle (app)
    app_gradle = f"""plugins {{
    id 'com.android.application'
}}

android {{
    namespace '{APP_PACKAGE}'
    compileSdk 34

    defaultConfig {{
        applicationId "{APP_PACKAGE}"
        minSdk 24
        targetSdk 34
        versionCode 30
        versionName "{APP_VERSION}"
    }}

    buildTypes {{
        release {{
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }}
    }}
    compileOptions {{
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }}
}}

dependencies {{
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core:1.12.0'
}}
"""
    
    with open(f"{BUILD_DIR}/app/build.gradle", 'w') as f:
        f.write(app_gradle)
    
    # settings.gradle
    settings = """include ':app'
"""
    
    with open(f"{BUILD_DIR}/settings.gradle", 'w') as f:
        f.write(settings)
    
    # gradle.properties
    props = """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
"""
    
    with open(f"{BUILD_DIR}/gradle.properties", 'w') as f:
        f.write(props)
    
    print("[+] Gradle files created")


def copy_scraper_script():
    """Copy the scraper script to assets"""
    print("[*] Copying scraper script...")
    
    if os.path.exists("Duke2_Enhanced.py"):
        shutil.copy("Duke2_Enhanced.py", f"{BUILD_DIR}/app/src/main/assets/Duke2_Enhanced.py")
        print("[+] Scraper script copied")
    else:
        print("[!] Duke2_Enhanced.py not found in current directory")
    
    # Copy tutorial
    if os.path.exists("TUTORIAL.md"):
        shutil.copy("TUTORIAL.md", f"{BUILD_DIR}/app/src/main/assets/TUTORIAL.md")
        print("[+] Tutorial copied")


def create_install_script():
    """Create Termux install script"""
    print("[*] Creating Termux install script...")
    
    script = """#!/data/data/com.termux/files/usr/bin/bash
# Duke2 Scraper - Termux Quick Installer

echo "============================================"
echo "  🕷️ Duke2 Scraper - Termux Installer"
echo "============================================"

# Update packages
echo "[*] Updating packages..."
pkg update -y

# Install Python
echo "[*] Installing Python..."
pkg install python -y

# Install dependencies
echo "[*] Installing Python dependencies..."
pip install requests beautifulsoup4 curl-cffi cloudscraper Pillow

# Create directory
mkdir -p /sdcard/Download/Duke2

# Copy script if in same directory
if [ -f "Duke2_Enhanced.py" ]; then
    cp Duke2_Enhanced.py $HOME/Duke2_Enhanced.py
    chmod +x $HOME/Duke2_Enhanced.py
    echo "[+] Script installed to $HOME/Duke2_Enhanced.py"
fi

# Create launcher
cat > $HOME/duke2 << 'EOF'
#!/data/data/com.termux/files/usr/bin/bash
cd $HOME
python Duke2_Enhanced.py "$@"
EOF
chmod +x $HOME/duke2

echo ""
echo "============================================"
echo "  ✅ Installation Complete!"
echo "============================================"
echo ""
echo "Usage:"
echo "  duke2          - Run the scraper"
echo "  duke2 --help   - Show help"
echo ""
echo "Or run directly:"
echo "  python Duke2_Enhanced.py"
echo ""
echo "Gallery will open at:"
echo "  /sdcard/Download/Duke2/gallery.html"
echo ""
"""
    
    with open(f"{BUILD_DIR}/install_termux.sh", 'w') as f:
        f.write(script)
    
    os.chmod(f"{BUILD_DIR}/install_termux.sh", 0o755)
    print("[+] install_termux.sh created")


def build_with_buildozer():
    """Alternative: Build with buildozer for simple APK"""
    print("[*] Creating buildozer.spec...")
    
    spec = f"""[app]
title = {APP_NAME}
package.name = duke2scraper
package.domain = com.duke2
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,md
version = {APP_VERSION}
requirements = python3,requests,beautifulsoup4,curl-cffi,cloudscraper,Pillow
orientation = portrait
fullscreen = 0
android.permissions = INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE
android.api = 34
android.minapi = 24
android.sdk = 34
android.ndk = 25b
android.arch = arm64-v8a
icon.filename = {APP_ICON}
"""
    
    with open(f"{BUILD_DIR}/buildozer.spec", 'w') as f:
        f.write(spec)
    
    print("[+] buildozer.spec created")
    print("[!] To build with buildozer:")
    print("    cd android_build && buildozer android debug")


def main():
    """Main build process"""
    print("=" * 60)
    print("  🕷️ Duke2 Scraper - Android APK Builder")
    print("=" * 60)
    print()
    
    create_android_structure()
    if not generate_icons():
        print("[!] Using default Android icon")
    create_manifest()
    create_resources()
    create_main_activity()
    create_gradle_files()
    copy_scraper_script()
    create_install_script()
    build_with_buildozer()
    
    print()
    print("=" * 60)
    print("  ✅ Android build structure ready!")
    print("=" * 60)
    print()
    print("Build options:")
    print("  1. Quick install (Termux):  bash android_build/install_termux.sh")
    print("  2. Build APK (Gradle):      cd android_build && ./gradlew assembleDebug")
    print("  3. Build APK (Buildozer):   cd android_build && buildozer android debug")
    print()
    print("Note: Full APK build requires Android SDK/NDK or Buildozer")
    print("      For Termux users, use option 1 for Python script install")


if __name__ == "__main__":
    main()
