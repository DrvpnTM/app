#!/bin/bash
# Builds lantern-hop (lantern-hop/main.go: Lantern's client core, getlantern/radiance, GPL-3.0, run as
# a local SOCKS proxy) for Android, as an executable the app runs from its native library folder:
#   liblanternhop.so
# into libs-lantern/<abi>/. Built for arm64-v8a, armeabi-v7a and x86_64, like Aether; the app hides
# the option where it is missing.
#
# Usage: bash compile-lantern.sh
# Needs NDK_HOME and the Go version lantern-hop/go.mod names (Radiance pins it; a newer Go fails to link).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$root/libs-lantern"
SRC="$root/lantern-hop"

if [[ -z "${NDK_HOME:-}" || ! -d "$NDK_HOME" ]]; then
  echo "Android NDK: set NDK_HOME" >&2
  exit 1
fi
command -v go >/dev/null || { echo "Go: go not found" >&2; exit 1; }

TOOLCHAIN=""
for candidate in linux-x86_64 linux-arm64 darwin-arm64 darwin-x86_64; do
  if [[ -d "$NDK_HOME/toolchains/llvm/prebuilt/$candidate/bin" ]]; then
    TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/$candidate/bin"
    break
  fi
done
[[ -n "$TOOLCHAIN" ]] || { echo "no llvm toolchain under $NDK_HOME" >&2; exit 1; }

# Dr VPN's minSdk; the executable must start on Android 7 as well.
API_LEVEL=24
ABIS="${ABIS:-arm64-v8a armeabi-v7a x86_64}"
# novpn: a SOCKS proxy instead of a TUN device. The rest are the tags Radiance's own builds use
# (its justfile); without them sing-box lacks the protocols Lantern's servers speak (QUIC, ...).
TAGS="novpn,with_gvisor,with_quic,with_dhcp,with_wireguard,with_utls,with_acme"

for abi in $ABIS; do
  case "$abi" in
    arm64-v8a) goarch=arm64; goarm=""; cc="aarch64-linux-android$API_LEVEL-clang" ;;
    armeabi-v7a) goarch=arm; goarm=7; cc="armv7a-linux-androideabi$API_LEVEL-clang" ;;
    x86_64) goarch=amd64; goarm=""; cc="x86_64-linux-android$API_LEVEL-clang" ;;
    *) echo "unknown ABI $abi" >&2; exit 1 ;;
  esac
  echo "[lantern] $abi"
  mkdir -p "$OUT_DIR/$abi"
  # cgo is needed: on Android the DNS lookups and OS version Radiance's libraries use come from C.
  # 16 KB pages, as Android 15 devices ask for. sing-box's libbox reaches into the os package on
  # Android, so the link name check is off, as in Lantern's own mobile build.
  (cd "$SRC" && env \
    CGO_ENABLED=1 GOOS=android GOARCH="$goarch" GOARM="$goarm" \
    CC="$TOOLCHAIN/$cc" CXX="$TOOLCHAIN/$cc++" \
    GOFLAGS=-mod=mod \
    go build -tags "$TAGS" -trimpath \
      -ldflags "-s -w -checklinkname=0 -extldflags '-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'" \
      -o "$OUT_DIR/$abi/liblanternhop.so" .)
done

ls -la "$OUT_DIR"/*/
