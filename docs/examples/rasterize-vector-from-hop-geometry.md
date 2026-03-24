# Example: Rasterize Vector from a Hop geometry field

Use `Rasterize Vector` with:

- Input source mode: `HOP_GEOMETRY_FIELD`
- Geometry field: `geom`
- Burn strategy: `ATTRIBUTE_FIELD`
- Burn attribute field: `class_id`
- Output raster: `/data/out.tif`
- Output format: `GTiff`
- Grid mode: `BOUNDS_RESOLUTION`
- Bounds: `2600000,1200000,2601000,1201000`
- CRS: `EPSG:2056`
- Resolution X / Y: `1` / `1`
- Output data type: `Byte`

Implementation detail:

- each input row is converted to a temporary `/vsimem/*.geojson` dataset
- GDAL rasterize then burns that dataset into the target raster
