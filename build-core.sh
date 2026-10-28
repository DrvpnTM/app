#!/bin/bash
# Builds libv2ray.aar (the Xray core) from patterniha/AndroidLibXrayLite, the core PattN/PattNG use.
# Its Xray-core allows VLESS/Trojan without TLS to public addresses (the official core refuses
# them), and adds the oob dialMode, the merge finalmask and "+one" DoH.
#
# Differences from patterniha's own release build:
#   - built for Android 7 (API 24), Dr VPN's minSdk; their release .aar needs Android 10.
#   - bundles the Loyalsoldier geoip/geosite files the official core uses, so the routing
#     presets' tags (geosite:cn, geosite:gfw, geosite:category-ru, ...) keep resolving.
#
# Usage: bash build-core.sh [tag] [output.aar]
#   tag    defaults to core-version.txt (a patterniha/AndroidLibXrayLite release tag)
#   output defaults to android/app/libs/libv2ray.aar
# Needs Go, the Android SDK and NDK (ANDROID_HOME, and ANDROID_NDK_HOME or NDK_HOME).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
tag="${1:-$(tr -d '[:space:]' < "$root/core-version.txt")}"
out="${2:-$root/android/app/libs/libv2ray.aar}"
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${NDK_HOME:-}}"
if [ -z "$ANDROID_NDK_HOME" ] || [ ! -d "$ANDROID_NDK_HOME" ]; then
  echo "Set ANDROID_NDK_HOME (or NDK_HOME) to an Android NDK" >&2
  exit 1
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git clone --quiet --depth 1 --branch "$tag" https://github.com/patterniha/AndroidLibXrayLite "$work/src"
cd "$work/src"

mkdir -p assets
rm -f assets/*.dat
for f in geoip.dat geosite.dat; do
  curl -fsSL --retry 3 -o "assets/$f" "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/$f"
done
curl -fsSL --retry 3 -o assets/geoip-only-cn-private.dat \
  https://raw.githubusercontent.com/Loyalsoldier/geoip/release/geoip-only-cn-private.dat

# gomobile/gobind at the golang.org/x/mobile version go.mod pins.
mobile="$(go list -m -f '{{.Version}}' golang.org/x/mobile)"
go install "golang.org/x/mobile/cmd/gomobile@$mobile" "golang.org/x/mobile/cmd/gobind@$mobile"
export PATH="$(go env GOPATH)/bin:$PATH"
gomobile init
gomobile bind -androidapi 24 -trimpath -ldflags='-s -w -buildid= -checklinkname=0' -o "$work/libv2ray.aar" ./

mkdir -p "$(dirname "$out")"
cp "$work/libv2ray.aar" "$out"
echo "Built $out from patterniha/AndroidLibXrayLite $tag"
