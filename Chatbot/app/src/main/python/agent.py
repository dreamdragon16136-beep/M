"""
Pixel Agent Python Backend
Runs inside Chaquopy on Android
Handles: file ops, reverse image search, archives, web search
"""
import os, sys, json, base64, shutil, zipfile, hashlib, re
from datetime import datetime

# Reverse Image Search
class ReverseImageSearch:
    @staticmethod
    def search_all(image_path):
        return {
            'trace_moe': ReverseImageSearch.trace_moe(image_path),
            'saucenao': ReverseImageSearch.saucenao(image_path),
            'google': ReverseImageSearch.google_images(image_path)
        }

    @staticmethod
    def trace_moe(image_path):
        try:
            import requests
            url = 'https://api.trace.moe/search'
            with open(image_path, 'rb') as f:
                files = {'image': f}
                resp = requests.post(url, files=files, timeout=30)
            data = resp.json()
            if not data.get('result'):
                return {'found': False, 'engine': 'trace.moe', 'message': 'No anime match found'}
            top = data['result'][0]
            similarity = top.get('similarity', 0) * 100
            return {
                'found': True, 'engine': 'trace.moe', 'type': 'anime_scene',
                'title': top.get('anilist', {}).get('title', {}).get('native', 'Unknown'),
                'title_english': top.get('anilist', {}).get('title', {}).get('english', ''),
                'episode': top.get('episode', 'N/A'),
                'timestamp': f"{int(top.get('from', 0)//60)}:{int(top.get('from', 0)%60):02d}",
                'similarity': f"{similarity:.1f}%"
            }
        except Exception as e:
            return {'found': False, 'engine': 'trace.moe', 'error': str(e)}

    @staticmethod
    def saucenao(image_path):
        try:
            import requests
            url = 'https://saucenao.com/search.php'
            with open(image_path, 'rb') as f:
                files = {'file': f}
                data = {'output_type': '2'}
                resp = requests.post(url, files=files, data=data, timeout=30)
            result = resp.json()
            if not result.get('results'):
                return {'found': False, 'engine': 'SauceNAO', 'message': 'No source found'}
            top = result['results'][0]
            header = top.get('header', {})
            d = top.get('data', {})
            return {
                'found': True, 'engine': 'SauceNAO', 'type': 'art_source',
                'similarity': header.get('similarity', '0%'),
                'source_url': d.get('ext_urls', [''])[0] if d.get('ext_urls') else '',
                'title': d.get('title', ''),
                'author': d.get('member_name', d.get('creator', '')),
                'source': d.get('source', '')
            }
        except Exception as e:
            return {'found': False, 'engine': 'SauceNAO', 'error': str(e)}

    @staticmethod
    def google_images(image_path):
        return {'found': True, 'engine': 'Google Images', 'type': 'web_search',
                'message': 'Open Google Lens for web results', 'lens_url': 'https://lens.google.com'}

