# Example: Raster Info on a public HTTP COG

Use `Raster Info` with:

- Input source mode: `HTTP_URL`
- Parameter source: `CONSTANT`
- Input raster / URL / VSI path:
  - `https://oin-hotosm.s3.amazonaws.com/5d9f3f0f4ceb4f0010b2977f/0/5d9f3f0f4ceb4f0010b29780.tif`
- Authentication type: `NONE`
- Additional info args:
  - `-stats`

Expected behaviour:

- `gdal_success=true`
- `gdal_details_json` contains the full GDAL info JSON
- no local file existence check is performed before the GDAL open
