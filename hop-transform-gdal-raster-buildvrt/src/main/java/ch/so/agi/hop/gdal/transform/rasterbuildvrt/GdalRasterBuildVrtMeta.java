package ch.so.agi.hop.gdal.transform.rasterbuildvrt;

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
    id = "GDAL_RASTER_BUILDVRT_TRANSFORM",
    name = "i18n::GdalRasterBuildVrtMeta.Name",
    description = "i18n::GdalRasterBuildVrtMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterbuildvrt/icons/raster-buildvrt.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-buildvrt.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "gdal", "vrt", "mosaic"})
public class GdalRasterBuildVrtMeta
    extends AbstractGdalRasterMeta<GdalRasterBuildVrtTransform, GdalRasterBuildVrtData> {

  @HopMetadataProperty private String inputInterpretationMode;
  @HopMetadataProperty private String inputListValueMode;
  @HopMetadataProperty private String inputListValue;
  @HopMetadataProperty private String inputListField;
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
  @HopMetadataProperty private String resolutionStrategy;
  @HopMetadataProperty private String bounds;
  @HopMetadataProperty private boolean separateBands;
  @HopMetadataProperty private String srcNoData;
  @HopMetadataProperty private String vrtNoData;
  @HopMetadataProperty private boolean allowProjectionDifference;
  @HopMetadataProperty private String additionalArgs;

  @Override
  public void setDefault() {
    inputInterpretationMode = "LOCAL_FILE";
    inputListValueMode = "CONSTANT";
    inputListValue = "";
    inputListField = "";
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
    resolutionStrategy = "AVERAGE";
    bounds = "";
    separateBands = false;
    srcNoData = "";
    vrtNoData = "";
    allowProjectionDifference = false;
    additionalArgs = "";
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
    if ("FIELD".equalsIgnoreCase(inputListValueMode)
        && (inputListField == null || inputListField.isBlank())) {
      remarks.add(
          new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input raster list field is required", transformMeta));
      return;
    }
    if (!"FIELD".equalsIgnoreCase(inputListValueMode)
        && (inputListValue == null || inputListValue.isBlank())) {
      remarks.add(
          new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input raster list is required", transformMeta));
      return;
    }
    if ("FIELD".equalsIgnoreCase(outputValueMode)
        && (outputField == null || outputField.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output field is required", transformMeta));
      return;
    }
    if (!"FIELD".equalsIgnoreCase(outputValueMode)
        && (outputValue == null || outputValue.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output mosaic path is required", transformMeta));
      return;
    }
    try {
      RasterTransformSupport.validateOutputSourceMode(outputSourceMode);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    try {
      if (bounds != null && !bounds.isBlank()) {
        BoundsSpec.parse(bounds);
      }
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(ICheckResult.TYPE_RESULT_OK, "Raster mosaic configuration looks valid", transformMeta));
  }

  public String getInputInterpretationMode() {
    return inputInterpretationMode;
  }

  public void setInputInterpretationMode(String inputInterpretationMode) {
    this.inputInterpretationMode = inputInterpretationMode;
  }

  public String getInputListValueMode() {
    return inputListValueMode;
  }

  public void setInputListValueMode(String inputListValueMode) {
    this.inputListValueMode = inputListValueMode;
  }

  public String getInputListValue() {
    return inputListValue;
  }

  public void setInputListValue(String inputListValue) {
    this.inputListValue = inputListValue;
  }

  public String getInputListField() {
    return inputListField;
  }

  public void setInputListField(String inputListField) {
    this.inputListField = inputListField;
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

  public String getResolutionStrategy() {
    return resolutionStrategy;
  }

  public void setResolutionStrategy(String resolutionStrategy) {
    this.resolutionStrategy = resolutionStrategy;
  }

  public String getBounds() {
    return bounds;
  }

  public void setBounds(String bounds) {
    this.bounds = bounds;
  }

  public boolean isSeparateBands() {
    return separateBands;
  }

  public void setSeparateBands(boolean separateBands) {
    this.separateBands = separateBands;
  }

  public String getSrcNoData() {
    return srcNoData;
  }

  public void setSrcNoData(String srcNoData) {
    this.srcNoData = srcNoData;
  }

  public String getVrtNoData() {
    return vrtNoData;
  }

  public void setVrtNoData(String vrtNoData) {
    this.vrtNoData = vrtNoData;
  }

  public boolean isAllowProjectionDifference() {
    return allowProjectionDifference;
  }

  public void setAllowProjectionDifference(boolean allowProjectionDifference) {
    this.allowProjectionDifference = allowProjectionDifference;
  }

  public String getAdditionalArgs() {
    return additionalArgs;
  }

  public void setAdditionalArgs(String additionalArgs) {
    this.additionalArgs = additionalArgs;
  }
}
