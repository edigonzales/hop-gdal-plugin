package ch.so.agi.hop.gdal.transform.rasterconvert;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
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
    id = "GDAL_RASTER_CONVERT_TRANSFORM",
    name = "i18n::GdalRasterConvertMeta.Name",
    description = "i18n::GdalRasterConvertMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterconvert/icons/raster-convert.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-convert.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "gdal", "translate", "convert"})
public class GdalRasterConvertMeta
    extends AbstractGdalRasterMeta<GdalRasterConvertTransform, GdalRasterConvertData> {

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
  @HopMetadataProperty private String bandSelection;
  @HopMetadataProperty private String outputDataType;
  @HopMetadataProperty private boolean scale;
  @HopMetadataProperty private boolean unscale;
  @HopMetadataProperty private String pixelWindow;
  @HopMetadataProperty private String coordinateWindow;
  @HopMetadataProperty private String outputNoData;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String additionalTranslateArgs;

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
    bandSelection = "";
    outputDataType = "";
    scale = false;
    unscale = false;
    pixelWindow = "";
    coordinateWindow = "";
    outputNoData = "";
    creationOptions = "";
    additionalTranslateArgs = "";
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
      CreationOptionParser.parse(creationOptions);
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalTranslateArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK, "Raster convert configuration looks valid", transformMeta));
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
  public String getBandSelection() { return bandSelection; }
  public void setBandSelection(String bandSelection) { this.bandSelection = bandSelection; }
  public String getOutputDataType() { return outputDataType; }
  public void setOutputDataType(String outputDataType) { this.outputDataType = outputDataType; }
  public boolean isScale() { return scale; }
  public void setScale(boolean scale) { this.scale = scale; }
  public boolean isUnscale() { return unscale; }
  public void setUnscale(boolean unscale) { this.unscale = unscale; }
  public String getPixelWindow() { return pixelWindow; }
  public void setPixelWindow(String pixelWindow) { this.pixelWindow = pixelWindow; }
  public String getCoordinateWindow() { return coordinateWindow; }
  public void setCoordinateWindow(String coordinateWindow) { this.coordinateWindow = coordinateWindow; }
  public String getOutputNoData() { return outputNoData; }
  public void setOutputNoData(String outputNoData) { this.outputNoData = outputNoData; }
  public String getCreationOptions() { return creationOptions; }
  public void setCreationOptions(String creationOptions) { this.creationOptions = creationOptions; }
  public String getAdditionalTranslateArgs() { return additionalTranslateArgs; }
  public void setAdditionalTranslateArgs(String additionalTranslateArgs) { this.additionalTranslateArgs = additionalTranslateArgs; }
}
