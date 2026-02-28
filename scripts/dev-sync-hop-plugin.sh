#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <HOP_HOME> [classifier]"
  echo "Classifier examples: osx-aarch64, osx-x86_64, linux-x86_64, linux-aarch64, windows-x86_64"
  exit 1
fi

HOP_HOME="$1"
CLASSIFIER="${2:-}"
PLUGIN_DIR="$HOP_HOME/plugins/transforms/ogr-reader"

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
MODULE="assemblies/assemblies-transform-ogr-reader-${CLASSIFIER}"
ZIP_PATH="${MODULE}/target/hop-transform-ogr-reader-${VERSION}-${CLASSIFIER}.zip"

mvn -pl "hop-transform-ogr-reader,${MODULE}" -am -DskipTests package

mkdir -p "$HOP_HOME"
rm -rf "$PLUGIN_DIR"
unzip -q -o "$ZIP_PATH" -d "$HOP_HOME"

echo "Installed OGR Reader plugin (${CLASSIFIER}) into $HOP_HOME"
