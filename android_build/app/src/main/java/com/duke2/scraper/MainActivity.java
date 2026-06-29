package com.duke2.scraper;

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

public class MainActivity extends Activity {
    
    private static final int REQUEST_STORAGE = 100;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    }
    
    private void addButton(LinearLayout layout, String text, android.view.View.OnClickListener listener) {
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
    }
    
    private void checkPermissions() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.INTERNET
            }, REQUEST_STORAGE);
        }
    }
    
    private void startScraper() {
        Toast.makeText(this, "Run in Termux: python Duke2_Enhanced.py", Toast.LENGTH_LONG).show();
        
        // Try to launch Termux
        try {
            Intent intent = new Intent();
            intent.setClassName("com.termux", "com.termux.app.TermuxActivity");
            startActivity(intent);
        } catch (Exception e) {
            showTermuxDialog();
        }
    }
    
    private void openGallery() {
        File galleryDir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        File galleryFile = new File(galleryDir, "gallery.html");
        
        if (galleryFile.exists()) {
            Uri uri = FileProvider.getUriForFile(this, "com.duke2.scraper.fileprovider", galleryFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/html");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No gallery found. Run scraper first!", Toast.LENGTH_LONG).show();
        }
    }
    
    private void openDownloadFolder() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Download/Duke2");
        if (!dir.exists()) dir.mkdirs();
        
        Uri uri = FileProvider.getUriForFile(this, "com.duke2.scraper.fileprovider", dir);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback - try file manager
            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            fileIntent.setDataAndType(Uri.parse(dir.getAbsolutePath()), "*/*");
            try {
                startActivity(fileIntent);
            } catch (Exception e2) {
                Toast.makeText(this, "Download folder: " + dir.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void showProxySettings() {
        new AlertDialog.Builder(this)
            .setTitle("⚙️ Proxy Settings")
            .setMessage("Configure proxy in Duke2_Enhanced.py:

"
                + "1. HTTP Proxy: http://host:port
"
                + "2. SOCKS5: socks5://user:pass@host:port
"
                + "3. Rotating proxy supported

"
                + "Enter proxy URL when prompted during scraper startup.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showBypassInfo() {
        new AlertDialog.Builder(this)
            .setTitle("🛡️ Cloudflare Bypass Engines")
            .setMessage("Available bypass engines:

"
                + "1. curl_cffi - TLS fingerprint impersonation (recommended)
"
                + "2. cloudscraper - JavaScript challenge solver
"
                + "3. Standard requests (limited)

"
                + "Install: pip install curl-cffi cloudscraper")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTutorial() {
        new AlertDialog.Builder(this)
            .setTitle("📖 Quick Tutorial")
            .setMessage("1. Install Termux from F-Droid
"
                + "2. Run: pkg install python
"
                + "3. Run: pip install requests bs4 curl-cffi cloudscraper
"
                + "4. Run: python Duke2_Enhanced.py
"
                + "5. Enter URL and configure options
"
                + "6. View results in gallery.html

"
                + "For full tutorial, see TUTORIAL.md")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTermuxDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Termux Required")
            .setMessage("Duke2 Scraper requires Termux to run.

"
                + "Install Termux from F-Droid, then:
"
                + "1. pkg install python
"
                + "2. pip install requests bs4 curl-cffi
"
                + "3. python Duke2_Enhanced.py")
            .setPositiveButton("Get Termux", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://f-droid.org/packages/com.termux/"));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
