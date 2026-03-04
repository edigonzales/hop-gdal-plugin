# hop-gdal-plugin

Apache Hop plugin project for reading and writing OGR vector data via `gdal-java-bindings`
and exposing geometry as `hop-geometry-type`.

## Status

## Modules

- `hop-ogr-core`
  - Shared OGR plugin helpers (classloader bridging, option text parsing).
- `hop-transform-ogr-reader`
  - Hop pipeline transform plugin (`OGR Input`) using GDAL OGR streaming API.
  - Supported options:
    - input file + layer (optional, fallback = first layer)
    - automatic layer discovery and field preview from datasource
    - selected attributes (CSV/semicolon)
    - attribute filter expression
    - spatial filter by BBOX or polygon WKT
    - feature limit
    - additional dataset open options (`key=value`)
    - FID output toggle and output field names
  - Examples (copy/paste):
    - Attribute filter expression: `status = 'ACTIVE' AND area_m2 >= 1000`
    - Spatial filter (BBOX): `2600000,1220000,2610000,1230000`
    - Spatial filter (polygon WKT): `POLYGON((2600000 1220000,2610000 1220000,2610000 1230000,2600000 1230000,2600000 1220000))`
    - Note: configure only one spatial filter at a time (`BBOX` or `polygon WKT`).
    - Feature limit: `5000` (positive integer, `> 0`)
- `hop-transform-ogr-exporter`
  - Hop pipeline transform plugin (`OGR Output`) using GDAL OGR write streaming API.
  - Supported options:
    - output file + format/driver
    - layer name
    - write mode (`FAIL_IF_EXISTS`, `OVERWRITE`, `APPEND`)
    - geometry input field
    - selected attributes (CSV/semicolon)
    - dataset creation options (`key=value`)
    - layer creation options (`key=value`)
    - optional forced geometry type (`AUTO`, `POINT`, `LINESTRING`, `POLYGON`, `MULTI*`)
- `assemblies/assemblies-transform-ogr-reader-<classifier>`
  - Platform-specific install ZIPs with runtime dependencies.
- `assemblies/assemblies-transform-ogr-exporter-<classifier>`
  - Platform-specific exporter ZIPs with runtime dependencies.
- `assemblies/assemblies-vector-suite-<classifier>`
  - Platform-specific suite ZIP containing both reader and exporter.
  - Installs a shared plugin folder (`ogr-vector`) with both transform JARs and one shared runtime `lib/`.
  - Classifiers:
    - `linux-x86_64`
    - `linux-aarch64`
    - `osx-x86_64`
    - `osx-aarch64`
    - `windows-x86_64`

## Build

Build all modules:

```bash
mvn clean verify
```

Use Java 23+ (verify with `javac -version`; must be `23` or higher).

If local snapshots are stale, republish bindings from local sources:

```bash
cd /Users/stefan/sources/gdal-java-bindings
./gradlew publishToMavenLocal
```

Build only exporter transform + one platform ZIP (example `osx-aarch64`):

```bash
mvn -pl hop-transform-ogr-exporter,assemblies/assemblies-transform-ogr-exporter-osx-aarch64 -am -DskipTests package
```

## Install in Hop

Unzip the desired platform ZIP into your Hop home.

Reader ZIP:

```bash
unzip -o assemblies/assemblies-transform-ogr-reader-osx-aarch64/target/hop-transform-ogr-reader-<version>-osx-aarch64.zip -d "$HOP_HOME"
```

Exporter ZIP:

```bash
unzip -o assemblies/assemblies-transform-ogr-exporter-osx-aarch64/target/hop-transform-ogr-exporter-<version>-osx-aarch64.zip -d "$HOP_HOME"
```

Vector suite ZIP (reader + exporter):

```bash
unzip -o assemblies/assemblies-vector-suite-osx-aarch64/target/hop-vector-suite-<version>-osx-aarch64.zip -d "$HOP_HOME"
```

Single-plugin ZIPs create:

```text
$HOP_HOME/plugins/transforms/ogr-reader
$HOP_HOME/plugins/transforms/ogr-exporter
```

Vector-suite ZIP creates:

```text
$HOP_HOME/plugins/transforms/ogr-vector
```

Migration note:
- If you switch from separate reader/exporter installs to vector-suite, remove old folders first to avoid mixed classloaders:
  - `$HOP_HOME/plugins/transforms/ogr-reader`
  - `$HOP_HOME/plugins/transforms/ogr-exporter`

## Fast Local Sync

Use:

```bash
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 ogr-reader
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 ogr-exporter
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 vector-suite
```

If classifier is omitted, the script auto-detects it on common macOS/Linux setups.
If target is omitted, it defaults to `ogr-reader` (backward compatible).
`vector-suite` sync automatically cleans `ogr-reader`, `ogr-exporter`, and `ogr-vector` before installing.
