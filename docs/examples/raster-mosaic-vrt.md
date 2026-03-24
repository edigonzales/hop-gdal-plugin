# Example: Raster Mosaic (VRT)

Use `Raster Mosaic (VRT)` with:

- Input source interpretation: `LOCAL_FILE`
- Input list parameter source: `CONSTANT`
- Input raster list:

```text
/data/a.tif
/data/b.tif
/data/c.tif
```

- Output VRT: `/data/mosaic.vrt`
- Resolution strategy: `AVERAGE`
- Allow projection difference: disabled for the strict case

V1 note:

- one row equals one VRT build job
- multi-row grouping is not part of this version
