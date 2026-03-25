#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 <HOP_HOME> [classifier|target] [target]"
  echo "Targets: gdal-suite (default), vector-suite, raster-suite"
  echo "Classifier examples: osx-aarch64, osx-x86_64, linux-x86_64, linux-aarch64, windows-x86_64"
  exit 1
fi

HOP_HOME="$1"
ARG2="${2:-}"
ARG3="${3:-}"

TARGET="gdal-suite"
CLASSIFIER=""

is_target() {
  [[ "$1" == "vector-suite" || "$1" == "raster-suite" || "$1" == "gdal-suite" ]]
}

resolve_target() {
  local target="$1"

  case "$target" in
    vector-suite)
      BUILD_MODULE="assemblies/assemblies-vector-suite-${CLASSIFIER}"
      ZIP_PATH="${BUILD_MODULE}/target/hop-vector-suite-${VERSION}-${CLASSIFIER}.zip"
      ;;
    raster-suite)
      BUILD_MODULE="assemblies/assemblies-raster-suite-${CLASSIFIER}"
      ZIP_PATH="${BUILD_MODULE}/target/hop-raster-suite-${VERSION}-${CLASSIFIER}.zip"
      ;;
    gdal-suite)
      BUILD_MODULE="assemblies/assemblies-gdal-suite-${CLASSIFIER}"
      ZIP_PATH="${BUILD_MODULE}/target/hop-gdal-suite-${VERSION}-${CLASSIFIER}.zip"
      ;;
    *)
      echo "Unsupported target: $target"
      exit 1
      ;;
  esac
}

if [[ -n "$ARG2" ]]; then
  if is_target "$ARG2"; then
    TARGET="$ARG2"
  else
    CLASSIFIER="$ARG2"
  fi
fi

if [[ -n "$ARG3" ]]; then
  if ! is_target "$ARG3"; then
    echo "Invalid target: $ARG3"
    exit 1
  fi
  TARGET="$ARG3"
fi

if [[ -z "$CLASSIFIER" ]]; then
  OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
  ARCH="$(uname -m)"

  case "$OS/$ARCH" in
    darwin/arm64) CLASSIFIER="osx-aarch64" ;;
    darwin/x86_64) CLASSIFIER="osx-x86_64" ;;
    linux/x86_64) CLASSIFIER="linux-x86_64" ;;
    linux/aarch64|linux/arm64) CLASSIFIER="linux-aarch64" ;;
    *)
      echo "Could not auto-detect classifier for $OS/$ARCH"
      exit 1
      ;;
  esac
fi

VERSION="$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -n 1)"
resolve_target "$TARGET"

LEGACY_PLUGIN_DIRS=(
  "$HOP_HOME/plugins/transforms/ogr-reader"
  "$HOP_HOME/plugins/transforms/ogr-exporter"
  "$HOP_HOME/plugins/transforms/ogr-vector"
  "$HOP_HOME/plugins/transforms/gdal-raster-info"
  "$HOP_HOME/plugins/transforms/gdal-raster-convert"
  "$HOP_HOME/plugins/transforms/gdal-raster-clip"
  "$HOP_HOME/plugins/transforms/gdal-raster-reproject"
  "$HOP_HOME/plugins/transforms/gdal-raster-resize"
  "$HOP_HOME/plugins/transforms/gdal-raster-warp"
  "$HOP_HOME/plugins/transforms/gdal-raster-buildvrt"
  "$HOP_HOME/plugins/transforms/gdal-rasterize-vector"
  "$HOP_HOME/plugins/transforms/gdal-raster"
  "$HOP_HOME/plugins/transforms/gdal-suite"
)

mvn -pl "$BUILD_MODULE" -am -DskipTests package -U

mkdir -p "$HOP_HOME"
for dir in "${LEGACY_PLUGIN_DIRS[@]}"; do
  rm -rf "$dir"
done

unzip -q -o "$ZIP_PATH" -d "$HOP_HOME"

echo "Installed ${TARGET} (${CLASSIFIER}) into $HOP_HOME"
