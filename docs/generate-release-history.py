#!/usr/bin/env python3
"""Append the latest Android and desktop versions to GitHub release notes."""

import argparse
import json
from pathlib import Path
from urllib.parse import quote

REPOSITORY = "DrvpnTM/app"
DESKTOP_REPOSITORY = "DrvpnTM/satelite-proxy"
RAW_ICONS = "https://raw.githubusercontent.com/DrvpnTM/app/main/docs/icons/platforms"
MARKER = "<!-- RELEASE_HISTORY -->"


def read_releases(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as file:
        releases = json.load(file)
    if not isinstance(releases, list):
        raise ValueError(f"{path} must contain a JSON array")
    return releases


def asset_url(repository: str, tag: str, name: str) -> str:
    return (
        f"https://github.com/{repository}/releases/download/"
        f"{quote(tag, safe='')}/{quote(name, safe='')}"
    )


def icon(platform: str, label: str) -> str:
    return f'<img src="{RAW_ICONS}/{platform}.svg" width="28" alt="{label}"> **{label}**'


def version_link(tag: str, url: str, label: str | None = None) -> str:
    return f"[{label or tag}]({url})"


def android_downloads(assets: list[str], repository: str, tag: str) -> str:
    groups = [
        ("Universal", lambda name: name == "DrVPN.apk" or name.endswith("_universal.apk")),
        ("ARM64", lambda name: name.endswith("_arm64-v8a.apk")),
        ("ARMv7", lambda name: name.endswith("_armeabi-v7a.apk")),
        ("x64", lambda name: name.endswith("_x86_64.apk")),
        ("x86", lambda name: name.endswith("_x86.apk")),
    ]
    links: list[str] = []
    used: set[str] = set()
    for label, matches in groups:
        name = next((asset for asset in assets if asset not in used and matches(asset)), None)
        if name:
            used.add(name)
            links.append(f"[{label}]({asset_url(repository, tag, name)})")
    return " · ".join(links)


def desktop_download_rows(tag: str, release_url: str, assets: list[str]) -> list[str]:
    specs = [
        (
            "windows",
            "Windows",
            lambda name: name.endswith("_x64-setup.exe")
            or name.endswith("_x64_portable.zip"),
            {"_x64-setup.exe": "Installer", "_x64_portable.zip": "Portable"},
        ),
        (
            "linux",
            "Linux",
            lambda name: name.endswith(".AppImage"),
            {".AppImage": "AppImage"},
        ),
        (
            "macos",
            "macOS",
            lambda name: name.endswith(".dmg"),
            {"_x64.dmg": "Intel", "_aarch64.dmg": "Apple silicon"},
        ),
    ]
    rows: list[str] = []
    for platform, label, matches, asset_labels in specs:
        downloads = [
            f"[{asset_labels.get(next((suffix for suffix in asset_labels if name.endswith(suffix)), ''), name)}]"
            f"({asset_url(DESKTOP_REPOSITORY, tag, name)})"
            for name in assets
            if matches(name)
        ]
        if downloads:
            rows.append(
                f"| {icon(platform, label)} | {version_link(tag, release_url)} "
                f"| {' · '.join(downloads)} |"
            )
    return rows


def build_history(
    android: list[dict],
    desktop: list[dict],
    version: str,
    android_assets: list[str],
) -> str:
    current_tag = f"v{version}"
    android_versions: list[dict] = [
        {
            "tag_name": current_tag,
            "html_url": f"https://github.com/{REPOSITORY}/releases/tag/{current_tag}",
            "assets": [{"name": name} for name in android_assets],
        }
    ]

    rows = [
        "### آخرین نسخه / Latest version",
        "",
        "| سیستم‌عامل / OS | نسخه / Version | دانلود / Downloads |",
        "|---|---|---|",
    ]
    for release in android_versions:
        tag = release.get("tag_name", "")
        assets = [
            asset.get("name", "")
            for asset in release.get("assets", [])
            if asset.get("name", "").lower().endswith(".apk")
        ]
        downloads = android_downloads(assets, REPOSITORY, tag)
        if downloads:
            rows.append(
                f"| {icon('android', 'Android')} "
                f"| {version_link(tag, release.get('html_url', '#'))} "
                f"| {downloads} |"
            )

    # Only the newest published desktop release; older ones are in the archive link below.
    latest_desktop = next(
        (
            release
            for release in desktop
            if release.get("tag_name", "").startswith("v")
            and not release.get("draft")
            and not release.get("prerelease")
        ),
        None,
    )
    for release in [latest_desktop] if latest_desktop else []:
        tag = release.get("tag_name", "")
        assets = [asset.get("name", "") for asset in release.get("assets", [])]
        rows.extend(
            desktop_download_rows(
                tag,
                release.get("html_url", "#"),
                assets,
            )
        )

    rows.extend(
        [
            "",
            f"[مشاهده همه انتشارهای اندروید / Android release archive](https://github.com/{REPOSITORY}/releases)",
            "",
            f"[مشاهده همه انتشارهای دسکتاپ / Desktop release archive](https://github.com/{DESKTOP_REPOSITORY}/releases)",
            "",
        ]
    )
    return "\n".join(rows)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--body", type=Path, required=True)
    parser.add_argument("--android-releases", type=Path, required=True)
    parser.add_argument("--desktop-releases", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--android-assets", type=Path, required=True)
    args = parser.parse_args()

    body = args.body.read_text(encoding="utf-8")
    if body.count(MARKER) != 1:
        raise ValueError(f"release body must contain exactly one {MARKER} marker")

    current_assets = sorted(
        asset.name for asset in args.android_assets.iterdir() if asset.suffix.lower() == ".apk"
    )
    history = build_history(
        read_releases(args.android_releases),
        read_releases(args.desktop_releases),
        args.version,
        current_assets,
    )
    args.body.write_text(body.replace(MARKER, history), encoding="utf-8")


if __name__ == "__main__":
    main()
