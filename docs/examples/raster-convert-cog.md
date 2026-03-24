# Example: Raster Convert to COG

Use `Raster Convert` with:

- Input source mode: `LOCAL_FILE`
- Output source mode: `LOCAL_FILE`
- Output format: `COG`
- Creation options:
  - `COMPRESS=ZSTD`
  - `BLOCKSIZE=512`
- Additional translate args:
  - `-stats`

This maps to a `gdal_translate`-style job and produces a Cloud Optimized GeoTIFF directly.
