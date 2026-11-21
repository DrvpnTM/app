#!/bin/bash
# Builds OpenVPN 2 (GPL-2.0, run as a program of its own) with OpenSSL for Android, as an
# executable the app runs from its native library folder:
#   libopenvpn.so
# into libs-openvpn/<abi>/. It is the engine of "Dr Open VPN" (RiseupVPN, OpenVpnManager).
#
# Built with TARGET_ANDROID: OpenVPN then opens no TUN device and changes no routes itself; it asks
# the app over its management socket (doc/android.txt in OpenVPN's source) and gets the VpnService
# TUN fd back. configure only knows Linux, so its TARGET_LINUX is swapped for TARGET_ANDROID.
#
# Usage: bash compile-openvpn.sh
# Needs NDK_HOME (or ANDROID_NDK_HOME), curl, make and perl.
set -euo pipefail

OPENVPN_VERSION=2.6.12
OPENVPN_SHA256=1c610fddeb686e34f1367c347e027e418e07523a10f4d8ce4a2c2af2f61a1929
OPENSSL_VERSION=3.0.15
OPENSSL_SHA256=23c666d0edf20f14249b3d8f0368acaee9ab585b09e1de82107c66e1f3ec9533

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$root/libs-openvpn"
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

fetch() { # url sha256 file
  if [[ ! -f "$work/$3" ]]; then
    curl -sSfL --retry 3 -o "$work/$3" "$1"
  fi
  echo "$2  $work/$3" | sha256sum -c - >/dev/null || { echo "checksum mismatch for $3" >&2; exit 1; }
}
fetch "https://swupdate.openvpn.org/community/releases/openvpn-$OPENVPN_VERSION.tar.gz" "$OPENVPN_SHA256" openvpn.tar.gz
fetch "https://github.com/openssl/openssl/releases/download/openssl-$OPENSSL_VERSION/openssl-$OPENSSL_VERSION.tar.gz" "$OPENSSL_SHA256" openssl.tar.gz

export PATH="$TOOLCHAIN:$PATH"
export ANDROID_NDK_ROOT="$NDK"

for abi in $ABIS; do
  case "$abi" in
    arm64-v8a) host=aarch64-linux-android; ossl=android-arm64 ;;
    armeabi-v7a) host=armv7a-linux-androideabi; ossl=android-arm ;;
    x86_64) host=x86_64-linux-android; ossl=android-x86_64 ;;
    *) echo "unknown ABI $abi" >&2; exit 1 ;;
  esac
  echo "[openvpn] $abi"
  build="$work/build-$abi"
  prefix="$build/prefix"
  rm -rf "$build"
  mkdir -p "$build" "$prefix"

  # OpenSSL: static libraries only.
  tar xzf "$work/openssl.tar.gz" -C "$build"
  (cd "$build/openssl-$OPENSSL_VERSION" &&
    ./Configure "$ossl" -D__ANDROID_API__=$API_LEVEL no-shared no-tests no-engine no-dso \
      --prefix="$prefix" --libdir=lib >/dev/null &&
    make -j"$JOBS" build_libs >/dev/null &&
    make install_dev >/dev/null)

  # OpenVPN: no compression (RiseupVPN uses none), no plugins, no Linux DCO.
  tar xzf "$work/openvpn.tar.gz" -C "$build"
  src="$build/openvpn-$OPENVPN_VERSION"
  (cd "$src" &&
    CC="$host$API_LEVEL-clang" AR=llvm-ar RANLIB=llvm-ranlib STRIP=llvm-strip \
    CFLAGS="-O2 -fPIE" LDFLAGS="-pie -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384" \
    OPENSSL_CFLAGS="-I$prefix/include" OPENSSL_LIBS="-L$prefix/lib -lssl -lcrypto" \
    LIBCAPNG_CFLAGS=" " LIBCAPNG_LIBS=" " \
    ./configure --host="$host" --with-crypto-library=openssl \
      --disable-lzo --disable-lz4 --disable-plugins --disable-dco --disable-unit-tests \
      --disable-plugin-auth-pam --disable-plugin-down-root --disable-selinux --disable-systemd \
      --enable-iproute2=no >/dev/null)
  # TARGET_ANDROID instead of TARGET_LINUX (see the top of this file). configure insists on
  # libcap-ng on Linux; Android has none and OpenVPN does not drop capabilities there. --mktun
  # (tun persist) has only a Linux implementation.
  sed -i.bak -e 's|^#define TARGET_LINUX 1|#define TARGET_ANDROID 1|' -e 's|^#define HAVE_LIBCAPNG 1|/* no libcap-ng */|' -e 's|^#define ENABLE_FEATURE_TUN_PERSIST 1|/* no --mktun on Android */|' "$src/config.h"
  grep -q '^#define TARGET_ANDROID 1' "$src/config.h" || { echo "config.h has no TARGET_LINUX to replace" >&2; exit 1; }
  (cd "$src" && make -j"$JOBS" -C src/compat >/dev/null && make -j"$JOBS" -C src/openvpn openvpn >/dev/null)

  mkdir -p "$OUT_DIR/$abi"
  llvm-strip -o "$OUT_DIR/$abi/libopenvpn.so" "$src/src/openvpn/openvpn"
done

ls -la "$OUT_DIR"/*/
