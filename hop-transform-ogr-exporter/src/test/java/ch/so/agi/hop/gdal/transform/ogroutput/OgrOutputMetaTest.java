package ch.so.agi.hop.gdal.transform.ogroutput;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;

class OgrOutputMetaTest {

  @Test
  void shouldSetExpectedDefaults() {
    OgrOutputMeta meta = new OgrOutputMeta();
    meta.setDefault();

    assertEquals("", meta.getFileName());
    assertEquals("", meta.getFormat());
    assertEquals("", meta.getLayerName());
    assertEquals("FAIL_IF_EXISTS", meta.getWriteMode());
    assertEquals("", meta.getGeometryField());
    assertEquals("", meta.getSelectedAttributes());
    assertEquals("", meta.getDatasetCreationOptions());
    assertEquals("", meta.getLayerCreationOptions());
    assertEquals("AUTO", meta.getForceGeometryType());
  }

  @Test
  void shouldAddErrorWhenFileNameIsMissing() {
    OgrOutputMeta meta = configuredMeta();
    meta.setFileName(" ");

    List<ICheckResult> remarks = check(meta);
    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorWhenFormatIsMissing() {
    OgrOutputMeta meta = configuredMeta();
    meta.setFormat(" ");

    List<ICheckResult> remarks = check(meta);
    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorWhenGeometryFieldIsMissing() {
    OgrOutputMeta meta = configuredMeta();
    meta.setGeometryField(" ");

    List<ICheckResult> remarks = check(meta);
    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorWhenWriteModeIsInvalid() {
    OgrOutputMeta meta = configuredMeta();
    meta.setWriteMode("INVALID");

    List<ICheckResult> remarks = check(meta);
    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddOkWhenConfigurationIsValid() {
    OgrOutputMeta meta = configuredMeta();

    List<ICheckResult> remarks = check(meta);
    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_OK, remarks.getFirst().getType());
  }

  private static OgrOutputMeta configuredMeta() {
    OgrOutputMeta meta = new OgrOutputMeta();
    meta.setDefault();
    meta.setFileName("/tmp/out.gpkg");
    meta.setFormat("GPKG");
    meta.setGeometryField("geom");
    return meta;
  }

  private static List<ICheckResult> check(OgrOutputMeta meta) {
    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrOutput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);
    return remarks;
  }
}
