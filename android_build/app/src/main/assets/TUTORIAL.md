# 🕷️ Duke2 Enhanced Scraper - User Tutorial

## Table of Contents
1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Cloudflare Bypass Engines](#cloudflare-bypass-engines)
4. [Gallery Viewer](#gallery-viewer)
5. [External Viewer](#external-viewer)
6. [Proxy Support](#proxy-support)
7. [Advanced Features](#advanced-features)
8. [Android/Termux Setup](#androidtermux-setup)
9. [Troubleshooting](#troubleshooting)
10. [Safety & Legal](#safety--legal)

---

## Introduction

Duke2 Enhanced is an advanced media scraper with built-in Cloudflare bypass, interactive gallery viewer, and external viewer support. It's designed to work on Android (via Termux), Linux, Windows, and macOS.

### What's New in v3.0
- **Cloudflare Bypass** - Three engines to bypass anti-bot protection
- **Gallery Viewer** - Interactive HTML gallery of downloaded content
- **External Viewer** - Open files in system default apps
- **Proxy Support** - HTTP/SOCKS5 proxy with rotation
- **Enhanced Extraction** - Lazy-loaded media, locked content patterns
- **Session Persistence** - Cookie saving for authenticated sites
- **Smart Retry** - Exponential backoff with automatic fallback

---

## Installation

### Prerequisites
- Python 3.8 or higher
- pip (Python package manager)

### Quick Install

```bash
# Install Python dependencies
pip install requests beautifulsoup4 curl-cffi cloudscraper Pillow

# Optional but recommended
pip install primp  # Alternative TLS impersonation library
```

### Platform-Specific Setup

#### Android (Termux)
```bash
# Install Termux from F-Droid (NOT Google Play)
# Then run:
pkg update
pkg install python -y
pip install requests beautifulsoup4 curl-cffi cloudscraper Pillow
termux-setup-storage  # Grant storage permission
```

#### Linux
```bash
sudo apt-get install python3 python3-pip
pip3 install requests beautifulsoup4 curl-cffi cloudscraper Pillow
```

#### Windows
```cmd
python -m pip install requests beautifulsoup4 curl-cffi cloudscraper Pillow
```

#### macOS
```bash
brew install python3
pip3 install requests beautifulsoup4 curl-cffi cloudscraper Pillow
```

---

## Cloudflare Bypass Engines

The scraper includes **three bypass engines** that automatically handle Cloudflare and other anti-bot protections:

### Engine 1: curl_cffi (Recommended - Fastest)
Uses **TLS fingerprint impersonation** to mimic a real Chrome browser at the network level.

**How it works:**
- Impersonates Chrome's TLS handshake (JA3/JA4 fingerprint)
- Uses HTTP/2 like a real browser
- Automatically rotates headers
- No browser overhead - pure HTTP speed

**When to use:** Best for most sites with TLS-level protection

**Install:** `pip install curl-cffi`

### Engine 2: cloudscraper (JavaScript Challenge Solver)
Handles Cloudflare's JavaScript challenges by solving them automatically.

**How it works:**
- Built on requests library
- Solves Cloudflare IUAM (I'm Under Attack Mode) challenges
- Handles JavaScript fingerprinting tests
- Good for moderate protection levels

**When to use:** Sites with JS challenges that curl_cffi can't bypass

**Install:** `pip install cloudscraper`

### Engine 3: Standard requests (Fallback)
Basic HTTP requests without bypass. Works on unprotected sites.

**When to use:** Simple sites without any anti-bot protection

### Engine Selection
When you start the scraper, you'll see:
```
Cloudflare bypass engine:
1. Auto (best available)
2. curl_cffi (TLS impersonation - fastest)
3. cloudscraper (JS challenge solver)
4. Standard requests (no bypass)
```

**Recommendation:** Use option 1 (Auto) to let the scraper pick the best engine.

---

## Gallery Viewer

After downloading, an interactive HTML gallery is automatically generated.

### Opening the Gallery

**Option 1 - Auto-open after scrape:**
```
When scrape completes, type 'y' when asked:
"Open gallery now? (y/n): y"
```

**Option 2 - Manual open:**
```bash
# Android (Termux)
termux-open /sdcard/Download/Duke2/gallery.html

# Linux
xdg-open ~/Downloads/Duke2/gallery.html

# macOS
open ~/Downloads/Duke2/gallery.html

# Windows
start %USERPROFILE%\Downloads\Duke2\gallery.html
```

### Gallery Features
- **Tab Navigation** - Switch between media types (Images, Videos, Audio, etc.)
- **Search** - Filter files by name
- **Sort** - Sort by name, size, or type
- **Lazy Loading** - Images load as you scroll
- **Fullscreen** - Click any image to view fullscreen
- **Download Button** - Re-download individual files
- **Responsive** - Works on mobile and desktop

### Gallery Location
```
Android:  /sdcard/Download/Duke2/gallery.html
Linux:    ~/Downloads/Duke2/gallery.html
Windows:  %USERPROFILE%\Downloads\Duke2\gallery.html
macOS:    ~/Downloads/Duke2/gallery.html
```

---

## External Viewer

The external viewer option opens downloaded files immediately in your system's default app.

### How to Enable

When starting the scraper, select:
```
External viewer:
1. No - download only
2. Yes - open each file after download
Select: 2
```

### Platform Behavior

**Android (Termux):**
- Uses `termux-open` to launch system apps
- Images → Gallery/Photos app
- Videos → Video player
- Audio → Music player
- Documents → Document viewer

**Linux:**
- Uses `xdg-open` for default applications

**macOS:**
- Uses `open` command

**Windows:**
- Uses `os.startfile` for default programs

### Use Cases
- Preview images as they download
- Listen to audio files immediately
- Watch videos without waiting for all downloads

---

## Proxy Support

Configure proxies to avoid IP bans and access geo-restricted content.

### Proxy Types Supported

**HTTP Proxy:**
```
Enter proxy URL: http://proxy.example.com:8080
```

**Authenticated HTTP Proxy:**
```
Enter proxy URL: http://username:password@proxy.example.com:8080
```

**SOCKS5 Proxy:**
```
Enter proxy URL: socks5://proxy.example.com:1080
```

**Rotating Proxy:**
```
Enter proxy URL: http://user:pass@rotating.proxy.provider.com:8080
```

### When to Use Proxies
- Scraping at high volume (avoid rate limits)
- Accessing geo-restricted content
- Avoiding IP bans on heavily protected sites
- Distributed scraping across multiple IPs

### Free Proxy Sources
- `proxy-list.download`
- `free-proxy-list.net`
- `geonode.com/free-proxy-list`

---

## Advanced Features

### Session Cookie Persistence

Cookies are automatically saved after each scrape:
```
Location: ~/.duke2_cookies.json
```

This means:
- Login sessions persist between runs
- Less likely to trigger re-authentication
- Better success on sites requiring login

### Smart Retry Logic

The scraper automatically retries failed requests:
- **Exponential backoff** - waits longer between each retry
- **Status code handling** - different strategies for 403, 429, 500+
- **Engine fallback** - tries alternative HTTP engines on failure
- **Header rotation** - rotates User-Agent and headers on 403 errors

### Enhanced Media Extraction

The scraper detects media from:
- Standard `src` attributes
- Lazy-load attributes (`data-src`, `data-lazy-src`, `data-original`)
- `srcset` responsive images
- CSS background images
- Meta tags (Open Graph, Twitter Cards)
- Video embeds (YouTube, Vimeo)
- "Locked" content patterns (premium/behind-paywall content)

### Media Type Configuration

When selecting media types:
```
1. Images    - .jpg, .png, .gif, .webp, .svg, .bmp, .avif
2. Videos    - .mp4, .webm, .mkv, .mov, .avi, .m4v
3. Audio     - .mp3, .ogg, .wav, .flac, .aac, .m4a
4. Documents - .pdf, .epub, .docx, .txt, .rtf
5. Archives  - .zip, .rar, .7z, .tar, .gz
6. eBooks    - .mobi, .azw3, .cbz, .cbr
7. All       - Everything above
```

---

## Android/Termux Setup

### Step-by-Step Android Installation

**Step 1: Install Termux**
```
1. Open F-Droid app (or visit f-droid.org)
2. Search for "Termux"
3. Install Termux and Termux:API
```
> ⚠️ **Important:** Use F-Droid version, NOT Google Play version

**Step 2: Setup Storage**
```bash
termux-setup-storage
# Tap "Allow" when prompted
```

**Step 3: Install Dependencies**
```bash
pkg update
pkg install python -y
pip install requests beautifulsoup4 curl-cffi cloudscraper Pillow
```

**Step 4: Download Scraper**
```bash
cd ~/storage/shared/Download
# Copy Duke2_Enhanced.py to this folder
```

**Step 5: Run**
```bash
python Duke2_Enhanced.py
```

### Quick Launcher (Optional)

Create a quick launcher command:
```bash
cat > $PREFIX/bin/duke2 << 'EOF'
#!/bin/bash
python /sdcard/Download/Duke2_Enhanced.py "$@"
EOF
chmod +x $PREFIX/bin/duke2

# Now just type:
duke2
```

### Android External Viewer Setup

The external viewer works automatically on Android:
- Uses `termux-open` command
- Opens files in your default Android apps
- Works with Gallery, VLC, MX Player, etc.

To set default apps:
1. When a file opens, Android asks which app to use
2. Select your preferred app
3. Tap "Always" to set as default

---

## Troubleshooting

### curl_cffi Not Found
```bash
# Install it
pip install curl-cffi

# If installation fails, try:
pip install --upgrade pip
pip install curl-cffi --no-cache-dir

# Or use alternative:
pip install primp
```

### Permission Denied (Android)
```bash
# Run this in Termux:
termux-setup-storage

# If still failing:
termux-reset  # Warning: resets Termux
```

### 403 Forbidden Errors
1. **Enable proxy** - Use rotating residential proxies
2. **Increase delays** - Sites may need more time between requests
3. **Reduce threads** - Lower MAX_THREADS in the script
4. **Try cloudscraper** - Some sites need JS challenge solving

### Rate Limited (429 Errors)
1. **Add delays** - The scraper has built-in exponential backoff
2. **Use proxies** - Rotate IPs to avoid rate limits
3. **Reduce crawl depth** - Less aggressive crawling
4. **Lower max_pages** - Crawl fewer pages

### Gallery Not Opening (Android)
```bash
# Install a file manager app first
pkg install termux-api

# Or view gallery via:
termux-open /sdcard/Download/Duke2/gallery.html
```

### Termux-Open Not Found
```bash
pkg install termux-api
# Also install Termux:API app from F-Droid
```

### Cloudscraper Errors
```bash
# Update cloudscraper
pip install --upgrade cloudscraper

# If still failing, the site may have advanced protection
# Try using proxies or manual cookie extraction
```

---

## Safety & Legal

### Responsible Scraping Guidelines

1. **Check robots.txt** - Always check `/robots.txt` before scraping
2. **Respect rate limits** - Don't overwhelm servers
3. **Don't scrape private content** - Only scrape publicly available data
4. **Follow terms of service** - Respect website ToS
5. **Don't redistribute** - Don't share scraped content without permission

### Legal Considerations

- Scraping publicly available data is generally legal
- Bypassing Cloudflare may violate website ToS
- Don't scrape personal data (GDPR/CCPA compliance)
- Don't scrape copyrighted content for redistribution
- Be aware of CFAA (Computer Fraud and Abuse Act) in the US

### Safety Tips

- Use proxies to protect your IP
- Don't scrape logged-in/personal account areas
- Keep downloaded content for personal use only
- Be respectful of server resources

---

## Quick Reference

### Command Summary
```bash
# Basic usage
python Duke2_Enhanced.py

# With proxy
# (Enter proxy when prompted)

# All media types, no limits
# Select option 7 for media types
# Enter 0 for all limits

# Conservative settings for protected sites
# Depth: 1, Max pages: 20, Same domain: yes
# Enable proxy
```

### File Locations
```
Downloads:     /sdcard/Download/Duke2/    (Android)
               ~/Downloads/Duke2/         (Linux/macOS)
               %USERPROFILE%\Downloads\Duke2\  (Windows)

Gallery:       .../Duke2/gallery.html
ZIP Archive:   .../Duke2/media.zip
JSON Results:  .../Duke2/results.json
CSV Results:   .../Duke2/results.csv
Cookies:       ~/.duke2_cookies.json
```

### Keyboard Shortcuts (During Operation)
```
Ctrl+C    - Stop scraping (graceful shutdown)
```

---

## Tips & Tricks

### For Heavily Protected Sites
1. Use curl_cffi engine
2. Enable HTTP proxy
3. Set max_depth to 1
4. Set max_pages to 10-20
5. Enable same_domain
6. Increase min_size_kb to filter small files

### For Maximum Downloads
1. Use "All" media types
2. Set max_depth to 3+
3. Set max_pages to 500+
4. Disable same_domain (with caution)
5. Set all limits to 0

### For Quick Previews
1. Enable external viewer
2. Set max limits low (5-10 per type)
3. Set max_depth to 1
4. Set max_pages to 10

### For Gallery-Only Sites
1. Select only "Images" media type
2. Set min_size_kb to 50 (filter thumbnails)
3. Enable same_domain

---

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Verify all dependencies are installed
3. Try different bypass engines
4. Enable proxy for protected sites

---

**Happy Scraping! 🕷️**
