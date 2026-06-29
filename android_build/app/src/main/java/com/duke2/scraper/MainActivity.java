package com.duke2.scraper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQUEST_STORAGE = 100;
    private EditText etUrl;
    private Spinner spinnerMediaType;
    private EditText etMaxItems;
    private EditText etMaxDepth;
    private EditText etMaxPages;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(0xFF0A0A0F);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🕷️ Duke2 Scraper");
        tvTitle.setTextSize(28);
        tvTitle.setTextColor(0xFFE94560);
        tvTitle.setPadding(0, 0, 0, 20);
        layout.addView(tvTitle);

        tvStatus = new TextView(this);
        tvStatus.setText("Status: Ready");
        tvStatus.setTextColor(0xFF888888);
        tvStatus.setPadding(0, 0, 0, 20);
        layout.addView(tvStatus);

        TextView tvUrl = new TextView(this);
        tvUrl.setText("Target URL");
        tvUrl.setTextColor(0xFFE0E0E0);
        layout.addView(tvUrl);

        etUrl = new EditText(this);
        etUrl.setHint("https://example.com");
        etUrl.setTextColor(0xFFE0E0E0);
        etUrl.setHintTextColor(0xFF888888);
        layout.addView(etUrl);

        TextView tvType = new TextView(this);
        tvType.setText("Media Type");
        tvType.setTextColor(0xFFE0E0E0);
        tvType.setPadding(0, 20, 0, 8);
        layout.addView(tvType);

        spinnerMediaType = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"images", "videos", "audio", "documents", "archives", "ebooks", "all"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMediaType.setAdapter(adapter);
        layout.addView(spinnerMediaType);

        TextView tvMaxItems = new TextView(this);
        tvMaxItems.setText("Max items per type (0 = unlimited)");
        tvMaxItems.setTextColor(0xFFE0E0E0);
        layout.addView(tvMaxItems);

        etMaxItems = new EditText(this);
        etMaxItems.setHint("0");
        etMaxItems.setTextColor(0xFFE0E0E0);
        etMaxItems.setHintTextColor(0xFF888888);
        layout.addView(etMaxItems);

        TextView tvDepth = new TextView(this);
        tvDepth.setText("Crawl depth");
        tvDepth.setTextColor(0xFFE0E0E0);
        layout.addView(tvDepth);

        etMaxDepth = new EditText(this);
        etMaxDepth.setHint("2");
        etMaxDepth.setTextColor(0xFFE0E0E0);
        etMaxDepth.setHintTextColor(0xFF888888);
        layout.addView(etMaxDepth);

        TextView tvPages = new TextView(this);
        tvPages.setText("Max pages to crawl");
        tvPages.setTextColor(0xFFE0E0E0);
        layout.addView(tvPages);

        etMaxPages = new EditText(this);
        etMaxPages.setHint("1");
        etMaxPages.setTextColor(0xFFE0E0E0);
        etMaxPages.setHintTextColor(0xFF888888);
        layout.addView(etMaxPages);

        Button btnStart = new Button(this);
        btnStart.setText("▶️ Start Download");
        btnStart.setBackgroundColor(0xFFE94560);
        btnStart.setTextColor(0xFFFFFFFF);
        btnStart.setPadding(20, 30, 20, 30);
        btnStart.setOnClickListener(v -> startScraper());
        layout.addView(btnStart);

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
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String mediaType = spinnerMediaType.getSelectedItem().toString();
        int maxItems = 0;
        int maxDepth = 2;
        int maxPages = 1;
        try { maxItems = Integer.parseInt(etMaxItems.getText().toString().trim()); } catch (Exception ignored) {}
        try { maxDepth = Integer.parseInt(etMaxDepth.getText().toString().trim()); } catch (Exception ignored) {}
        try { maxPages = Integer.parseInt(etMaxPages.getText().toString().trim()); } catch (Exception ignored) {}

        tvStatus.setText("Starting embedded scraper...");

        // Initialize Chaquopy Python runtime if needed
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        final String fUrl = url;
        final String fMedia = mediaType;
        final int fMaxItems = maxItems;
        final int fMaxDepth = maxDepth;
        final int fMaxPages = maxPages;

        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("duke_wrapper");
                PyObject result = module.callAttr("run_from_android", fUrl, fMedia, fMaxItems, fMaxDepth, fMaxPages);
                String out = result.toString();
                new Handler(Looper.getMainLooper()).post(() -> tvStatus.setText("Scraper finished: " + out));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void copyAsset(String assetName, File outFile) throws Exception {
        try (InputStream in = getAssets().open(assetName); OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
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
            .setMessage("Configure proxy in Duke2_Enhanced.py:\n\n" +
                "1. HTTP Proxy: http://host:port\n" +
                "2. SOCKS5: socks5://user:pass@host:port\n" +
                "3. Rotating proxy supported\n\n" +
                "Enter proxy URL when prompted during scraper startup.")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showBypassInfo() {
        new AlertDialog.Builder(this)
            .setTitle("🛡️ Cloudflare Bypass Engines")
            .setMessage("Available bypass engines:\n\n" +
                "1. curl_cffi - TLS fingerprint impersonation (recommended)\n" +
                "2. cloudscraper - JavaScript challenge solver\n" +
                "3. Standard requests (limited)\n\n" +
                "Install: pip install curl-cffi cloudscraper")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTutorial() {
        new AlertDialog.Builder(this)
            .setTitle("📖 Quick Tutorial")
            .setMessage("1. Install Termux from F-Droid\n" +
                "2. Run: pkg install python\n" +
                "3. Run: pip install requests bs4 curl-cffi cloudscraper\n" +
                "4. Run: python Duke2_Enhanced.py\n" +
                "5. Enter URL and configure options\n" +
                "6. View results in gallery.html\n\n" +
                "For full tutorial, see TUTORIAL.md")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showTermuxDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Termux Required")
            .setMessage("Duke2 Scraper requires Termux to run.\n\n" +
                "Install Termux from F-Droid, then:\n" +
                "1. pkg install python\n" +
                "2. pip install requests bs4 curl-cffi\n" +
                "3. python Duke2_Enhanced.py")
            .setPositiveButton("Get Termux", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://f-droid.org/packages/com.termux/"));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
