# Remote Raster and Auth

## Principle

Remote rasters are first-class inputs. The plugin does not pre-download HTTP/HTTPS datasets into a
local temp file just to satisfy GDAL. Reads go through GDAL/VSI.

## Input modes

Raster transforms expose these source modes:

- `LOCAL_FILE`
- `HTTP_URL`
- `GDAL_VSI`

`HTTP_URL` is appropriate for public or authenticated HTTP/HTTPS COGs.
`GDAL_VSI` is the escape hatch for explicit VSI paths.

## Output modes

For output targets, only these modes are supported:

- `LOCAL_FILE`
- `GDAL_VSI`

`HTTP_URL` output is intentionally blocked. GDAL `/vsicurl/` is treated as a read-focused
dataset path and is not used as a reliable generic write target in this plugin.

## Authentication

The shared model distinguishes auth type from free-form config:

- none
- basic auth
- bearer token
- signed URL
- custom header

That is reflected in Hop dialogs, job resolution and the binding config layer.

## GDAL config flow

1. Hop transform resolves auth/config values for the current row/job.
2. `DefaultRasterGdalClient` maps them into `GdalConfig`.
3. The bindings apply them with `ScopedGdalConfig`.
4. GDAL opens the dataset under those config values.
5. Previous thread-local values are restored afterwards.

## Diagnostics

The common result envelope carries:

- success/failure
- message
- elapsed time
- resolved input/output identifiers
- details JSON

For remote failures, the goal is to surface whether the issue is:

- wrong URL/dataset identifier
- auth failure
- header/token mismatch
- generic GDAL open/read problem

## V1 boundaries

- signed URLs are modelled as URL-only; there is no separate signing helper
- there is no secret vault integration yet
- advanced cloud/object-store profiles are not yet exposed as dedicated UI concepts
