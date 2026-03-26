# Raster Suite Architecture

## Scope

V1 adds raster transforms only. Workflow/actions are intentionally excluded so the first release
can focus on stable row-driven pipeline behaviour and a clean binding surface in
`gdal-java-bindings`.

## Main layers

- `gdal-java-bindings`
  - exposes GDAL utility APIs over FFM/Panama
  - now includes `DatasetRef`, `GdalConfig`, `ScopedGdalConfig`, `info`, `buildVrt`,
    `rasterize`, plus `DatasetRef`-aware `translate`, `warp`, `Ogr.open` and `Ogr.create`
- `hop-gdal-raster-core`
  - shared Hop-side dataset/auth/config models
  - generic result-field handling
  - `ValueOrField<T>` resolution for constant-vs-row-field parameters
  - common GDAL adapter (`RasterGdalClient`)
  - `/vsimem/` helper for inline vector materialization
- concrete raster transforms
  - small Meta/Data/Transform/Dialog modules with transform-specific job specs encoded in the
    Meta classes and assembled into GDAL CLI-style argument lists
  - product-facing raster tasks are now split into `Info`, `Convert`, `Clip`, `Reproject`,
    `Resize`, `Mosaic` and `Rasterize Vector`
  - `Raster Mosaic` stays internally VRT-based through `buildVrt`

## Dataset model

Both bindings and Hop plugin use an explicit dataset reference model instead of implicit `Path`
handling:

- `LOCAL_FILE`
- `HTTP_URL`
- `GDAL_VSI`

That avoids local `Files.exists(...)` checks against remote datasets and keeps HTTP/HTTPS COGs as
first-class GDAL inputs.

For output datasets, the runtime allows only `LOCAL_FILE` and `GDAL_VSI`.
`HTTP_URL` output is rejected to avoid unsupported `/vsicurl/` write assumptions.

## Config and auth model

Remote access is split into:

- structured auth (`AuthConfigSpec`)
- free GDAL/VSI config key-values (`GdalConfigOptions`)

The bindings apply config with thread-local scope per operation using `ScopedGdalConfig`.
This keeps row/job-specific credentials isolated between transform copies.

## Result handling

Each raster transform uses the same technical status envelope:

- `gdal_success`
- `gdal_message`
- `gdal_elapsed_ms`
- `gdal_input`
- `gdal_output`
- `gdal_details_json`

## Geometry path for rasterization

`Rasterize Vector` supports two input modes:

- dataset/layer input
- Hop geometry field input

Hop geometry rows are converted to a temporary `/vsimem/*.geojson` dataset per row and then
processed through GDAL rasterize. This avoids shell-outs and keeps the business logic inside the
bindings plus shared raster core.

## Product direction

The raster product line intentionally follows smaller, task-oriented GDAL-style commands instead of
one broad spatial transform. The old wide `Warp` surface has been replaced by separate product
transforms for clipping, reprojection and resizing. This keeps dialogs shorter and makes it easier
to expose user-friendly presets without mixing unrelated options into one form.
