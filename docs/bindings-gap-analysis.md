# Bindings Gap Analysis

## Initial state

Before the raster-suite work, the public bindings mainly exposed:

- `Gdal.vectorTranslate(...)`
- `Gdal.translate(...)`
- `Gdal.warp(...)`
- OGR streaming APIs for vector read/write

The main gaps for raster transforms were:

- no public `info` API
- no public `buildVrt` API
- no public `rasterize` API
- runtime APIs were `Path`-centric
- GDAL config was effectively global instead of scoped per job
- no structured remote/auth model for HTTP/HTTPS COG access

## Implemented changes

The bindings now expose:

- `DatasetRef`
- `DatasetRefType`
- `GdalConfig`
- `ScopedGdalConfig`
- `Gdal.info(...)`
- `Gdal.buildVrt(...)`
- `Gdal.rasterize(...)`
- `DatasetRef` / `GdalConfig` overloads for `translate` and `warp`
- `DatasetRef` / `GdalConfig` overloads for `Ogr.open` and `Ogr.create`

Low-level FFM coverage was extended for:

- `GDALInfo`
- `GDALBuildVRT`
- `GDALRasterize`
- matching `*OptionsNew` / `*OptionsFree`
- thread-local GDAL config operations via CPL

## Remote dataset model

`DatasetRef` distinguishes:

- local path
- HTTP/HTTPS URL
- explicit GDAL/VSI path

HTTP/HTTPS URLs are turned into GDAL `/vsicurl/` identifiers instead of being downloaded in Java.

## Auth/config model

Supported V1 mappings:

- Basic auth
  - `GDAL_HTTP_AUTH=BASIC`
  - `GDAL_HTTP_USERPWD=user:password`
- Bearer token
  - `GDAL_HTTP_BEARER=<token>`
- Custom header
  - `GDAL_HTTP_HEADERS=Header: value`
- Signed URL
  - carried by the URL itself

Scoped config ensures these values are applied per operation and restored afterwards.

## Remaining follow-up items

- more direct integration tests around authenticated HTTP mock servers
- optional future exposures such as `DEMProcessing`, `Nearblack`, `Grid`
- possible regeneration of the checked-in low-level bindings through the jextract script once the
  native header workflow is refreshed again
