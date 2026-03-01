package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.junit.jupiter.api.Test;

class OgrInputMetaTest {

  @Test
  void shouldSetExpectedDefaults() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();

    assertEquals("", meta.getFileName());
    assertEquals("", meta.getLayerName());
    assertTrue(meta.isIncludeFid());
    assertEquals("fid", meta.getFidFieldName());
    assertEquals("geometry", meta.getGeometryFieldName());
    assertEquals("", meta.getSelectedAttributes());
    assertEquals("", meta.getAttributeFilter());
    assertEquals("", meta.getBbox());
    assertEquals("", meta.getPolygonWkt());
    assertEquals("", meta.getFeatureLimit());
    assertEquals("", meta.getOpenOptions());
  }

  @Test
  void shouldAddGeometryAndFidFieldsByDefault() throws Exception {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertTrue(rowMeta.indexOfValue("fid") >= 0);
    assertTrue(rowMeta.indexOfValue("geometry") >= 0);
    assertEquals(2, rowMeta.size());
  }

  @Test
  void shouldSkipFidFieldWhenDisabled() throws Exception {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setIncludeFid(false);

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertTrue(rowMeta.indexOfValue("fid") < 0);
    assertTrue(rowMeta.indexOfValue("geometry") >= 0);
    assertEquals(1, rowMeta.size());
  }

  @Test
  void shouldFallbackToDefaultGeometryFieldNameWhenGeometryFieldNameIsBlank() throws Exception {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setIncludeFid(false);
    meta.setGeometryFieldName("  ");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertTrue(rowMeta.indexOfValue("geometry") >= 0);
    assertEquals(1, rowMeta.size());
  }

  @Test
  void shouldAddSelectedAttributesAsStringFieldsInDeclaredMetadata() throws Exception {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setSelectedAttributes("name; value ,name");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertTrue(rowMeta.indexOfValue("name") >= 0);
    assertTrue(rowMeta.indexOfValue("value") >= 0);
    assertTrue(rowMeta.indexOfValue("fid") >= 0);
    assertTrue(rowMeta.indexOfValue("geometry") >= 0);
    assertEquals(4, rowMeta.size());
  }

  @Test
  void shouldAddErrorRemarkWhenFileNameIsMissing() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setFileName(" ");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrInput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddOkRemarkWhenFileNameIsConfigured() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrInput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_OK, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorRemarkWhenBothSpatialFiltersAreConfigured() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");
    meta.setBbox("1,2,3,4");
    meta.setPolygonWkt("POLYGON((0 0,1 0,1 1,0 1,0 0))");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrInput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorRemarkWhenFeatureLimitIsInvalid() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");
    meta.setFeatureLimit("0");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrInput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }

  @Test
  void shouldAddErrorRemarkWhenOpenOptionsAreInvalid() {
    OgrInputMeta meta = new OgrInputMeta();
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");
    meta.setOpenOptions("INVALID");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(
        remarks,
        new PipelineMeta(),
        new TransformMeta("OgrInput", meta),
        null,
        new String[0],
        new String[0],
        null,
        null,
        null);

    assertEquals(1, remarks.size());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
  }
}
