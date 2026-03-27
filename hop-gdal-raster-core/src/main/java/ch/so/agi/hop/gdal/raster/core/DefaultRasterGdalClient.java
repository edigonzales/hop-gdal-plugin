package ch.so.agi.hop.gdal.raster.core;

import ch.so.agi.gdal.ffm.Gdal;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultRasterGdalClient implements RasterGdalClient {
  @Override
  public String rasterInfo(DatasetRef input, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterInfo(
                toBindingDatasetRef(input), toBindingConfig(remoteAccess), args.toArray(String[]::new)));
  }

  @Override
  public void rasterConvert(
      DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterConvert(
                toBindingDatasetRef(output),
                toBindingDatasetRef(input),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void rasterClip(
      DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterClip(
                toBindingDatasetRef(output),
                toBindingDatasetRef(input),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void rasterReproject(
      DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterReproject(
                toBindingDatasetRef(output),
                toBindingDatasetRef(input),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void rasterResize(
      DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterResize(
                toBindingDatasetRef(output),
                toBindingDatasetRef(input),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void rasterMosaic(
      List<DatasetRef> inputs, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterMosaic(
                toBindingDatasetRef(output),
                inputs.stream().map(this::toBindingDatasetRef).toList(),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void rasterZonalStats(
      DatasetRef rasterInput,
      DatasetRef zonesInput,
      DatasetRef output,
      RemoteAccessSpec remoteAccess,
      List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.rasterZonalStats(
                toBindingDatasetRef(output),
                toBindingDatasetRef(rasterInput),
                toBindingDatasetRef(zonesInput),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  @Override
  public void vectorRasterize(
      DatasetRef vectorInput, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () ->
            Gdal.vectorRasterize(
                toBindingDatasetRef(output),
                toBindingDatasetRef(vectorInput),
                toBindingConfig(remoteAccess),
                args.toArray(String[]::new)));
  }

  public ch.so.agi.gdal.ffm.DatasetRef toBindingDatasetRef(DatasetRef ref) {
    return switch (ref.type()) {
      case LOCAL_FILE -> ch.so.agi.gdal.ffm.DatasetRef.local(Path.of(ref.value()));
      case HTTP_URL -> ch.so.agi.gdal.ffm.DatasetRef.httpUrl(ref.value());
      case GDAL_VSI -> ch.so.agi.gdal.ffm.DatasetRef.gdalVsi(ref.value());
    };
  }

  public ch.so.agi.gdal.ffm.GdalConfig toBindingConfig(RemoteAccessSpec remoteAccess) {
    ch.so.agi.gdal.ffm.GdalConfig config = ch.so.agi.gdal.ffm.GdalConfig.empty();
    if (remoteAccess == null) {
      return config;
    }

    Map<String, String> authConfig = new LinkedHashMap<>();
    AuthConfigSpec auth = remoteAccess.authConfig();
    switch (auth.type()) {
      case BASIC_AUTH -> {
        requireValue(auth.username(), "Basic auth username");
        requireValue(auth.password(), "Basic auth password");
        authConfig.put("GDAL_HTTP_AUTH", "BASIC");
        authConfig.put("GDAL_HTTP_USERPWD", auth.username() + ":" + auth.password());
      }
      case BEARER_TOKEN -> {
        requireValue(auth.bearerToken(), "Bearer token");
        authConfig.put("GDAL_HTTP_BEARER", auth.bearerToken());
      }
      case CUSTOM_HEADER -> {
        requireValue(auth.customHeaderName(), "Custom header name");
        requireValue(auth.customHeaderValue(), "Custom header value");
        authConfig.put("GDAL_HTTP_HEADERS", auth.customHeaderName() + ": " + auth.customHeaderValue());
      }
      case SIGNED_URL, NONE -> {
        // No additional GDAL auth options required here.
      }
    }

    if (!authConfig.isEmpty()) {
      config = config.withConfig(authConfig);
    }
    if (remoteAccess.configOptions() != null && !remoteAccess.configOptions().isEmpty()) {
      config = config.withConfig(remoteAccess.configOptions().values());
    }
    return config;
  }

  private static void requireValue(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
  }
}
