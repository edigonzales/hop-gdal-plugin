package ch.so.agi.hop.gdal.transform.ogroutput;

import java.util.List;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "OGR_OUTPUT_TRANSFORM",
    name = "i18n::OgrOutputMeta.Name",
    description = "i18n::OgrOutputMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/ogroutput/icons/ogr-output.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Output",
    documentationUrl = "/pipeline/transforms/ogr-output.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"i18n::OgrOutputMeta.keyword", "ogr", "gdal", "vector", "export"})
public class OgrOutputMeta extends BaseTransformMeta<OgrOutput, OgrOutputData> {

  private static final Class<?> PKG = OgrOutputMeta.class;

  @HopMetadataProperty private String fileName;
  @HopMetadataProperty private String format;
  @HopMetadataProperty private String layerName;
  @HopMetadataProperty private String writeMode;
  @HopMetadataProperty private String geometryField;
  @HopMetadataProperty private String selectedAttributes;
  @HopMetadataProperty private String datasetCreationOptions;
  @HopMetadataProperty private String layerCreationOptions;
  @HopMetadataProperty private String forceGeometryType;

  @Override
  public void setDefault() {
    fileName = "";
    format = "";
    layerName = "";
    writeMode = "FAIL_IF_EXISTS";
    geometryField = "";
    selectedAttributes = "";
    datasetCreationOptions = "";
    layerCreationOptions = "";
    forceGeometryType = OgrOutputOptionsUtil.FORCE_GEOMETRY_AUTO;
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
    // sink transform: no output rows
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
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.FileNameMissing"),
              transformMeta));
      return;
    }

    if (format == null || format.isBlank()) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.FormatMissing"),
              transformMeta));
      return;
    }

    if (geometryField == null || geometryField.isBlank()) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.GeometryFieldMissing"),
              transformMeta));
      return;
    }

    try {
      OgrOutputOptionsUtil.parseWriteMode(writeMode);
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.InvalidWriteMode"),
              transformMeta));
      return;
    }

    try {
      if (!containsVariable(datasetCreationOptions)
          && OgrOutputOptionsUtil.trimToNull(datasetCreationOptions) != null) {
        OgrOutputOptionsUtil.parseKeyValueOptions(datasetCreationOptions);
      }
      if (!containsVariable(layerCreationOptions)
          && OgrOutputOptionsUtil.trimToNull(layerCreationOptions) != null) {
        OgrOutputOptionsUtil.parseKeyValueOptions(layerCreationOptions);
      }
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.InvalidCreateOptions"),
              transformMeta));
      return;
    }

    try {
      OgrOutputOptionsUtil.validateForceGeometryType(forceGeometryType);
    } catch (IllegalArgumentException e) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.InvalidForceGeometry"),
              transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(
            ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "OgrOutputMeta.CheckResult.Ok"),
            transformMeta));
  }

  private boolean containsVariable(String value) {
    return value != null && value.contains("${");
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getLayerName() {
    return layerName;
  }

  public void setLayerName(String layerName) {
    this.layerName = layerName;
  }

  public String getWriteMode() {
    return writeMode;
  }

  public void setWriteMode(String writeMode) {
    this.writeMode = writeMode;
  }

  public String getGeometryField() {
    return geometryField;
  }

  public void setGeometryField(String geometryField) {
    this.geometryField = geometryField;
  }

  public String getSelectedAttributes() {
    return selectedAttributes;
  }

  public void setSelectedAttributes(String selectedAttributes) {
    this.selectedAttributes = selectedAttributes;
  }

  public String getDatasetCreationOptions() {
    return datasetCreationOptions;
  }

  public void setDatasetCreationOptions(String datasetCreationOptions) {
    this.datasetCreationOptions = datasetCreationOptions;
  }

  public String getLayerCreationOptions() {
    return layerCreationOptions;
  }

  public void setLayerCreationOptions(String layerCreationOptions) {
    this.layerCreationOptions = layerCreationOptions;
  }

  public String getForceGeometryType() {
    return forceGeometryType;
  }

  public void setForceGeometryType(String forceGeometryType) {
    this.forceGeometryType = forceGeometryType;
  }
}
