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

## Bundled Unix CA defaults

For remote HTTPS access on macOS and Linux, the plugin relies on the bundled
`gdal-java-bindings` runtime defaults:

- Unix native classifier bundles now ship `ssl/cacert.pem`
- the bindings runtime exposes that file as `CURL_CA_BUNDLE` and `SSL_CERT_FILE`
- the defaults are only applied when neither environment variables nor Java system properties
  already define either key

The Hop plugin itself only maps auth plus explicit `GDAL/VSI config options` into `GdalConfig`.
User-supplied CA-related config still wins because scoped config overrides the runtime defaults.

## Config option syntax

`GDAL/VSI config options` use `key=value` entries separated by commas, semicolons or new lines.

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
- missing or incompatible CA trust material

## V1 boundaries

- signed URLs are modelled as URL-only; there is no separate signing helper
- there is no secret vault integration yet
- advanced cloud/object-store profiles are not yet exposed as dedicated UI concepts
- additional enterprise or self-signed CAs still require explicit user-provided config