# File Tools
class FileTools:
    FORBIDDEN = ['/etc/passwd', '/etc/shadow', '~/.ssh', '/proc', '/sys', '/dev']
    MAX_SIZE = 10 * 1024 * 1024

    def __init__(self, working_dir):
        self.working_dir = os.path.abspath(working_dir)
        os.makedirs(self.working_dir, exist_ok=True)

    def _resolve(self, path):
        abs_path = os.path.abspath(os.path.join(self.working_dir, path))
        if not abs_path.startswith(self.working_dir):
            raise ValueError("Path escapes working directory")
        return abs_path

    def list_directory(self, path='.'):
        abs_path = self._resolve(path)
        items = []
        for item in os.listdir(abs_path):
            fp = os.path.join(abs_path, item)
            items.append({
                'type': 'dir' if os.path.isdir(fp) else 'file',
                'name': item,
                'path': fp,
                'size': os.path.getsize(fp) if os.path.isfile(fp) else 0
            })
        return {'items': items, 'path': abs_path}

    def read_file(self, path, limit=1000):
        abs_path = self._resolve(path)
        if not os.path.exists(abs_path):
            return {'error': 'File not found'}
        with open(abs_path, 'r', errors='replace') as f:
            content = f.read(limit)
        return {'content': content}

    def write_file(self, path, content):
        abs_path = self._resolve(path)
        os.makedirs(os.path.dirname(abs_path), exist_ok=True)
        with open(abs_path, 'w') as f:
            f.write(content)
        return {'success': True}

    def delete_file(self, path):
        abs_path = self._resolve(path)
        if os.path.isdir(abs_path):
            shutil.rmtree(abs_path)
        else:
            os.remove(abs_path)
        return {'success': True}

    def create_directory(self, path):
        abs_path = self._resolve(path)
        os.makedirs(abs_path, exist_ok=True)
        return {'success': True}

    def move_file(self, src, dst):
        shutil.move(self._resolve(src), self._resolve(dst))
        return {'success': True}

    def copy_file(self, src, dst):
        src_path = self._resolve(src)
        dst_path = self._resolve(dst)
        if os.path.isdir(src_path):
            shutil.copytree(src_path, dst_path)
        else:
            shutil.copy2(src_path, dst_path)
        return {'success': True}

    def search_files(self, query, path='.'):
        abs_path = self._resolve(path)
        results = []
        for root, dirs, files in os.walk(abs_path):
            for f in files:
                if query.lower() in f.lower():
                    results.append(os.path.join(root, f))
        return {'results': results}

    def get_file_info(self, path):
        abs_path = self._resolve(path)
        stat = os.stat(abs_path)
        return {'path': abs_path, 'size': stat.st_size, 'modified': datetime.fromtimestamp(stat.st_mtime).isoformat()}

    def organize_files(self, path='.', by='extension'):
        abs_path = self._resolve(path)
        for item in os.listdir(abs_path):
            fp = os.path.join(abs_path, item)
            if os.path.isfile(fp):
                if by == 'extension':
                    ext = os.path.splitext(item)[1].lower() or 'no_extension'
                    dest = os.path.join(abs_path, ext.lstrip('.'))
                elif by == 'date':
                    mtime = datetime.fromtimestamp(os.path.getmtime(fp))
                    dest = os.path.join(abs_path, mtime.strftime('%Y-%m'))
                else:
                    dest = os.path.join(abs_path, 'misc')
                os.makedirs(dest, exist_ok=True)
                shutil.move(fp, os.path.join(dest, item))
        return {'success': True}

    def create_archive(self, source, archive_name, fmt='zip'):
        abs_src = self._resolve(source)
        abs_arc = self._resolve(archive_name)
        if fmt == 'zip':
            if os.path.isdir(abs_src):
                shutil.make_archive(abs_arc.replace('.zip', ''), 'zip', abs_src)
            else:
                with zipfile.ZipFile(abs_arc, 'w') as zf:
                    zf.write(abs_src, os.path.basename(abs_src))
        elif fmt == 'cbz':
            cbz_path = abs_arc if abs_arc.endswith('.cbz') else abs_arc + '.cbz'
            with zipfile.ZipFile(cbz_path, 'w') as zf:
                for root, dirs, files in os.walk(abs_src):
                    for f in files:
                        zf.write(os.path.join(root, f), f)
        elif fmt == '7z':
            try:
                import py7zr
                with py7zr.SevenZipFile(abs_arc if abs_arc.endswith('.7z') else abs_arc + '.7z', 'w') as sz:
                    if os.path.isdir(abs_src):
                        sz.writeall(abs_src, os.path.basename(abs_src))
                    else:
                        sz.write(abs_src, os.path.basename(abs_src))
            except ImportError:
                return {'error': 'py7zr not available'}
        return {'success': True, 'archive': abs_arc}

    def extract_archive(self, archive_path, dest='.'):
        abs_arc = self._resolve(archive_path)
        abs_dest = self._resolve(dest)
        os.makedirs(abs_dest, exist_ok=True)
        if abs_arc.endswith('.zip') or abs_arc.endswith('.cbz'):
            with zipfile.ZipFile(abs_arc, 'r') as zf:
                zf.extractall(abs_dest)
        elif abs_arc.endswith('.7z'):
            try:
                import py7zr
                with py7zr.SevenZipFile(abs_arc, 'r') as sz:
                    sz.extractall(abs_dest)
            except ImportError:
                return {'error': 'py7zr not available'}
        return {'success': True}

    def reverse_image_search(self, image_path):
        return ReverseImageSearch.search_all(self._resolve(image_path))

    def web_search(self, query):
        try:
            import requests
            url = f"https://html.duckduckgo.com/html/?q={requests.utils.quote(query)}"
            headers = {'User-Agent': 'Mozilla/5.0'}
            resp = requests.get(url, headers=headers, timeout=15)
            results = []
            snippets = re.findall(r'<a[^>]*class="result__a"[^>]*href="([^"]*)"[^>]*>(.*?)</a>', resp.text)
            for link, title in snippets[:5]:
                results.append({'title': re.sub(r'<[^>]+>', '', title), 'url': link})
            return {'results': results}
        except Exception as e:
            return {'error': str(e)}

    def web_browse(self, url):
        try:
            import requests
            headers = {'User-Agent': 'Mozilla/5.0'}
            resp = requests.get(url, headers=headers, timeout=15)
            text = re.sub(r'<[^>]+>', ' ', resp.text)
            text = re.sub(r'\s+', ' ', text).strip()[:5000]
            return {'content': text}
        except Exception as e:
            return {'error': str(e)}

    def download_file(self, url, filename):
        try:
            import requests
            abs_path = self._resolve(filename)
            resp = requests.get(url, timeout=60, stream=True)
            with open(abs_path, 'wb') as f:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)
            return {'success': True, 'path': abs_path, 'size': os.path.getsize(abs_path)}
        except Exception as e:
            return {'error': str(e)}

    def identify_image(self, image_path):
        try:
            from PIL import Image
            abs_path = self._resolve(image_path)
            img = Image.open(abs_path)
            return {'format': img.format, 'size': img.size, 'mode': img.mode}
        except Exception as e:
            return {'error': str(e)}

