package ch.so.agi.hop.gdal.raster.core;

public record AuthConfigSpec(
    AuthType type,
    String username,
    String password,
    String bearerToken,
    String customHeaderName,
    String customHeaderValue) {

  public AuthConfigSpec {
    type = type == null ? AuthType.NONE : type;
  }

  public enum AuthType {
    NONE,
    BASIC_AUTH,
    BEARER_TOKEN,
    SIGNED_URL,
    CUSTOM_HEADER;

    public static AuthType fromValue(String value) {
      if (value == null || value.isBlank()) {
        return NONE;
      }
      return switch (value.trim().toUpperCase()) {
        case "BASIC", "BASIC_AUTH" -> BASIC_AUTH;
        case "BEARER", "BEARER_TOKEN" -> BEARER_TOKEN;
        case "SIGNED_URL" -> SIGNED_URL;
        case "CUSTOM_HEADER" -> CUSTOM_HEADER;
        default -> NONE;
      };
    }
  }
}
