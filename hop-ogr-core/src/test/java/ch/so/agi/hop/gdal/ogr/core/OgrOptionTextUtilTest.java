package ch.so.agi.hop.gdal.ogr.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OgrOptionTextUtilTest {

  @Test
  void shouldSplitCsvOrSemicolonAndDeduplicate() {
    List<String> values = OgrOptionTextUtil.splitCsvOrSemicolon("name;value, name ; fid");
    assertEquals(List.of("name", "value", "fid"), values);
  }

  @Test
  void shouldParseKeyValueOptions() {
    Map<String, String> options = OgrOptionTextUtil.parseKeyValueOptions("A=1;B=two");
    assertEquals(Map.of("A", "1", "B", "two"), options);
  }

  @Test
  void shouldRejectInvalidKeyValueOption() {
    assertThrows(
        IllegalArgumentException.class, () -> OgrOptionTextUtil.parseKeyValueOptions("INVALID"));
  }

  @Test
  void shouldParsePositiveLong() {
    assertEquals(42L, OgrOptionTextUtil.parsePositiveLong("42", "Limit"));
  }

  @Test
  void shouldRejectInvalidPositiveLong() {
    assertThrows(
        IllegalArgumentException.class, () -> OgrOptionTextUtil.parsePositiveLong("0", "Limit"));
  }

  @Test
  void shouldValidateMutuallyExclusive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> OgrOptionTextUtil.validateMutuallyExclusive("a", "b", "one", "two"));
  }

  @Test
  void shouldReturnNullForBlankTrimToNull() {
    assertTrue(OgrOptionTextUtil.trimToNull("  ") == null);
  }
}
