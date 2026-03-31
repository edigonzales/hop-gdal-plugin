package ch.so.agi.hop.gdal.transform.ogrinput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.variables.Variables;
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
  void shouldAddSelectedAttributesUsingProjectedFieldTypes() throws Exception {
    RecordingSchemaResolver schemaResolver =
        new RecordingSchemaResolver(
            List.of(
                new OgrFieldDefinition("name", OgrFieldType.STRING),
                new OgrFieldDefinition("value", OgrFieldType.REAL)));
    OgrInputMeta meta = new OgrInputMeta(schemaResolver);
    meta.setDefault();
    meta.setFileName("${DATASET}");
    meta.setLayerName("${LAYER}");
    meta.setOpenOptions("${OPEN_OPTIONS}");
    meta.setSelectedAttributes("name; value ,name");

    Variables variables = new Variables();
    variables.setVariable("DATASET", "/tmp/data.gpkg");
    variables.setVariable("LAYER", "buildings");
    variables.setVariable("OPEN_OPTIONS", "FLATTEN_NESTED_ATTRIBUTES=YES");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, variables, null);

    assertTrue(rowMeta.indexOfValue("name") >= 0);
    assertTrue(rowMeta.indexOfValue("value") >= 0);
    assertTrue(rowMeta.indexOfValue("fid") >= 0);
    assertTrue(rowMeta.indexOfValue("geometry") >= 0);
    assertEquals(4, rowMeta.size());
    assertEquals(IValueMeta.TYPE_STRING, rowMeta.getValueMeta(rowMeta.indexOfValue("name")).getType());
    assertEquals(IValueMeta.TYPE_NUMBER, rowMeta.getValueMeta(rowMeta.indexOfValue("value")).getType());
    assertEquals(Path.of("/tmp/data.gpkg"), schemaResolver.lastFilePath);
    assertEquals("buildings", schemaResolver.lastLayerName);
    assertEquals("name; value ,name", schemaResolver.lastSelectedAttributes);
    assertEquals(Map.of("FLATTEN_NESTED_ATTRIBUTES", "YES"), schemaResolver.lastOpenOptions);
  }

  @Test
  void shouldDeclareAllLayerFieldsWhenSelectedAttributesAreBlank() throws Exception {
    OgrInputMeta meta =
        new OgrInputMeta(
            new RecordingSchemaResolver(
                List.of(
                    new OgrFieldDefinition("egid", OgrFieldType.INTEGER64),
                    new OgrFieldDefinition("art_txt", OgrFieldType.STRING),
                    new OgrFieldDefinition("height", OgrFieldType.REAL))));
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertIterableEquals(List.of("fid", "egid", "art_txt", "height", "geometry"), fieldNames(rowMeta));
    assertEquals(IValueMeta.TYPE_INTEGER, rowMeta.getValueMeta(rowMeta.indexOfValue("egid")).getType());
    assertEquals(IValueMeta.TYPE_STRING, rowMeta.getValueMeta(rowMeta.indexOfValue("art_txt")).getType());
    assertEquals(IValueMeta.TYPE_NUMBER, rowMeta.getValueMeta(rowMeta.indexOfValue("height")).getType());
  }

  @Test
  void shouldOmitFidWhenDisabledWhileKeepingResolvedLayerFields() throws Exception {
    OgrInputMeta meta =
        new OgrInputMeta(
            new RecordingSchemaResolver(
                List.of(
                    new OgrFieldDefinition("egid", OgrFieldType.INTEGER64),
                    new OgrFieldDefinition("art_txt", OgrFieldType.STRING))));
    meta.setDefault();
    meta.setIncludeFid(false);
    meta.setFileName("/tmp/data.gpkg");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertIterableEquals(List.of("egid", "art_txt", "geometry"), fieldNames(rowMeta));
  }

  @Test
  void shouldFallbackToConservativeMetadataWhenSchemaProbeFails() throws Exception {
    OgrInputMeta meta =
        new OgrInputMeta(
            (filePath, requestedLayerName, selectedAttributes, openOptions) -> {
              throw new IllegalArgumentException("boom");
            });
    meta.setDefault();
    meta.setFileName("/tmp/data.gpkg");
    meta.setSelectedAttributes("name; value");

    RowMeta rowMeta = new RowMeta();
    meta.getFields(rowMeta, "origin", null, null, null, null);

    assertIterableEquals(List.of("fid", "name", "value", "geometry"), fieldNames(rowMeta));
    assertEquals(IValueMeta.TYPE_STRING, rowMeta.getValueMeta(rowMeta.indexOfValue("name")).getType());
    assertEquals(IValueMeta.TYPE_STRING, rowMeta.getValueMeta(rowMeta.indexOfValue("value")).getType());
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

  private static List<String> fieldNames(RowMeta rowMeta) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i < rowMeta.size(); i++) {
      names.add(rowMeta.getValueMeta(i).getName());
    }
    return names;
  }

  private static final class RecordingSchemaResolver implements OgrInputMeta.SchemaResolver {
    private final List<OgrFieldDefinition> projectedFields;
    private Path lastFilePath;
    private String lastLayerName;
    private String lastSelectedAttributes;
    private Map<String, String> lastOpenOptions = Map.of();

    private RecordingSchemaResolver(List<OgrFieldDefinition> projectedFields) {
      this.projectedFields = projectedFields;
    }

    @Override
    public List<OgrFieldDefinition> resolveProjectedFields(
        Path filePath, String requestedLayerName, String selectedAttributes, Map<String, String> openOptions) {
      this.lastFilePath = filePath;
      this.lastLayerName = requestedLayerName;
      this.lastSelectedAttributes = selectedAttributes;
      this.lastOpenOptions = Map.copyOf(openOptions);
      return projectedFields;
    }
  }
}
