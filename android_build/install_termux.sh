#!/data/data/com.termux/files/usr/bin/bash
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
