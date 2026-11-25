#!/usr/bin/env python3
"""
Server lokal sederhana untuk mengunduh APK HP Monitor langsung ke perangkat Android
dalam satu jaringan WiFi lokal yang sama.
"""

import http.server
import os
import socket
import socketserver
import sys

PORT = 8080
DIST_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dist")


def get_lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


class ApkServerHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST_DIR, **kwargs)

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()

            files = []
            if os.path.exists(DIST_DIR):
                for f in sorted(os.listdir(DIST_DIR)):
                    if f.endswith(".apk"):
                        size_mb = os.path.getsize(os.path.join(DIST_DIR, f)) / (1024 * 1024)
                        files.append((f, f"{size_mb:.2f} MB"))

            lan_ip = get_lan_ip()
            html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>HP Monitor — Unduh APK Lokal</title>
    <style>
        body {{
            background: #121212;
            color: #E0E0E0;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            margin: 0;
            padding: 24px 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }}
        .card {{
            background: #1E1E1E;
            border: 1px solid #333;
            border-radius: 12px;
            padding: 20px;
            max-width: 440px;
            width: 100%;
            box-sizing: border-box;
        }}
        h2 {{
            margin-top: 0;
            color: #FFFFFF;
            font-size: 20px;
        }}
        .badge {{
            display: inline-block;
            background: #2563EB;
            color: white;
            font-size: 11px;
            font-weight: bold;
            padding: 2px 8px;
            border-radius: 4px;
            margin-bottom: 12px;
        }}
        .target {{
            color: #10B981;
            font-weight: bold;
        }}
        .btn {{
            display: block;
            background: #2563EB;
            color: white;
            text-decoration: none;
            text-align: center;
            padding: 14px 16px;
            border-radius: 8px;
            font-size: 16px;
            font-weight: bold;
            margin-top: 10px;
        }}
        .btn-secondary {{
            background: #2A2A2A;
            border: 1px solid #444;
            color: #CCCCCC;
        }}
        .info {{
            font-size: 13px;
            color: #888;
            margin-top: 16px;
            line-height: 1.5;
        }}
    </style>
</head>
<body>
    <div class="card">
        <span class="badge">LAN Server • {lan_ip}</span>
        <h2>Unduh HP Monitor APK</h2>
        <p style="font-size: 14px; color: #AAA;">Pilih versi yang sesuai dengan perangkat Android:</p>
"""
            for fname, fsize in files:
                if "armeabi-v7a" in fname:
                    html += f"""
        <div style="margin-bottom: 16px;">
            <div style="font-size: 14px; margin-bottom: 4px;">
                <span class="target">&#9733; Rekomendasi (ARM 32-bit / Android 5.1.1):</span>
            </div>
            <a class="btn" href="/{fname}" download>{fname} ({fsize})</a>
        </div>
"""
                else:
                    html += f"""
        <div style="margin-bottom: 16px;">
            <div style="font-size: 13px; color: #888; margin-bottom: 4px;">Perangkat 64-bit (ARM 64-bit):</div>
            <a class="btn btn-secondary" href="/{fname}" download>{fname} ({fsize})</a>
        </div>
"""

            html += f"""
        <div class="info">
            <strong>Catatan Instalasi:</strong><br>
            1. Buka URL ini di browser HP target: <code>http://{lan_ip}:{PORT}</code><br>
            2. Klik tombol unduh di atas.<br>
            3. Jika muncul peringatan keamanan, izinkan "Install from unknown sources" di pengaturan HP.
        </div>
    </div>
</body>
</html>"""
            self.wfile.write(html.encode("utf-8"))
            return

        # Serve APK with proper MIME type
        if self.path.endswith(".apk"):
            filepath = os.path.join(DIST_DIR, self.path.lstrip("/"))
            if os.path.exists(filepath):
                self.send_response(200)
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Length", str(os.path.getsize(filepath)))
                self.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(filepath)}"')
                self.end_headers()
                with open(filepath, "rb") as f:
                    self.copyfile(f, self.wfile)
                return

        super().do_GET()


def main():
    if not os.path.exists(DIST_DIR):
        os.makedirs(DIST_DIR)

    lan_ip = get_lan_ip()
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", port), ApkServerHandler) as httpd:
        print("=" * 60)
        print(" HP Monitor — Local APK Download Server")
        print("=" * 60)
        print(f" Status: Berjalan di 0.0.0.0:{port}")
        print()
        print(f" Buka URL ini di browser HP Android target (Samsung J1 Ace):")
        print(f"   -> http://{lan_ip}:{port}")
        print()
        print(f" Atau direct link download ARM 32-bit:")
        print(f"   -> http://{lan_ip}:{port}/app-armeabi-v7a-release.apk")
        print()
        print(" Tekan Ctrl+C untuk menghentikan server.")
        print("=" * 60)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer dihentikan.")


if __name__ == "__main__":
    main()
