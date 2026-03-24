package ch.so.agi.hop.gdal.transform.rasterizevector;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import java.util.List;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "GDAL_RASTERIZE_VECTOR_TRANSFORM",
    name = "i18n::GdalRasterizeVectorMeta.Name",
    description = "i18n::GdalRasterizeVectorMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterizevector/icons/rasterize-vector.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-rasterize-vector.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "gdal", "rasterize", "vector"})
public class GdalRasterizeVectorMeta
    extends AbstractGdalRasterMeta<GdalRasterizeVectorTransform, GdalRasterizeVectorData> {

  @HopMetadataProperty private String vectorInputMode;
  @HopMetadataProperty private String inputSourceMode;
  @HopMetadataProperty private String inputValueMode;
  @HopMetadataProperty private String inputValue;
  @HopMetadataProperty private String inputField;
  @HopMetadataProperty private String layerName;
  @HopMetadataProperty private String geometryField;
  @HopMetadataProperty private String burnStrategy;
  @HopMetadataProperty private String burnValue;
  @HopMetadataProperty private String burnField;
  @HopMetadataProperty private String outputSourceMode;
  @HopMetadataProperty private String outputValueMode;
  @HopMetadataProperty private String outputValue;
  @HopMetadataProperty private String outputField;
  @HopMetadataProperty private String outputFormat;
  @HopMetadataProperty private String gridMode;
  @HopMetadataProperty private String bounds;
  @HopMetadataProperty private String crs;
  @HopMetadataProperty private String resolutionX;
  @HopMetadataProperty private String resolutionY;
  @HopMetadataProperty private String width;
  @HopMetadataProperty private String height;
  @HopMetadataProperty private String outputDataType;
  @HopMetadataProperty private String initValue;
  @HopMetadataProperty private String noDataValue;
  @HopMetadataProperty private boolean allTouched;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String additionalArgs;
  @HopMetadataProperty private String authType;
  @HopMetadataProperty private String authUsername;
  @HopMetadataProperty private String authPassword;
  @HopMetadataProperty private String bearerToken;
  @HopMetadataProperty private String customHeaderName;
  @HopMetadataProperty private String customHeaderValue;
  @HopMetadataProperty private String gdalConfigOptions;

  @Override
  public void setDefault() {
    vectorInputMode = "DATASET_LAYER";
    inputSourceMode = "LOCAL_FILE";
    inputValueMode = "CONSTANT";
    inputValue = "";
    inputField = "";
    layerName = "";
    geometryField = "";
    burnStrategy = "CONSTANT_VALUE";
    burnValue = "1";
    burnField = "";
    outputSourceMode = "LOCAL_FILE";
    outputValueMode = "CONSTANT";
    outputValue = "";
    outputField = "";
    outputFormat = "GTiff";
    gridMode = "BOUNDS_RESOLUTION";
    bounds = "";
    crs = "";
    resolutionX = "";
    resolutionY = "";
    width = "";
    height = "";
    outputDataType = "";
    initValue = "";
    noDataValue = "";
    allTouched = false;
    creationOptions = "";
    additionalArgs = "";
    authType = "NONE";
    authUsername = "";
    authPassword = "";
    bearerToken = "";
    customHeaderName = "";
    customHeaderValue = "";
    gdalConfigOptions = "";
    setFailOnError(true);
    setAddResultFields(true);
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
    if ("HOP_GEOMETRY_FIELD".equalsIgnoreCase(vectorInputMode)) {
      if (geometryField == null || geometryField.isBlank()) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Geometry field is required", transformMeta));
        return;
      }
    } else {
      if ("FIELD".equalsIgnoreCase(inputValueMode) && (inputField == null || inputField.isBlank())) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input field is required", transformMeta));
        return;
      }
      if (!"FIELD".equalsIgnoreCase(inputValueMode) && (inputValue == null || inputValue.isBlank())) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input vector dataset is required", transformMeta));
        return;
      }
    }
    if ("ATTRIBUTE_FIELD".equalsIgnoreCase(burnStrategy)
        && (burnField == null || burnField.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Burn attribute field is required", transformMeta));
      return;
    }
    if (!"ATTRIBUTE_FIELD".equalsIgnoreCase(burnStrategy)
        && (burnValue == null || burnValue.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Burn value is required", transformMeta));
      return;
    }
    if ("FIELD".equalsIgnoreCase(outputValueMode) && (outputField == null || outputField.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output field is required", transformMeta));
      return;
    }
    if (!"FIELD".equalsIgnoreCase(outputValueMode) && (outputValue == null || outputValue.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output raster is required", transformMeta));
      return;
    }

    try {
      BoundsSpec.parse(bounds);
      CreationOptionParser.parse(creationOptions);
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(ICheckResult.TYPE_RESULT_OK, "Rasterize vector configuration looks valid", transformMeta));
  }

  public String getVectorInputMode() {
    return vectorInputMode;
  }

  public void setVectorInputMode(String vectorInputMode) {
    this.vectorInputMode = vectorInputMode;
  }

  public String getInputSourceMode() {
    return inputSourceMode;
  }

  public void setInputSourceMode(String inputSourceMode) {
    this.inputSourceMode = inputSourceMode;
  }

  public String getInputValueMode() {
    return inputValueMode;
  }

  public void setInputValueMode(String inputValueMode) {
    this.inputValueMode = inputValueMode;
  }

  public String getInputValue() {
    return inputValue;
  }

  public void setInputValue(String inputValue) {
    this.inputValue = inputValue;
  }

  public String getInputField() {
    return inputField;
  }

  public void setInputField(String inputField) {
    this.inputField = inputField;
  }

  public String getLayerName() {
    return layerName;
  }

  public void setLayerName(String layerName) {
    this.layerName = layerName;
  }

  public String getGeometryField() {
    return geometryField;
  }

  public void setGeometryField(String geometryField) {
    this.geometryField = geometryField;
  }

  public String getBurnStrategy() {
    return burnStrategy;
  }

  public void setBurnStrategy(String burnStrategy) {
    this.burnStrategy = burnStrategy;
  }

  public String getBurnValue() {
    return burnValue;
  }

  public void setBurnValue(String burnValue) {
    this.burnValue = burnValue;
  }

  public String getBurnField() {
    return burnField;
  }

  public void setBurnField(String burnField) {
    this.burnField = burnField;
  }

  public String getOutputSourceMode() {
    return outputSourceMode;
  }

  public void setOutputSourceMode(String outputSourceMode) {
    this.outputSourceMode = outputSourceMode;
  }

  public String getOutputValueMode() {
    return outputValueMode;
  }

  public void setOutputValueMode(String outputValueMode) {
    this.outputValueMode = outputValueMode;
  }

  public String getOutputValue() {
    return outputValue;
  }

  public void setOutputValue(String outputValue) {
    this.outputValue = outputValue;
  }

  public String getOutputField() {
    return outputField;
  }

  public void setOutputField(String outputField) {
    this.outputField = outputField;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getGridMode() {
    return gridMode;
  }

  public void setGridMode(String gridMode) {
    this.gridMode = gridMode;
  }

  public String getBounds() {
    return bounds;
  }

  public void setBounds(String bounds) {
    this.bounds = bounds;
  }

  public String getCrs() {
    return crs;
  }

  public void setCrs(String crs) {
    this.crs = crs;
  }

  public String getResolutionX() {
    return resolutionX;
  }

  public void setResolutionX(String resolutionX) {
    this.resolutionX = resolutionX;
  }

  public String getResolutionY() {
    return resolutionY;
  }

  public void setResolutionY(String resolutionY) {
    this.resolutionY = resolutionY;
  }

  public String getWidth() {
    return width;
  }

  public void setWidth(String width) {
    this.width = width;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getOutputDataType() {
    return outputDataType;
  }

  public void setOutputDataType(String outputDataType) {
    this.outputDataType = outputDataType;
  }

  public String getInitValue() {
    return initValue;
  }

  public void setInitValue(String initValue) {
    this.initValue = initValue;
  }

  public String getNoDataValue() {
    return noDataValue;
  }

  public void setNoDataValue(String noDataValue) {
    this.noDataValue = noDataValue;
  }

  public boolean isAllTouched() {
    return allTouched;
  }

  public void setAllTouched(boolean allTouched) {
    this.allTouched = allTouched;
  }

  public String getCreationOptions() {
    return creationOptions;
  }

  public void setCreationOptions(String creationOptions) {
    this.creationOptions = creationOptions;
  }

  public String getAdditionalArgs() {
    return additionalArgs;
  }

  public void setAdditionalArgs(String additionalArgs) {
    this.additionalArgs = additionalArgs;
  }

  public String getAuthType() {
    return authType;
  }

  public void setAuthType(String authType) {
    this.authType = authType;
  }

  public String getAuthUsername() {
    return authUsername;
  }

  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }

  public String getAuthPassword() {
    return authPassword;
  }

  public void setAuthPassword(String authPassword) {
    this.authPassword = authPassword;
  }

  public String getBearerToken() {
    return bearerToken;
  }

  public void setBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
  }

  public String getCustomHeaderName() {
    return customHeaderName;
  }

  public void setCustomHeaderName(String customHeaderName) {
    this.customHeaderName = customHeaderName;
  }

  public String getCustomHeaderValue() {
    return customHeaderValue;
  }

  public void setCustomHeaderValue(String customHeaderValue) {
    this.customHeaderValue = customHeaderValue;
  }

  public String getGdalConfigOptions() {
    return gdalConfigOptions;
  }

  public void setGdalConfigOptions(String gdalConfigOptions) {
    this.gdalConfigOptions = gdalConfigOptions;
  }
}
