package ch.so.agi.hop.gdal.transform.rasterresize;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
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
    id = "GDAL_RASTER_RESIZE_TRANSFORM",
    name = "i18n::GdalRasterResizeMeta.Name",
    description = "i18n::GdalRasterResizeMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterresize/icons/raster-resize.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-resize.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "resize", "resample", "gdal"})
public class GdalRasterResizeMeta
    extends AbstractGdalRasterMeta<GdalRasterResizeTransform, GdalRasterResizeData> {

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
  @HopMetadataProperty private String sizingMode;
  @HopMetadataProperty private String resolutionX;
  @HopMetadataProperty private String resolutionY;
  @HopMetadataProperty private String width;
  @HopMetadataProperty private String height;
  @HopMetadataProperty private String resamplingMethod;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String additionalResizeArgs;

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
    sizingMode = "SIZE";
    resolutionX = "";
    resolutionY = "";
    width = "";
    height = "";
    resamplingMethod = "near";
    creationOptions = "";
    additionalResizeArgs = "";
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
      validateSizingMode();
      CreationOptionParser.parse(creationOptions);
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalResizeArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK, "Raster resize configuration looks valid", transformMeta));
  }

  private void validateSizingMode() {
    String normalized = sizingMode == null ? "" : sizingMode.trim().toUpperCase();
    switch (normalized) {
      case "SIZE" -> {
        if (width == null || width.isBlank() || height == null || height.isBlank()) {
          throw new IllegalArgumentException("Width and height are required");
        }
      }
      case "RESOLUTION" -> {
        if (resolutionX == null || resolutionX.isBlank() || resolutionY == null || resolutionY.isBlank()) {
          throw new IllegalArgumentException("Resolution X and Y are required");
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
  public String getResamplingMethod() { return resamplingMethod; }
  public void setResamplingMethod(String resamplingMethod) { this.resamplingMethod = resamplingMethod; }
  public String getCreationOptions() { return creationOptions; }
  public void setCreationOptions(String creationOptions) { this.creationOptions = creationOptions; }
  public String getAdditionalResizeArgs() { return additionalResizeArgs; }
  public void setAdditionalResizeArgs(String additionalResizeArgs) { this.additionalResizeArgs = additionalResizeArgs; }
}
