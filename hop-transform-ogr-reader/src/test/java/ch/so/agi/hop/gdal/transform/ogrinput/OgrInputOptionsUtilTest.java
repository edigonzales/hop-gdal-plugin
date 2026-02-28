package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OgrInputOptionsUtilTest {

  @Test
  void shouldSplitCsvOrSemicolonAndDeduplicate() {
    List<String> values = OgrInputOptionsUtil.splitCsvOrSemicolon("name;value, name ; fid");
    assertEquals(List.of("name", "value", "fid"), values);
  }

  @Test
  void shouldReturnEmptyListForBlankSplitInput() {
    assertTrue(OgrInputOptionsUtil.splitCsvOrSemicolon("  ").isEmpty());
  }

  @Test
  void shouldParseKeyValueOptionsFromSemicolonSeparatedText() {
    Map<String, String> options = OgrInputOptionsUtil.parseKeyValueOptions("A=1;B=two");
    assertEquals(Map.of("A", "1", "B", "two"), options);
  }

  @Test
  void shouldParseKeyValueOptionsFromMultilineText() {
    Map<String, String> options =
        OgrInputOptionsUtil.parseKeyValueOptions(
            """
            X_POSSIBLE_NAMES=foo
            Y_POSSIBLE_NAMES=bar
            """);
    assertEquals(Map.of("X_POSSIBLE_NAMES", "foo", "Y_POSSIBLE_NAMES", "bar"), options);
  }

  @Test
  void shouldRejectInvalidKeyValueOptionEntry() {
    assertThrows(
        IllegalArgumentException.class, () -> OgrInputOptionsUtil.parseKeyValueOptions("INVALID"));
  }

  @Test
  void shouldParsePositiveLimit() {
    assertEquals(42L, OgrInputOptionsUtil.parsePositiveLimit("42"));
  }

  @Test
  void shouldRejectZeroLimit() {
    assertThrows(IllegalArgumentException.class, () -> OgrInputOptionsUtil.parsePositiveLimit("0"));
  }

  @Test
  void shouldRejectSpatialFilterConflict() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OgrInputOptionsUtil.validateSpatialFilterExclusivity(
                "1,2,3,4", "POLYGON((0 0,1 0,1 1,0 1,0 0))"));
  }
}
