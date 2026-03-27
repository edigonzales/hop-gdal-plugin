# Raster Transforms

## Raster Info

- UI name: `Raster Info`
- Binding/API: `Gdal.rasterInfo(...)`
- Purpose: read raster metadata as JSON
- Primary output: full JSON payload in `gdal_details_json`

## Raster Convert

- UI name: `Raster Convert`
- Binding/API: `Gdal.rasterConvert(...)`
- Purpose: format conversion with user-friendly output presets
- Typical uses:
  - GeoTIFF to COG
  - GeoTIFF compression presets
  - tiling presets for GTiff/COG
  - format-specific creation option overrides

## Raster Clip

- UI name: `Raster Clip`
- Binding/API: `Gdal.rasterClip(...)`
- Purpose: spatial clipping only
- Typical uses:
  - bounds clipping
  - pixel window clipping
  - cutline clipping from inline geometry or template dataset

## Raster Reproject

- UI name: `Raster Reproject`
- Binding/API: `Gdal.rasterReproject(...)`
- Purpose: CRS change and target sizing
- Typical uses:
  - reprojection into a target CRS
  - target resolution or target size
  - optional output bounds and nodata handling

## Raster Resize

- UI name: `Raster Resize`
- Binding/API: `Gdal.rasterResize(...)`
- Purpose: output size or output resolution changes without clip-specific UI
- Typical uses:
  - resize to fixed width/height
  - resize to target resolution
  - choose explicit resampling

## Raster Mosaic

- UI name: `Raster Mosaic`
- Binding/API: `Gdal.rasterMosaic(...)`
- Purpose: generate a virtual raster mosaic or stack from a list of raster inputs
- V1 limitation:
  - source list is supplied per row as semicolon/newline/JSON-array text
  - output is VRT-only in this phase
  - no grouped row aggregation yet

## Rasterize Vector

- UI name: `Rasterize Vector`
- Binding/API: `Gdal.vectorRasterize(...)`
- Purpose: burn vector content into a raster
- Supported V1 inputs:
  - dataset/layer
  - Hop geometry field

## Common behaviour

- row-driven execution
- fail-fast or continue-and-mark-row-failed
- constant value or field-driven parameter resolution where relevant
- shared result fields for status, timing and diagnostics
