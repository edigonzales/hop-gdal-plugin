# hop-gdal-plugin

Apache Hop plugin project for reading OGR vector data via `gdal-java-bindings`
and exposing geometry as `hop-geometry-type`.

## Status

Current milestone:
- Phase 0 completed: technical baseline decisions documented.
- Phase 2 completed (scaffold): Maven multi-module structure, transform skeleton, packaging ZIPs and unit tests are in place.
- Phase 3 started: OGR streaming reader implementation with layer/attribute/filter options.

Phase-0 baseline:
- `docs/phase-0-technical-baseline.md`

## Modules

- `hop-transform-ogr-reader`
  - Hop pipeline transform plugin (`OGR Input`) using GDAL OGR streaming API.
  - Supported options:
    - input file + layer (optional, fallback = first layer)
    - dialog-assisted layer discovery and field preview from datasource
    - selected attributes (CSV/semicolon)
    - attribute filter expression
    - spatial filter by BBOX or polygon WKT
    - feature limit
    - allowed OGR drivers
    - additional dataset open options (`key=value`)
    - FID output toggle and output field names
- `assemblies/assemblies-transform-ogr-reader-<classifier>`
  - Platform-specific install ZIPs with runtime dependencies.
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

Build only transform + one platform ZIP (example `osx-aarch64`):

```bash
mvn -pl hop-transform-ogr-reader,assemblies/assemblies-transform-ogr-reader-osx-aarch64 -am -DskipTests package
```

## Install in Hop

Unzip the desired platform ZIP into your Hop home:

```bash
unzip -o assemblies/assemblies-transform-ogr-reader-osx-aarch64/target/hop-transform-ogr-reader-<version>-osx-aarch64.zip -d "$HOP_HOME"
```

This creates:

```text
$HOP_HOME/plugins/transforms/ogr-reader
```

## Fast Local Sync

Use:

```bash
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64
```

If classifier is omitted, the script auto-detects it on common macOS/Linux setups.
