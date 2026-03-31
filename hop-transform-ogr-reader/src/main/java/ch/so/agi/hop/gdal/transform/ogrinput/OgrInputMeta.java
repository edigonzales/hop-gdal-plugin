package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import com.atolcd.hop.core.row.value.ValueMetaGeometry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "OGR_INPUT_TRANSFORM",
    name = "i18n::OgrInputMeta.Name",
    description = "i18n::OgrInputMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/ogrinput/icons/ogr-input.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Input",
    documentationUrl = "/pipeline/transforms/ogr-input.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"i18n::OgrInputMeta.keyword", "ogr", "gdal", "vector"})
public class OgrInputMeta extends BaseTransformMeta<OgrInput, OgrInputData> {

  private static final Class<?> PKG = OgrInputMeta.class;
  private final SchemaResolver schemaResolver;

  @HopMetadataProperty private String fileName;
  @HopMetadataProperty private String layerName;

  @HopMetadataProperty private boolean includeFid = true;
  @HopMetadataProperty private String fidFieldName = "fid";
  @HopMetadataProperty private String geometryFieldName = "geometry";
  @HopMetadataProperty private String selectedAttributes;
  @HopMetadataProperty private String attributeFilter;
  @HopMetadataProperty private String bbox;
  @HopMetadataProperty private String polygonWkt;
  @HopMetadataProperty private String featureLimit;
  @HopMetadataProperty private String openOptions;

  public OgrInputMeta() {
    this(DefaultSchemaResolver.INSTANCE);
  }

  OgrInputMeta(SchemaResolver schemaResolver) {
    this.schemaResolver = Objects.requireNonNull(schemaResolver, "schemaResolver must not be null");
  }

  @Override
  public void setDefault() {
    fileName = "";
    layerName = "";
    includeFid = true;
    fidFieldName = "fid";
    geometryFieldName = "geometry";
    selectedAttributes = "";
    attributeFilter = "";
    bbox = "";
    polygonWkt = "";
    featureLimit = "";
    openOptions = "";
  }

  @Override
  public void getFields(
      IRowMeta rowMeta,
      String origin,
      IRowMeta[] info,
      TransformMeta nextTransform,
      IVariables variables,
      IHopMetadataProvider metadataProvider)
      throws HopTransformException {
    List<OgrFieldDefinition> projectedFields = resolveProjectedFieldsBestEffort(variables);
    if (includeFid) {
      rowMeta.addValueMeta(new ValueMetaInteger(resolveFieldName(fidFieldName, "fid")));
    }
    if (projectedFields == null) {
      for (String selectedAttribute : OgrInputOptionsUtil.splitCsvOrSemicolon(selectedAttributes)) {
        rowMeta.addValueMeta(new ValueMetaString(selectedAttribute));
      }
    } else {
      for (OgrFieldDefinition projectedField : projectedFields) {
        rowMeta.addValueMeta(OgrInput.toHopValueMeta(projectedField));
      }
    }
    rowMeta.addValueMeta(new ValueMetaGeometry(resolveFieldName(geometryFieldName, "geometry")));
  }