# Entry point called from Kotlin
def process_command(command_json):
    """Main entry point called from Kotlin UI"""
    try:
        cmd = json.loads(command_json)
        action = cmd.get('action')
        working_dir = cmd.get('working_dir', '/sdcard/PixelAgent')
        ft = FileTools(working_dir)

        if action == 'list':
            return json.dumps(ft.list_directory(cmd.get('path', '.')))
        elif action == 'read':
            return json.dumps(ft.read_file(cmd.get('path'), cmd.get('limit', 1000)))
        elif action == 'write':
            return json.dumps(ft.write_file(cmd.get('path'), cmd.get('content')))
        elif action == 'delete':
            return json.dumps(ft.delete_file(cmd.get('path')))
        elif action == 'mkdir':
            return json.dumps(ft.create_directory(cmd.get('path')))
        elif action == 'move':
            return json.dumps(ft.move_file(cmd.get('src'), cmd.get('dst')))
        elif action == 'copy':
            return json.dumps(ft.copy_file(cmd.get('src'), cmd.get('dst')))
        elif action == 'search':
            return json.dumps(ft.search_files(cmd.get('query'), cmd.get('path', '.')))
        elif action == 'info':
            return json.dumps(ft.get_file_info(cmd.get('path')))
        elif action == 'organize':
            return json.dumps(ft.organize_files(cmd.get('path', '.'), cmd.get('by', 'extension')))
        elif action == 'archive':
            return json.dumps(ft.create_archive(cmd.get('source'), cmd.get('name'), cmd.get('format', 'zip')))
        elif action == 'extract':
            return json.dumps(ft.extract_archive(cmd.get('archive'), cmd.get('dest', '.')))
        elif action == 'reverse_search':
            return json.dumps(ft.reverse_image_search(cmd.get('image_path')))
        elif action == 'web_search':
            return json.dumps(ft.web_search(cmd.get('query')))
        elif action == 'web_browse':
            return json.dumps(ft.web_browse(cmd.get('url')))
        elif action == 'download':
            return json.dumps(ft.download_file(cmd.get('url'), cmd.get('filename')))
        elif action == 'identify':
            return json.dumps(ft.identify_image(cmd.get('image_path')))
        else:
            return json.dumps({'error': f'Unknown action: {action}'})
    except Exception as e:
        return json.dumps({'error': str(e)})
