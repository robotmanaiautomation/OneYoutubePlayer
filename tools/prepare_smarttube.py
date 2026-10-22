#!/usr/bin/env python3
"""Generate an isolated source workspace; never rewrite upstream submodules.

This prepares sources, NOT a binary-compatible SmartTube Android library. Gradle,
resource, native-library and dependency integration are separate build gates.
"""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "vendor/SmartTube"
OUTPUT = ROOT / ".generated/smarttube"
DOT_OLD = "com.google.android.exoplayer2"
DOT_NEW = "dev.oneyoutubeplayer.smarttube.exoplayer2"
REPLACEMENTS = (
    (DOT_OLD, DOT_NEW),
    (DOT_OLD.replace(".", "/"), DOT_NEW.replace(".", "/")),
    (DOT_OLD.replace(".", "_"), DOT_NEW.replace(".", "_")),
)
TEXT_SUFFIXES = {".java", ".kt", ".xml", ".gradle", ".kts", ".pro",
                 ".properties", ".cc", ".cpp", ".c", ".h", ".cmake", ".txt", ".json", ".proto"}
IGNORE_DIRS = {".git", ".gradle", ".idea", ".kotlin", "build", "captures"}


def relocate(text):
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    return text


def transform_source(relative, text):
    text = relocate(text)
    if relative == "exoplayer-amzn-2.10.6/constants.gradle":
        # 3.17.3's macOS artifact is not runnable on this Apple Silicon host.
        text = text.replace("protobufVersion = '3.17.3'", "protobufVersion = '3.25.5'")
    return text


def commit(path):
    return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=path, text=True).strip()


def prepare():
    required = [SOURCE / "settings.gradle", SOURCE / "MediaServiceCore/youtubeapi",
                SOURCE / "SharedModules/sharedutils"]
    if not all(path.exists() for path in required):
        raise RuntimeError("Initialize vendor/SmartTube submodules first")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    marker = OUTPUT / ".oneplayer-generated"
    if OUTPUT.exists():
        if not marker.is_file() or OUTPUT.is_symlink():
            raise RuntimeError("Refusing to replace an unmarked output directory")
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir()
    marker.write_text("Disposable generated SmartTube sources\n")
    changed = []
    count = 0
    for current, dirs, files in __import__("os").walk(SOURCE):
        dirs[:] = sorted(d for d in dirs if d not in IGNORE_DIRS)
        for name in sorted(files):
            source = Path(current) / name
            if name == ".git" or source.is_symlink():
                continue
            relative = source.relative_to(SOURCE)
            target = OUTPUT / relocate(relative.as_posix())
            target.parent.mkdir(parents=True, exist_ok=True)
            data = source.read_bytes()
            if source.suffix in TEXT_SUFFIXES:
                try:
                    text = data.decode("utf-8")
                except UnicodeDecodeError:
                    text = None
                if text is not None:
                    converted = transform_source(relative.as_posix(), text).encode("utf-8")
                    if converted != data:
                        changed.append({"path": relative.as_posix(),
                                        "upstream_sha256": hashlib.sha256(data).hexdigest()})
                    data = converted
            target.write_bytes(data)
            shutil.copymode(source, target)
            if name == "gradlew":
                target.chmod(target.stat().st_mode | 0o111)
            count += 1
    report = {
        "status": "source-prepared-export-runtime-with-tools/export_smarttube.gradle",
        "smarttube_commit": commit(SOURCE),
        "media_service_core_commit": commit(SOURCE / "MediaServiceCore"),
        "shared_modules_commit": commit(SOURCE / "SharedModules"),
        "namespace": DOT_NEW,
        "copied_files": count,
        "transformed_files": changed,
        "downstream_validation": ["library-build", "dependency-isolation", "native-loading",
                          "one-apk-dex", "on-device-playback"],
    }
    (OUTPUT / "oneplayer-relocation.json").write_text(json.dumps(report, indent=2) + "\n")
    local_sdk = ROOT / ".local/android-sdk"
    if local_sdk.is_dir():
        (OUTPUT / "local.properties").write_text(f"sdk.dir={local_sdk}\n")
    elif (ROOT / "local.properties").is_file():
        sdk_lines = [line for line in (ROOT / "local.properties").read_text().splitlines()
                     if line.startswith("sdk.dir=")]
        (OUTPUT / "local.properties").write_text("\n".join(sdk_lines) + "\n")
    print(json.dumps({key: value for key, value in report.items()
                      if key != "transformed_files"}, indent=2))
    print(f"Transformed {len(changed)} source files; upstream sources unchanged.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    prepare()