  @Override
  public void check(
      List<ICheckResult> remarks,
      PipelineMeta pipelineMeta,
      TransformMeta transformMeta,
      IRowMeta prev,
      String[] input,
      String[] output,
      IRowMeta info,
      IVariables variables,
      IHopMetadataProvider metadataProvider) {

    if (fileName == null || fileName.isBlank()) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrInputMeta.CheckResult.FileNameMissing"),
              transformMeta));
      return;
    }

    try {
      OgrInputOptionsUtil.validateSpatialFilterExclusivity(bbox, polygonWkt);
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrInputMeta.CheckResult.SpatialFilterConflict"),
              transformMeta));
      return;
    }

    try {
      if (OgrInputOptionsUtil.trimToNull(featureLimit) != null && !containsVariable(featureLimit)) {
        OgrInputOptionsUtil.parsePositiveLimit(featureLimit);
      }
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrInputMeta.CheckResult.InvalidFeatureLimit"),
              transformMeta));
      return;
    }

    try {
      if (OgrInputOptionsUtil.trimToNull(openOptions) != null && !containsVariable(openOptions)) {
        OgrInputOptionsUtil.parseKeyValueOptions(openOptions);
      }
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrInputMeta.CheckResult.InvalidOpenOptions"),
              transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(
            ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "OgrInputMeta.CheckResult.Ok"),
            transformMeta));
  }

  private String resolveFieldName(String fieldName, String defaultName) {
    return fieldName == null || fieldName.isBlank() ? defaultName : fieldName.trim();
  }

  private List<OgrFieldDefinition> resolveProjectedFieldsBestEffort(IVariables variables) {
    String resolvedFileName = resolveVariable(variables, fileName);
    if (resolvedFileName.isBlank()) {
      return null;
    }

    try {
      String resolvedLayerName = resolveVariable(variables, layerName);
      String resolvedSelectedAttributes = resolveVariable(variables, selectedAttributes);
      Map<String, String> resolvedOpenOptions =
          OgrInputOptionsUtil.parseKeyValueOptions(resolveVariable(variables, openOptions));
      return schemaResolver.resolveProjectedFields(
          Path.of(resolvedFileName),
          resolvedLayerName,
          resolvedSelectedAttributes,
          resolvedOpenOptions);
    } catch (Exception e) {
      if (isDebug()) {
        logDebug("Unable to probe OGR schema for design-time fields, using fallback metadata: " + e.getMessage());
      }
      return null;
    }
  }

  private String resolveVariable(IVariables variables, String value) {
    if (value == null) {
      return "";
    }
    return variables == null ? value.trim() : variables.resolve(value).trim();
  }

  private boolean containsVariable(String value) {
    return value != null && value.contains("${");
  }

  interface SchemaResolver {
    List<OgrFieldDefinition> resolveProjectedFields(
        Path filePath, String requestedLayerName, String selectedAttributes, Map<String, String> openOptions);
  }

  private static final class DefaultSchemaResolver implements SchemaResolver {
    private static final DefaultSchemaResolver INSTANCE = new DefaultSchemaResolver();

    @Override
    public List<OgrFieldDefinition> resolveProjectedFields(
        Path filePath, String requestedLayerName, String selectedAttributes, Map<String, String> openOptions) {
      List<OgrLayerDefinition> layers = OgrSchemaProbe.readLayers(filePath, openOptions);
      OgrLayerDefinition layerDefinition = OgrSchemaProbe.resolveLayer(layers, requestedLayerName);
      return OgrInput.selectProjectedFields(layerDefinition, selectedAttributes);
    }
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getLayerName() {
    return layerName;
  }

  public void setLayerName(String layerName) {
    this.layerName = layerName;
  }

  public boolean isIncludeFid() {
    return includeFid;
  }

  public void setIncludeFid(boolean includeFid) {
    this.includeFid = includeFid;
  }

  public String getFidFieldName() {
    return fidFieldName;
  }

  public void setFidFieldName(String fidFieldName) {
    this.fidFieldName = fidFieldName;
  }

  public String getGeometryFieldName() {
    return geometryFieldName;
  }

  public void setGeometryFieldName(String geometryFieldName) {
    this.geometryFieldName = geometryFieldName;
  }

  public String getSelectedAttributes() {
    return selectedAttributes;
  }

  public void setSelectedAttributes(String selectedAttributes) {
    this.selectedAttributes = selectedAttributes;
  }

  public String getAttributeFilter() {
    return attributeFilter;
  }

  public void setAttributeFilter(String attributeFilter) {
    this.attributeFilter = attributeFilter;
  }

  public String getBbox() {
    return bbox;
  }

  public void setBbox(String bbox) {
    this.bbox = bbox;
  }

  public String getPolygonWkt() {
    return polygonWkt;
  }

  public void setPolygonWkt(String polygonWkt) {
    this.polygonWkt = polygonWkt;
  }

  public String getFeatureLimit() {
    return featureLimit;
  }

  public void setFeatureLimit(String featureLimit) {
    this.featureLimit = featureLimit;
  }

  public String getOpenOptions() {
    return openOptions;
  }

  public void setOpenOptions(String openOptions) {
    this.openOptions = openOptions;
  }
}
