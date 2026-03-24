package ch.so.agi.hop.gdal.transform.rasterinfo;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
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
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "GDAL_RASTER_INFO_TRANSFORM",
    name = "i18n::GdalRasterInfoMeta.Name",
    description = "i18n::GdalRasterInfoMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterinfo/icons/raster-info.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-info.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "gdal", "info"})
public class GdalRasterInfoMeta
    extends AbstractGdalRasterMeta<GdalRasterInfoTransform, GdalRasterInfoData> {

  private static final Class<?> PKG = GdalRasterInfoMeta.class;

  @HopMetadataProperty private String inputSourceMode;
  @HopMetadataProperty private String inputValueMode;
  @HopMetadataProperty private String inputValue;
  @HopMetadataProperty private String inputField;
  @HopMetadataProperty private String authType;
  @HopMetadataProperty private String authUsername;
  @HopMetadataProperty private String authPassword;
  @HopMetadataProperty private String bearerToken;
  @HopMetadataProperty private String customHeaderName;
  @HopMetadataProperty private String customHeaderValue;
  @HopMetadataProperty private String gdalConfigOptions;
  @HopMetadataProperty private String additionalInfoArgs;

  @Override
  public void setDefault() {
    inputSourceMode = "LOCAL_FILE";
    inputValueMode = "CONSTANT";
    inputValue = "";
    inputField = "";
    authType = "NONE";
    authUsername = "";
    authPassword = "";
    bearerToken = "";
    customHeaderName = "";
    customHeaderValue = "";
    gdalConfigOptions = "";
    additionalInfoArgs = "";
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
    if (inputValueMode == null || inputValueMode.isBlank()) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input value mode is required", transformMeta));
      return;
    }
    if ("FIELD".equalsIgnoreCase(inputValueMode)) {
      if (inputField == null || inputField.isBlank()) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input field is required", transformMeta));
        return;
      }
    } else if (inputValue == null || inputValue.isBlank()) {
      remarks.add(
          new CheckResult(
              ICheckResult.TYPE_RESULT_ERROR,
              BaseMessages.getString(PKG, "GdalRasterInfoMeta.CheckResult.InputMissing"),
              transformMeta));
      return;
    }

    try {
      ch.so.agi.hop.gdal.raster.core.CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser.parse(additionalInfoArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(
            ICheckResult.TYPE_RESULT_OK,
            BaseMessages.getString(PKG, "GdalRasterInfoMeta.CheckResult.Ok"),
            transformMeta));
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

  public String getAdditionalInfoArgs() {
    return additionalInfoArgs;
  }

  public void setAdditionalInfoArgs(String additionalInfoArgs) {
    this.additionalInfoArgs = additionalInfoArgs;
  }
}
