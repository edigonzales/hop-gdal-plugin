# Phase 0 - Technical Baseline

This document captures binding decisions for the first implementation stage.
These decisions are intentionally strict to de-risk Phase 1 and MVP delivery.

## 1) Runtime and Java Baseline

Decision:
- Build target: Java 23
- Runtime target: Java 23+ (Java 25 supported)
- Hop runtime for this plugin must run on Java 23+

Why:
- `gdal-ffm-core` is built with Java 23 class level and uses FFM runtime access.
- Running this plugin on Java 17 would fail with class version/runtime constraints.

Consequence:
- This plugin is not Java-17-compatible in the first release line.
- Documentation and smoke tests must always mention:
  - `--enable-native-access=ALL-UNNAMED`

## 2) Packaging Strategy for Native Artifacts

Decision:
- Produce platform-specific plugin ZIPs (one ZIP per classifier), not one universal ZIP.
- Supported classifiers from `gdal-ffm-natives`:
  - `linux-x86_64`
  - `linux-aarch64`
  - `osx-x86_64`
  - `osx-aarch64`
  - `windows-x86_64`

Why:
- Native loader aborts when multiple bundled native manifests for one platform are present.
- A universal ZIP increases ambiguity/risk and makes support harder.
- Platform ZIPs keep deployment predictable and smaller.

Consequence:
- Assembly layout in later phases will use one module/profile per classifier.
- Release process will publish multiple install ZIPs.

## 3) Scope Decision for MVP Transform

Decision:
- MVP transform type: source transform (no required input stream).
- File input in MVP: static file path only (variables allowed).
- Layer selection in MVP: explicit layer name or default-first-layer.

Why:
- Fastest path to verified streaming reader end-to-end.
- Reduces early complexity in row-to-row field resolution.

Out of MVP (post-MVP):
- File path from upstream row field.
- Multi-file fan-out behavior from incoming rows.

## 4) Filter and Selection Scope (MVP)

In MVP, implement:
- Attribute projection (selected fields only)
- Attribute filter (OGR attribute filter expression)
- Spatial filter by BBOX
- Spatial filter by polygon WKT

Additional low-cost options to include in MVP:
- Open options (`key=value` list)
- Allowed drivers list
- Include FID output toggle
- Feature limit

## 5) Dependency and Repository Baseline

Dependencies to be used in plugin modules:
- `ch.so.agi:gdal-ffm-core`
- `ch.so.agi:gdal-ffm-natives` or `ch.so.agi:gdal-ffm-natives-swiss` (exactly one classifier)
- `ch.so.agi:hop-geometry-type`

Maven repository baseline:
- `https://jars.sogeo.services/snapshots`
- `https://jars.sogeo.services/releases`

## 6) Phase 1 Entry Criteria

Before starting plugin code, Phase 1 in `gdal-java-bindings` must provide:
- Working `OgrRuntime` implementation (open/read/close path)
- Layer discovery API (layer count/name access)
- Field metadata discovery API (field list + primitive type info)
- Filter wiring in runtime (attribute + spatial)

If one of these is missing, plugin implementation is blocked or would require unsafe workarounds.
