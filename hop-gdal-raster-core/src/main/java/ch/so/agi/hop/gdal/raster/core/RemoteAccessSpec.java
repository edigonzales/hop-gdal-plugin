package ch.so.agi.hop.gdal.raster.core;

public record RemoteAccessSpec(AuthConfigSpec authConfig, GdalConfigOptions configOptions) {
  public RemoteAccessSpec {
    authConfig = authConfig == null ? new AuthConfigSpec(AuthConfigSpec.AuthType.NONE, null, null, null, null, null) : authConfig;
    configOptions = configOptions == null ? GdalConfigOptions.empty() : configOptions;
  }

  public static RemoteAccessSpec empty() {
    return new RemoteAccessSpec(null, null);
  }
}
