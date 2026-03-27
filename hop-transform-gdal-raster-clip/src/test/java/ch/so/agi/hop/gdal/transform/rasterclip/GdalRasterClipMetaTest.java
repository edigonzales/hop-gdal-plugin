package ch.so.agi.hop.gdal.transform.rasterclip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.ICheckResult;
import org.junit.jupiter.api.Test;

class GdalRasterClipMetaTest {
  @Test
  void defaultsAreInitialized() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    assertEquals("BOUNDING_BOX", meta.getClipMode());
    assertEquals("CONSTANT", meta.getClipParameterSourceMode());
    assertEquals("FAIL_IF_EXISTS", meta.getOutputWriteMode());
  }

  @Test
  void fieldBasedBoundsValidationRequiresFieldName() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setClipParameterSourceMode("FIELD");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Bounds field is required", remarks.getFirst().getText());
  }

  @Test
  void pixelWindowConstantValidationRequiresFourValues() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setClipMode("PIXEL_WINDOW");
    meta.setPixelWindow("1,2,3");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals(
        "Pixel window must contain four values: xoff,yoff,xsize,ysize",
        remarks.getFirst().getText());
  }

  @Test
  void inlineGeometryFieldValidationRequiresFieldName() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setClipMode("INLINE_GEOMETRY");
    meta.setClipParameterSourceMode("FIELD");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Inline geometry field is required", remarks.getFirst().getText());
  }

  @Test
  void templateDatasetFieldValidationRequiresFieldName() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setClipMode("TEMPLATE_DATASET");
    meta.setClipParameterSourceMode("FIELD");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals("Template dataset field is required", remarks.getFirst().getText());
  }

  @Test
  void templateLayerMayStayOptionalConstant() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setClipMode("TEMPLATE_DATASET");
    meta.setTemplateDatasetValue("/tmp/template.gpkg");
    meta.setTemplateLayerName("mask");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_OK, remarks.getFirst().getType());
  }

  @Test
  void checkShouldRejectHttpUrlOutputMode() {
    GdalRasterClipMeta meta = new GdalRasterClipMeta();
    meta.setDefault();
    meta.setInputValue("/tmp/in.tif");
    meta.setOutputValue("/tmp/out.tif");
    meta.setOutputSourceMode("HTTP_URL");

    List<ICheckResult> remarks = new ArrayList<>();
    meta.check(remarks, null, null, null, null, null, null, null, null);

    assertFalse(remarks.isEmpty());
    assertEquals(ICheckResult.TYPE_RESULT_ERROR, remarks.getFirst().getType());
    assertEquals(
        "HTTP/HTTPS output is not supported; use LOCAL_FILE or GDAL_VSI",
        remarks.getFirst().getText());
  }
}
