#!/bin/bash
# Builds the Aether core (patterniha/Aether, the WARP core PattN/PattNG ship) and its two helpers
# for Android, as executables the app runs from its native library folder:
#   libaether.so                Aether itself (Rust, with the "tor" feature: Tor inside or around WARP)
#   libpsiphon-tunnel-core.so   the Psiphon client Aether runs for its Psiphon modes (Go)
#   liblyrebird.so              Tor's pluggable transport for bridges: obfs4, snowflake, ... (Go)
# into libs-aether/<abi>/. Built for arm64-v8a, armeabi-v7a and x86_64; 32-bit x86 is left out as
# in PattNG (Aether's own releases don't cover it), and the app hides Aether where it is missing.
#
# Usage: bash compile-aether.sh [commit]   (defaults to aether-version.txt)
# Needs NDK_HOME, Rust (rustup, cargo) and Go.
# Adapted from PattNG's compile-aether.sh, compile-psiphon.sh and compile-pt.sh (GPL-3.0).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
commit="${1:-$(tr -d '[:space:]' < "$root/aether-version.txt")}"
OUT_DIR="$root/libs-aether"
SRC="$root/build-aether-src"

if [[ -z "${NDK_HOME:-}" || ! -d "$NDK_HOME" ]]; then
  echo "Android NDK: set NDK_HOME" >&2
  exit 1
fi
command -v cargo >/dev/null || { echo "Rust: cargo not found" >&2; exit 1; }
command -v go >/dev/null || { echo "Go: go not found" >&2; exit 1; }

if [[ ! -d "$SRC/.git" ]]; then
  git init -q "$SRC"
  git -C "$SRC" remote add origin https://github.com/patterniha/Aether.git
fi
git -C "$SRC" fetch -q --depth 1 origin "$commit"
git -C "$SRC" checkout -q --force FETCH_HEAD
git -C "$SRC" submodule update -q --init --recursive --depth 1

# Dr VPN's minSdk; the executables must start on Android 7 as well.
API_LEVEL=24
ABIS="armeabi-v7a arm64-v8a x86_64"

triple_for() {
  case "$1" in
    armeabi-v7a) echo "armv7-linux-androideabi" ;;
    arm64-v8a) echo "aarch64-linux-android" ;;
    x86_64) echo "x86_64-linux-android" ;;
  esac
}
clang_for() {
  case "$1" in
    armeabi-v7a) echo "armv7a-linux-androideabi$API_LEVEL" ;;
    *) echo "$(triple_for "$1")$API_LEVEL" ;;
  esac
}
goarch_for() {
  case "$1" in
    armeabi-v7a) echo "arm" ;;
    arm64-v8a) echo "arm64" ;;
    x86_64) echo "amd64" ;;
  esac
}

TOOLCHAIN=""
for candidate in linux-x86_64 linux-arm64 darwin-arm64 darwin-x86_64; do
  if [[ -d "$NDK_HOME/toolchains/llvm/prebuilt/$candidate/bin" ]]; then
    TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/$candidate/bin"
    break
  fi
done
[[ -n "$TOOLCHAIN" ]] || { echo "no llvm toolchain under $NDK_HOME" >&2; exit 1; }
SYSROOT="$(cd "$TOOLCHAIN/../sysroot" && pwd)"
CORE_DIR="$SRC/aether"

for abi in $ABIS; do
  triple="$(triple_for "$abi")"
  clang_target="$(clang_for "$abi")"
  env_triple="$(echo "$triple" | tr 'a-z-' 'A-Z_')"
  under_triple="$(echo "$triple" | tr '-' '_')"
  rustup target add "$triple" >/dev/null 2>&1 || true
  mkdir -p "$OUT_DIR/$abi"

  # The C++ runtime BoringSSL needs is linked statically: Android doesn't ship libc++_shared.so,
  # and a program run from the library folder would not start without it.
  echo "[aether] core for $abi"
  env \
    ANDROID_NDK_HOME="$NDK_HOME" ANDROID_NDK_ROOT="$NDK_HOME" \
    "CARGO_TARGET_${env_triple}_LINKER=$TOOLCHAIN/$clang_target-clang" \
    "CARGO_TARGET_${env_triple}_RUSTFLAGS=-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384" \
    "CC_${under_triple}=$TOOLCHAIN/clang" \
    "CXX_${under_triple}=$TOOLCHAIN/clang++" \
    "CFLAGS_${under_triple}=--target=$clang_target" \
    "CXXFLAGS_${under_triple}=--target=$clang_target" \
    "AR_${under_triple}=$TOOLCHAIN/llvm-ar" \
    "BINDGEN_EXTRA_CLANG_ARGS_${under_triple}=--target=$clang_target --sysroot=$SYSROOT" \
    "BORING_BSSL_RUST_CPPLIB_${under_triple}=static:-bundle=c++" \
    cargo build --release --locked --features tor --manifest-path "$CORE_DIR/Cargo.toml" --target "$triple" --bin aether
  produced="$CORE_DIR/target/$triple/release/aether"
  if "$TOOLCHAIN/llvm-readelf" --dynamic-table "$produced" | grep -q 'libc++_shared.so'; then
    echo "the core for $abi needs libc++_shared.so, which Android does not ship" >&2
    exit 1
  fi
  cp "$produced" "$OUT_DIR/$abi/libaether.so"

  goarch="$(goarch_for "$abi")"
  goarm=""
  [[ "$abi" == "armeabi-v7a" ]] && goarm="7"
  for helper in psiphon pt; do
    stage="$(mktemp -d)"
    echo "[aether] $helper for $abi"
    bash "$SRC/$helper-build.sh" linux "$goarch" "$stage" "$goarm"
    if [[ "$helper" == "psiphon" ]]; then
      cp "$stage/psiphon-tunnel-core" "$OUT_DIR/$abi/libpsiphon-tunnel-core.so"
    else
      cp "$stage/lyrebird" "$OUT_DIR/$abi/liblyrebird.so"
    fi
    rm -rf "$stage"
  done
done

ls -la "$OUT_DIR"/*/
