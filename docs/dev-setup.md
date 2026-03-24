# Development Setup

## Repositories

- `hop-gdal-plugin`
- `gdal-java-bindings`

Both repositories should be available locally because the raster work depends on new snapshot
artifacts from the bindings.

## Java

- build target: Java 23
- local builds can use a newer JDK if the build still compiles with `--release 23`
- Hop runtime should be started with native access enabled

Example:

```bash
export JAVA_HOME=/path/to/jdk
export PATH="$JAVA_HOME/bin:$PATH"
```

## Bindings workflow

Rebuild and publish the bindings locally:

```bash
cd /Users/stefan/sources/gdal-java-bindings
./gradlew publishToMavenLocal
```

Optional integration tests:

```bash
GDAL_FFM_RUN_INTEGRATION=true ./gradlew :gdal-ffm-core:integrationTest
```

## Plugin workflow

Build all modules:

```bash
cd /Users/stefan/sources/hop-gdal-plugin
mvn clean verify
```

Build the combined suite ZIP for one platform:

```bash
mvn -pl assemblies/assemblies-gdal-suite-osx-aarch64 -am -DskipTests package
```

Install quickly into a local Hop home:

```bash
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64
```

Optional subset installs use the same shared plugin directory:

```bash
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 vector-suite
./scripts/dev-sync-hop-plugin.sh "$HOP_HOME" osx-aarch64 raster-suite
```

## Runtime

Start Hop with native access:

```bash
HOP_OPTIONS="--enable-native-access=ALL-UNNAMED -Xmx2048m" ./hop-gui.sh
```

## Test material

- bindings integration tests use `GDAL_FFM_TESTDATA_DIR` when available
- plugin examples are documented in `docs/examples/`
