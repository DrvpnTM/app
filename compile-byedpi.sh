#!/bin/bash
# Builds ByeDPI (hufrea/byedpi, MIT; its program is "ciadpi") for Android, as an executable the
# app runs from its native library folder:
#   libbyedpi.so
# into libs-byedpi/<abi>/. It is the "Dr Anti-DPI" extra hop (ByeDpiManager): a local SOCKS proxy
# that splits, reorders or fakes the first packets of each connection to the server, so DPI does
# not recognise it.
#
# Usage: bash compile-byedpi.sh
# Needs NDK_HOME (or ANDROID_NDK_HOME), curl and make.
set -euo pipefail

BYEDPI_COMMIT=7efde1b1296eaaa187b70e951894dde17527489c   # tag v0.17.3
BYEDPI_SHA256=ca696af46ebe22f81dae66531aa7fcb53a77d636ffbb5685e68d08f6e61ebcc1

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$root/libs-byedpi"
NDK="${NDK_HOME:-${ANDROID_NDK_HOME:-}}"
if [[ -z "$NDK" || ! -d "$NDK" ]]; then
  echo "Android NDK: set NDK_HOME" >&2
  exit 1
fi

TOOLCHAIN=""
for candidate in linux-x86_64 darwin-x86_64 darwin-arm64; do
  if [[ -d "$NDK/toolchains/llvm/prebuilt/$candidate/bin" ]]; then
    TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$candidate/bin"
    break
  fi
done
[[ -n "$TOOLCHAIN" ]] || { echo "no llvm toolchain under $NDK" >&2; exit 1; }

# Dr VPN's minSdk; the executable must start on Android 7 as well.
API_LEVEL=24
ABIS="${ABIS:-arm64-v8a armeabi-v7a x86_64}"
JOBS="$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)"

work="${WORK_DIR:-$(mktemp -d)}"
[[ -n "${WORK_DIR:-}" ]] || trap 'rm -rf "$work"' EXIT
mkdir -p "$work"
if [[ ! -f "$work/byedpi.tar.gz" ]]; then
  curl -sSfL --retry 3 -o "$work/byedpi.tar.gz" "https://github.com/hufrea/byedpi/archive/$BYEDPI_COMMIT.tar.gz"
fi
echo "$BYEDPI_SHA256  $work/byedpi.tar.gz" | sha256sum -c - >/dev/null || { echo "checksum mismatch for byedpi" >&2; exit 1; }

for abi in $ABIS; do
  case "$abi" in
    arm64-v8a) cc="aarch64-linux-android$API_LEVEL-clang" ;;
    armeabi-v7a) cc="armv7a-linux-androideabi$API_LEVEL-clang" ;;
    x86_64) cc="x86_64-linux-android$API_LEVEL-clang" ;;
    *) echo "unknown ABI $abi" >&2; exit 1 ;;
  esac
  echo "[byedpi] $abi"
  build="$work/build-$abi"
  rm -rf "$build" && mkdir -p "$build"
  tar xzf "$work/byedpi.tar.gz" -C "$build" --strip-components=1
  # Not built as ANDROID_APP: the program then logs to stderr, which the app reads, instead of logcat.
  make -C "$build" -j"$JOBS" CC="$TOOLCHAIN/$cc" \
    CFLAGS="-I. -std=c99 -O2 -fPIE -Wno-unused" \
    LDFLAGS="-pie -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384" >/dev/null
  mkdir -p "$OUT_DIR/$abi"
  "$TOOLCHAIN/llvm-strip" -o "$OUT_DIR/$abi/libbyedpi.so" "$build/ciadpi"
done

ls -la "$OUT_DIR"/*/
