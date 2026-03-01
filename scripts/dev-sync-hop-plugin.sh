#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 <HOP_HOME> [classifier|target] [target]"
  echo "Targets: ogr-reader (default), ogr-exporter, vector-suite"
  echo "Classifier examples: osx-aarch64, osx-x86_64, linux-x86_64, linux-aarch64, windows-x86_64"
  exit 1
fi

HOP_HOME="$1"
ARG2="${2:-}"
ARG3="${3:-}"

TARGET="ogr-reader"
CLASSIFIER=""

is_target() {
  [[ "$1" == "ogr-reader" || "$1" == "ogr-exporter" || "$1" == "vector-suite" ]]
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

case "$TARGET" in
  ogr-reader)
    MODULE="assemblies/assemblies-transform-ogr-reader-${CLASSIFIER}"
    ZIP_PATH="${MODULE}/target/hop-transform-ogr-reader-${VERSION}-${CLASSIFIER}.zip"
    BUILD_MODULES="hop-transform-ogr-reader,${MODULE}"
    CLEAN_DIRS=("$HOP_HOME/plugins/transforms/ogr-reader")
    ;;
  ogr-exporter)
    MODULE="assemblies/assemblies-transform-ogr-exporter-${CLASSIFIER}"
    ZIP_PATH="${MODULE}/target/hop-transform-ogr-exporter-${VERSION}-${CLASSIFIER}.zip"
    BUILD_MODULES="hop-transform-ogr-exporter,${MODULE}"
    CLEAN_DIRS=("$HOP_HOME/plugins/transforms/ogr-exporter")
    ;;
  vector-suite)
    MODULE="assemblies/assemblies-vector-suite-${CLASSIFIER}"
    ZIP_PATH="${MODULE}/target/hop-vector-suite-${VERSION}-${CLASSIFIER}.zip"
    BUILD_MODULES="hop-transform-ogr-reader,hop-transform-ogr-exporter,${MODULE}"
    CLEAN_DIRS=(
      "$HOP_HOME/plugins/transforms/ogr-reader"
      "$HOP_HOME/plugins/transforms/ogr-exporter"
    )
    ;;
  *)
    echo "Unsupported target: $TARGET"
    exit 1
    ;;
esac

mvn -pl "$BUILD_MODULES" -am -DskipTests package

mkdir -p "$HOP_HOME"
for dir in "${CLEAN_DIRS[@]}"; do
  rm -rf "$dir"
done
unzip -q -o "$ZIP_PATH" -d "$HOP_HOME"

echo "Installed ${TARGET} (${CLASSIFIER}) into $HOP_HOME"
