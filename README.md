# hop-gdal-plugin

Apache Hop 2.17 plugin suite for GDAL/OGR-based vector and raster transforms on Java 23 with
`gdal-java-bindings` and the Java Foreign Function & Memory API.

## Included suites

- Vector suite
  - `OGR Input`
  - `OGR Output`
- Raster suite
  - `Raster Info`
  - `Raster Convert`
  - `Raster Clip`
  - `Raster Reproject`
  - `Raster Resize`
  - `Raster Mosaic`
  - `Rasterize Vector`
- GDAL suite
  - all vector and raster transforms in one shared installable ZIP

The repository stays additive: the existing OGR/vector modules remain in place and the raster
modules live beside them in the same Maven reactor.

## Modules

- `hop-ogr-core`
  - Shared OGR helpers for vector transforms.
- `hop-gdal-raster-core`
  - Shared raster models, result fields, dataset/auth/config resolution, `/vsimem/` helpers and
    a testable GDAL client adapter over `gdal-java-bindings`.
- `hop-transform-ogr-reader`
- `hop-transform-ogr-exporter`
- `hop-transform-gdal-raster-info`
- `hop-transform-gdal-raster-convert`
- `hop-transform-gdal-raster-clip`
- `hop-transform-gdal-raster-reproject`
- `hop-transform-gdal-raster-resize`
- `hop-transform-gdal-raster-buildvrt`
- `hop-transform-gdal-rasterize-vector`
- `assemblies/`
  - Vector suite ZIPs.
  - Raster suite ZIPs.
  - GDAL suite ZIPs.

## Java and build

Build target is exactly Java 23 via `maven.compiler.release=23`.
Running the build with a newer JDK is fine as long as it still compiles with `--release 23`.

Build everything:

```bash
mvn clean verify
```

If local Maven snapshots of the bindings are stale:

```bash
cd /Users/stefan/sources/gdal-java-bindings
./gradlew publishToMavenLocal
```

The bindings themselves also target Java 23 and expose the raster APIs required by this repo:
`info`, `translate`, `warp`, `buildVrt`, `rasterize`, `DatasetRef`, `GdalConfig` and
`ScopedGdalConfig`.

## Install in Hop

Only suite ZIPs are shipped. Unzip one or more suite ZIPs into `HOP_HOME`.
All suites target the same plugin directory so shared libraries, including the GDAL native bundle,
live only once under `HOP_HOME`.

Examples for `osx-aarch64`:

```bash
unzip -o assemblies/assemblies-raster-suite-osx-aarch64/target/hop-raster-suite-<version>-osx-aarch64.zip -d "$HOP_HOME"
unzip -o assemblies/assemblies-vector-suite-osx-aarch64/target/hop-vector-suite-<version>-osx-aarch64.zip -d "$HOP_HOME"
unzip -o assemblies/assemblies-gdal-suite-osx-aarch64/target/hop-gdal-suite-<version>-osx-aarch64.zip -d "$HOP_HOME"
```

All suite ZIPs install into the same plugin directory:

```text
$HOP_HOME/plugins/transforms/gdal-suite
```

`vector-suite` and `raster-suite` are additive subsets of `gdal-suite`.
If you unzip both subset ZIPs into the same `HOP_HOME`, the result is one shared runtime
directory with the union of transform JARs and one shared `lib/`.

Start Hop with native access enabled:

```bash
HOP_JAVA_HOME=/path/to/java-23-or-newer \
HOP_OPTIONS="--enable-native-access=ALL-UNNAMED -Xmx2048m" \
./hop-gui.sh
```

## Fast local sync

The dev sync script is suite-only and defaults to `gdal-suite`.

```bash
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 vector-suite
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 raster-suite
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 gdal-suite
```

If the classifier is omitted, the script auto-detects it for common macOS/Linux setups.
If the target is omitted, it defaults to `gdal-suite`.
The script removes the old single-plugin directories and the previous split suite directories
before syncing the requested suite.

## Remote raster support

Remote raster datasets are first-class inputs in the raster transforms.
The shared dataset model distinguishes:

- local file
- HTTP/HTTPS URL
- explicit GDAL/VSI path

`HTTP_URL` inputs are passed to GDAL as `/vsicurl/` datasets, not downloaded upfront in Java.
This is intended for HTTP/HTTPS COG access and block-wise reads.

Supported auth modelling in V1:

- `NONE`
- `BASIC_AUTH`
- `BEARER_TOKEN`
- `SIGNED_URL`
- `CUSTOM_HEADER`

These modes are mapped into GDAL config options via the bindings:

- Basic auth: `GDAL_HTTP_AUTH`, `GDAL_HTTP_USERPWD`
- Bearer token: `GDAL_HTTP_BEARER`
- Custom header: `GDAL_HTTP_HEADERS`
- Signed URL: URL only, no extra auth config

Per-job config is scoped through `GdalConfig` / `ScopedGdalConfig`, so auth/config values do not
bleed across transform copies.

## Raster transform notes

- `Raster Info`
  - row-driven, JSON-first output
  - writes the full GDAL info JSON to `gdal_details_json`
- `Raster Convert`
  - wraps `gdal_translate`
  - intentionally focuses on format conversion plus friendly compression / tiling presets
- `Raster Clip`
  - uses `gdal_translate` for pixel-window clipping and `gdalwarp` for bounds / cutline clipping
- `Raster Reproject`
  - wraps `gdalwarp`
  - focuses on CRS change, target sizing and resampling
- `Raster Resize`
  - wraps `gdal_translate`
  - focuses on output size or output resolution changes
- `Raster Mosaic`
  - wraps `gdalbuildvrt`
  - V1 is intentionally VRT-only and expects a raster list per row, not grouped multi-row aggregation
- `Rasterize Vector`
  - wraps `gdal_rasterize`
  - supports dataset/layer input and Hop geometry field input
  - Hop geometry rows are materialized to `/vsimem/` vector datasets

All raster transforms can append the same technical result fields:

- `gdal_success`
- `gdal_message`
- `gdal_elapsed_ms`
- `gdal_input`
- `gdal_output`
- `gdal_details_json`

## Docs

- `docs/raster-suite-architecture.md`
- `docs/raster-transforms.md`
- `docs/dev-setup.md`
- `docs/bindings-gap-analysis.md`
- `docs/remote-raster-and-auth.md`
- `docs/examples/`

## V1 limitations

- V1 contains only pipeline transforms, no workflow/actions.
- `Raster Mosaic` is list-per-row only and currently writes virtual mosaics (VRT), not materialized outputs.
- The product line now favors smaller task-oriented transforms over a single broad warp dialog.
- Automatic migration of older `Raster Clip / Reproject / Resample` pipelines is out of scope.
- Sensitive auth values are modelled separately and not meant for verbose logging, but there is no
  dedicated secret store integration yet.
