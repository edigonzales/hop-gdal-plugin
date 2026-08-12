package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.gdal.ffm.OgrWriteMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OgrOutputOptionsUtilTest {

  @Test
  void shouldSplitCsvOrSemicolonAndDeduplicate() {
    List<String> values = OgrOutputOptionsUtil.splitCsvOrSemicolon("name;value, name ; fid");
    assertEquals(List.of("name", "value", "fid"), values);
  }

  @Test
  void shouldParseKeyValueOptions() {
    Map<String, String> options = OgrOutputOptionsUtil.parseKeyValueOptions("A=1;B=two");
    assertEquals(Map.of("A", "1", "B", "two"), options);
  }

  @Test
  void shouldParseWriteMode() {
    assertEquals(OgrWriteMode.APPEND, OgrOutputOptionsUtil.parseWriteMode("append"));
    assertEquals(OgrWriteMode.FAIL_IF_EXISTS, OgrOutputOptionsUtil.parseWriteMode(null));
  }

  @Test
  void shouldRejectInvalidWriteMode() {
    assertThrows(IllegalArgumentException.class, () -> OgrOutputOptionsUtil.parseWriteMode("invalid"));
  }

  @Test
  void shouldParseForcedGeometryCodes() {
    assertEquals(-1, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("AUTO"));
    assertEquals(1, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("POINT"));
    assertEquals(6, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("MULTIPOLYGON"));
    assertEquals(8, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("CIRCULARSTRING"));
    assertEquals(9, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("COMPOUNDCURVE"));
    assertEquals(10, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("CURVEPOLYGON"));
    assertEquals(11, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("MULTICURVE"));
    assertEquals(12, OgrOutputOptionsUtil.parseForcedGeometryTypeCode("MULTISURFACE"));
  }

  @Test
  void shouldExposeSqlMmCurveTypesInForceGeometrySelection() {
    assertTrue(OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES.contains("CIRCULARSTRING"));
    assertTrue(OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES.contains("COMPOUNDCURVE"));
    assertTrue(OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES.contains("CURVEPOLYGON"));
    assertTrue(OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES.contains("MULTICURVE"));
    assertTrue(OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES.contains("MULTISURFACE"));
  }

  @Test
  void shouldRejectInvalidForcedGeometryType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> OgrOutputOptionsUtil.parseForcedGeometryTypeCode("CIRCLE"));
  }

  @Test
  void shouldTrimToNull() {
    assertTrue(OgrOutputOptionsUtil.trimToNull("   ") == null);
  }

  @Test
  void shouldPreserveCurrentFormatSelectionWhenStillAvailable() {
    assertEquals(
        "CSV",
        OgrOutputOptionsUtil.resolveFormatSelection("CSV", List.of("Amigo Cloud", "CSV", "GPKG")));
  }

  @Test
  void shouldMatchCurrentFormatSelectionIgnoringCase() {
    assertEquals(
        "CSV",
        OgrOutputOptionsUtil.resolveFormatSelection("csv", List.of("Amigo Cloud", "CSV", "GPKG")));
  }

  @Test
  void shouldFallbackFormatSelectionToFirstAvailableValue() {
    assertEquals(
        "Amigo Cloud",
        OgrOutputOptionsUtil.resolveFormatSelection(
            "GeoJSON", List.of("Amigo Cloud", "CSV", "GPKG")));
  }
}
