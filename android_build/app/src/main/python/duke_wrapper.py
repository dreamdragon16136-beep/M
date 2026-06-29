from typing import List
import Duke2_Enhanced

def run_from_android(url: str, media_type: str, max_items: int = 0, max_depth: int = 2, max_pages: int = 1):
    """Wrapper exposed to Android via Chaquopy.
    Calls the existing run_scrape function in Duke2_Enhanced.py and returns the gallery path or status string.
    """
    try:
        # Normalize media types: allow 'all' to expand to all types
        if media_type == 'all':
            media_types = list(Duke2_Enhanced.MEDIA_EXTENSIONS.keys())
        else:
            media_types = [media_type]

        # Call the run_scrape function if available
        if hasattr(Duke2_Enhanced, 'run_scrape'):
            gallery = Duke2_Enhanced.run_scrape(
                url=url,
                media_types=media_types,
                min_size_kb=0,
                max_depth=max_depth,
                max_pages=max_pages,
                same_domain=True,
                proxy=None,
                bypass_choice=None,
                use_external_viewer=False,
                max_per_type=max_items,
                save_dir=None
            )
            return str(gallery)
        else:
            # Fallback: call main with args
            args = ['--url', url, '--media-type', media_type]
            if max_items and max_items > 0:
                args += ['--max-per-type', str(max_items)]
            if max_depth:
                args += ['--max-depth', str(max_depth)]
            if max_pages:
                args += ['--max-pages', str(max_pages)]
            Duke2_Enhanced.main(args)
            return 'started'
    except Exception as e:
        return 'error: ' + str(e)
