# Raster Transforms

## Raster Info

- UI name: `Raster Info`
- Binding/API: `Gdal.info(...)`
- Purpose: read raster metadata as JSON
- Primary output: full JSON payload in `gdal_details_json`

## Raster Convert

- UI name: `Raster Convert`
- Binding/API: `Gdal.translate(...)`
- Purpose: format conversion and structural raster changes
- Typical uses:
  - GeoTIFF to COG
  - band selection
  - datatype changes
  - source/coordinate window extraction

## Raster Clip / Reproject / Resample

- UI name: `Raster Clip / Reproject / Resample`
- Binding/API: `Gdal.warp(...)`
- Purpose: spatial transformation
- Typical uses:
  - reprojection
  - resolution changes
  - bounds clipping
  - cutline/AOI clipping

## Raster Mosaic (VRT)

- UI name: `Raster Mosaic (VRT)`
- Binding/API: `Gdal.buildVrt(...)`
- Purpose: generate a VRT mosaic or stack from a list of raster inputs
- V1 limitation:
  - source list is supplied per row as semicolon/newline/JSON-array text
  - no grouped row aggregation yet

## Rasterize Vector

- UI name: `Rasterize Vector`
- Binding/API: `Gdal.rasterize(...)`
- Purpose: burn vector content into a raster
- Supported V1 inputs:
  - dataset/layer
  - Hop geometry field

## Common behaviour

- row-driven execution
- fail-fast or continue-and-mark-row-failed
- constant value or field-driven parameter resolution where relevant
- shared result fields for status, timing and diagnostics
