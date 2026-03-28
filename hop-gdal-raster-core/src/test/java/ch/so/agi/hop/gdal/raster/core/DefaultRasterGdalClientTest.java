package ch.so.agi.hop.gdal.raster.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DefaultRasterGdalClientTest {
  @Test
  void emptyRemoteAccessProducesEmptyBindingConfig() {
    DefaultRasterGdalClient client = new DefaultRasterGdalClient();

    ch.so.agi.gdal.ffm.GdalConfig config =
        client.toBindingConfig(
            RemoteAccessSpec.empty(),
            new DatasetRef(DatasetRefType.HTTP_URL, "https://example.com/input.tif"));

    assertTrue(config.options().isEmpty());
  }

  @Test
  void preservesAuthAndUserConfigOptions() {
    DefaultRasterGdalClient client = new DefaultRasterGdalClient();
    RemoteAccessSpec remoteAccess =
        new RemoteAccessSpec(
            new AuthConfigSpec(AuthConfigSpec.AuthType.BASIC_AUTH, "user", "secret", null, null, null),
            new GdalConfigOptions(Map.of("GDAL_DISABLE_READDIR_ON_OPEN", "EMPTY_DIR")));

    ch.so.agi.gdal.ffm.GdalConfig config =
        client.toBindingConfig(
            remoteAccess, new DatasetRef(DatasetRefType.HTTP_URL, "https://example.com/input.tif"));

    assertEquals("BASIC", config.options().get("GDAL_HTTP_AUTH"));
    assertEquals("user:secret", config.options().get("GDAL_HTTP_USERPWD"));
    assertEquals("EMPTY_DIR", config.options().get("GDAL_DISABLE_READDIR_ON_OPEN"));
    assertFalse(config.options().containsKey("CURL_CA_BUNDLE"));
    assertFalse(config.options().containsKey("SSL_CERT_FILE"));
  }
}
