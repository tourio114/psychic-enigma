#!/usr/bin/env python3
"""Download the official Gradle distribution, verify SHA-256, then build with JDK 25+."""
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '9.6.0'

def main():
    java_home = os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if java_home else shutil.which('java')
    if not java:
        raise RuntimeError('Missing JDK 25+. Install JDK 25 and set JAVA_HOME, then run again.')
    check = subprocess.run([java, '-version'], capture_output=True, text=True, check=True)
    match = re.search(r'version "(\d+)', check.stdout + check.stderr)
    if not match or int(match.group(1)) < 25:
        raise RuntimeError('This Minecraft target requires JDK 25 or newer.')
    home = ROOT / '.tools'
    gradle = home / f'gradle-{VERSION}' / 'bin' / ('gradle.bat' if os.name == 'nt' else 'gradle')
    if not gradle.exists():
        home.mkdir(exist_ok=True)
        url = f'https://services.gradle.org/distributions/gradle-{VERSION}-bin.zip'
        archive = home / 'gradle.zip'
        print(f'Downloading official Gradle {VERSION}...', flush=True)
        with urllib.request.urlopen(url + '.sha256', timeout=30) as r:
            expected = r.read().decode().split()[0].lower()
        with urllib.request.urlopen(url, timeout=60) as source, archive.open('wb') as out:
            shutil.copyfileobj(source, out)
        actual = hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest()
        if actual != expected:
            archive.unlink()
            raise RuntimeError('Gradle SHA-256 mismatch')
        with zipfile.ZipFile(archive) as z:
            for item in z.infolist():
                if not (home / item.filename).resolve().is_relative_to(home.resolve()):
                    raise RuntimeError('Invalid Gradle archive entry')
            z.extractall(home)
        archive.unlink()
        if os.name != 'nt': gradle.chmod(0o755)
    subprocess.run([str(gradle), '--no-daemon', 'build', '--stacktrace', *sys.argv[1:]], cwd=ROOT, check=True)
    print('Build complete: build/libs/dynamic-island-0.2.0.jar')

if __name__ == '__main__':
    try: main()
    except Exception as exc:
        print(f'Build failed: {exc}', file=sys.stderr)
        sys.exit(1)
