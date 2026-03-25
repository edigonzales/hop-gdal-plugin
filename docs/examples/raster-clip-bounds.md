# Example: Raster Clip (Bounds)

Use `Raster Clip` with:

- `Clip mode = BOUNDING_BOX`
- input as local file, HTTP URL or explicit GDAL/VSI path
- bounds as `minX,minY,maxX,maxY`
- output as GeoTIFF or COG

Typical use:

- clip a public COG down to a smaller working extent before later analysis
