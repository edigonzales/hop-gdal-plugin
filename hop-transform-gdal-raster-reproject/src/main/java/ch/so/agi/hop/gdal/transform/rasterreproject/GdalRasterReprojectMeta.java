package ch.so.agi.hop.gdal.transform.rasterreproject;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
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
    id = "GDAL_RASTER_REPROJECT_TRANSFORM",
    name = "i18n::GdalRasterReprojectMeta.Name",
    description = "i18n::GdalRasterReprojectMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterreproject/icons/raster-reproject.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-reproject.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "reproject", "warp", "gdal"})
public class GdalRasterReprojectMeta
    extends AbstractGdalRasterMeta<GdalRasterReprojectTransform, GdalRasterReprojectData> {

  @HopMetadataProperty private String inputSourceMode;
  @HopMetadataProperty private String inputValueMode;
  @HopMetadataProperty private String inputValue;
  @HopMetadataProperty private String inputField;
  @HopMetadataProperty private String outputSourceMode;
  @HopMetadataProperty private String outputValueMode;
  @HopMetadataProperty private String outputValue;
  @HopMetadataProperty private String outputField;
  @HopMetadataProperty private String authType;
  @HopMetadataProperty private String authUsername;
  @HopMetadataProperty private String authPassword;
  @HopMetadataProperty private String bearerToken;
  @HopMetadataProperty private String customHeaderName;
  @HopMetadataProperty private String customHeaderValue;
  @HopMetadataProperty private String gdalConfigOptions;
  @HopMetadataProperty private String outputFormat;
  @HopMetadataProperty private boolean overwrite;
  @HopMetadataProperty private String sourceCrs;
  @HopMetadataProperty private String targetCrs;
  @HopMetadataProperty private String sizingMode;
  @HopMetadataProperty private String resolutionX;
  @HopMetadataProperty private String resolutionY;
  @HopMetadataProperty private String width;
  @HopMetadataProperty private String height;
  @HopMetadataProperty private String bounds;
  @HopMetadataProperty private String resamplingMethod;
  @HopMetadataProperty private String sourceNoData;
  @HopMetadataProperty private String destinationNoData;
  @HopMetadataProperty private boolean targetAlignedPixels;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String additionalReprojectArgs;

  @Override
  public void setDefault() {
    inputSourceMode = "LOCAL_FILE";
    inputValueMode = "CONSTANT";
    inputValue = "";
    inputField = "";
    outputSourceMode = "LOCAL_FILE";
    outputValueMode = "CONSTANT";
    outputValue = "";
    outputField = "";
    authType = "NONE";
    authUsername = "";
    authPassword = "";
    bearerToken = "";
    customHeaderName = "";
    customHeaderValue = "";
    gdalConfigOptions = "";
    outputFormat = "GTiff";
    overwrite = false;
    sourceCrs = "";
    targetCrs = "";
    sizingMode = "NONE";
    resolutionX = "";
    resolutionY = "";
    width = "";
    height = "";
    bounds = "";
    resamplingMethod = "near";
    sourceNoData = "";
    destinationNoData = "";
    targetAlignedPixels = false;
    creationOptions = "";
    additionalReprojectArgs = "";
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
    if ("FIELD".equalsIgnoreCase(inputValueMode) && (inputField == null || inputField.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input field is required", transformMeta));
      return;
    }
    if (!"FIELD".equalsIgnoreCase(inputValueMode) && (inputValue == null || inputValue.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input raster is required", transformMeta));
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
      RasterTransformSupport.validateOutputSourceMode(outputSourceMode);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    try {
      if (targetCrs == null || targetCrs.isBlank()) {
        throw new IllegalArgumentException("Destination CRS is required");
      }
      if (bounds != null && !bounds.isBlank()) {
        BoundsSpec.parse(bounds);
      }
      validateSizingMode();
      CreationOptionParser.parse(creationOptions);
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalReprojectArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK, "Raster reproject configuration looks valid", transformMeta));
  }

  private void validateSizingMode() {
    String normalized = sizingMode == null ? "" : sizingMode.trim().toUpperCase();
    switch (normalized) {
      case "", "NONE" -> {
      }
      case "RESOLUTION" -> {
        if (resolutionX == null || resolutionX.isBlank() || resolutionY == null || resolutionY.isBlank()) {
          throw new IllegalArgumentException("Resolution X and Y are required");
        }
      }
      case "SIZE" -> {
        if (width == null || width.isBlank() || height == null || height.isBlank()) {
          throw new IllegalArgumentException("Width and height are required");
        }
      }
      default -> throw new IllegalArgumentException("Unsupported sizing mode: " + sizingMode);
    }
  }

  public String getInputSourceMode() { return inputSourceMode; }
  public void setInputSourceMode(String inputSourceMode) { this.inputSourceMode = inputSourceMode; }
  public String getInputValueMode() { return inputValueMode; }
  public void setInputValueMode(String inputValueMode) { this.inputValueMode = inputValueMode; }
  public String getInputValue() { return inputValue; }
  public void setInputValue(String inputValue) { this.inputValue = inputValue; }
  public String getInputField() { return inputField; }
  public void setInputField(String inputField) { this.inputField = inputField; }
  public String getOutputSourceMode() { return outputSourceMode; }
  public void setOutputSourceMode(String outputSourceMode) { this.outputSourceMode = outputSourceMode; }
  public String getOutputValueMode() { return outputValueMode; }
  public void setOutputValueMode(String outputValueMode) { this.outputValueMode = outputValueMode; }
  public String getOutputValue() { return outputValue; }
  public void setOutputValue(String outputValue) { this.outputValue = outputValue; }
  public String getOutputField() { return outputField; }
  public void setOutputField(String outputField) { this.outputField = outputField; }
  public String getAuthType() { return authType; }
  public void setAuthType(String authType) { this.authType = authType; }
  public String getAuthUsername() { return authUsername; }
  public void setAuthUsername(String authUsername) { this.authUsername = authUsername; }
  public String getAuthPassword() { return authPassword; }
  public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
  public String getBearerToken() { return bearerToken; }
  public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
  public String getCustomHeaderName() { return customHeaderName; }
  public void setCustomHeaderName(String customHeaderName) { this.customHeaderName = customHeaderName; }
  public String getCustomHeaderValue() { return customHeaderValue; }
  public void setCustomHeaderValue(String customHeaderValue) { this.customHeaderValue = customHeaderValue; }
  public String getGdalConfigOptions() { return gdalConfigOptions; }
  public void setGdalConfigOptions(String gdalConfigOptions) { this.gdalConfigOptions = gdalConfigOptions; }
  public String getOutputFormat() { return outputFormat; }
  public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
  public boolean isOverwrite() { return overwrite; }
  public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
  public String getSourceCrs() { return sourceCrs; }
  public void setSourceCrs(String sourceCrs) { this.sourceCrs = sourceCrs; }
  public String getTargetCrs() { return targetCrs; }
  public void setTargetCrs(String targetCrs) { this.targetCrs = targetCrs; }
  public String getSizingMode() { return sizingMode; }
  public void setSizingMode(String sizingMode) { this.sizingMode = sizingMode; }
  public String getResolutionX() { return resolutionX; }
  public void setResolutionX(String resolutionX) { this.resolutionX = resolutionX; }
  public String getResolutionY() { return resolutionY; }
  public void setResolutionY(String resolutionY) { this.resolutionY = resolutionY; }
  public String getWidth() { return width; }
  public void setWidth(String width) { this.width = width; }
  public String getHeight() { return height; }
  public void setHeight(String height) { this.height = height; }
  public String getBounds() { return bounds; }
  public void setBounds(String bounds) { this.bounds = bounds; }
  public String getResamplingMethod() { return resamplingMethod; }
  public void setResamplingMethod(String resamplingMethod) { this.resamplingMethod = resamplingMethod; }
  public String getSourceNoData() { return sourceNoData; }
  public void setSourceNoData(String sourceNoData) { this.sourceNoData = sourceNoData; }
  public String getDestinationNoData() { return destinationNoData; }
  public void setDestinationNoData(String destinationNoData) { this.destinationNoData = destinationNoData; }
  public boolean isTargetAlignedPixels() { return targetAlignedPixels; }
  public void setTargetAlignedPixels(boolean targetAlignedPixels) { this.targetAlignedPixels = targetAlignedPixels; }
  public String getCreationOptions() { return creationOptions; }
  public void setCreationOptions(String creationOptions) { this.creationOptions = creationOptions; }
  public String getAdditionalReprojectArgs() { return additionalReprojectArgs; }
  public void setAdditionalReprojectArgs(String additionalReprojectArgs) { this.additionalReprojectArgs = additionalReprojectArgs; }
}
