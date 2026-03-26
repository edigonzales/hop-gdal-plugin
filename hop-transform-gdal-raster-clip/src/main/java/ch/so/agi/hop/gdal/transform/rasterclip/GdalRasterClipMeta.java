package ch.so.agi.hop.gdal.transform.rasterclip;

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
    id = "GDAL_RASTER_CLIP_TRANSFORM",
    name = "i18n::GdalRasterClipMeta.Name",
    description = "i18n::GdalRasterClipMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterclip/icons/raster-clip.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-clip.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "clip", "crop", "gdal"})
public class GdalRasterClipMeta
    extends AbstractGdalRasterMeta<GdalRasterClipTransform, GdalRasterClipData> {

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
  @HopMetadataProperty private String clipMode;
  @HopMetadataProperty private String bounds;
  @HopMetadataProperty private String pixelWindow;
  @HopMetadataProperty private String inlineGeometry;
  @HopMetadataProperty private String templateSourceMode;
  @HopMetadataProperty private String templateDatasetValue;
  @HopMetadataProperty private String templateLayerName;
  @HopMetadataProperty private boolean addAlpha;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String additionalClipArgs;

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
    clipMode = "BOUNDING_BOX";
    bounds = "";
    pixelWindow = "";
    inlineGeometry = "";
    templateSourceMode = "LOCAL_FILE";
    templateDatasetValue = "";
    templateLayerName = "";
    addAlpha = false;
    creationOptions = "";
    additionalClipArgs = "";
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
      validateClipMode();
      CreationOptionParser.parse(creationOptions);
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalClipArgs);
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_OK, "Raster clip configuration looks valid", transformMeta));
  }

  private void validateClipMode() {
    String normalizedMode = clipMode == null ? "" : clipMode.trim().toUpperCase();
    switch (normalizedMode) {
      case "BOUNDING_BOX" -> BoundsSpec.parse(bounds);
      case "PIXEL_WINDOW" -> {
        if (pixelWindow == null || pixelWindow.isBlank()) {
          throw new IllegalArgumentException("Pixel window is required");
        }
        String[] parts = pixelWindow.split("[,;]");
        if (parts.length != 4) {
          throw new IllegalArgumentException("Pixel window must contain four values: xoff,yoff,xsize,ysize");
        }
      }
      case "INLINE_GEOMETRY" -> {
        if (inlineGeometry == null || inlineGeometry.isBlank()) {
          throw new IllegalArgumentException("Inline geometry is required");
        }
      }
      case "TEMPLATE_DATASET" -> {
        if (templateDatasetValue == null || templateDatasetValue.isBlank()) {
          throw new IllegalArgumentException("Template dataset is required");
        }
      }
      default -> throw new IllegalArgumentException("Unsupported clip mode: " + clipMode);
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
  public String getClipMode() { return clipMode; }
  public void setClipMode(String clipMode) { this.clipMode = clipMode; }
  public String getBounds() { return bounds; }
  public void setBounds(String bounds) { this.bounds = bounds; }
  public String getPixelWindow() { return pixelWindow; }
  public void setPixelWindow(String pixelWindow) { this.pixelWindow = pixelWindow; }
  public String getInlineGeometry() { return inlineGeometry; }
  public void setInlineGeometry(String inlineGeometry) { this.inlineGeometry = inlineGeometry; }
  public String getTemplateSourceMode() { return templateSourceMode; }
  public void setTemplateSourceMode(String templateSourceMode) { this.templateSourceMode = templateSourceMode; }
  public String getTemplateDatasetValue() { return templateDatasetValue; }
  public void setTemplateDatasetValue(String templateDatasetValue) { this.templateDatasetValue = templateDatasetValue; }
  public String getTemplateLayerName() { return templateLayerName; }
  public void setTemplateLayerName(String templateLayerName) { this.templateLayerName = templateLayerName; }
  public boolean isAddAlpha() { return addAlpha; }
  public void setAddAlpha(boolean addAlpha) { this.addAlpha = addAlpha; }
  public String getCreationOptions() { return creationOptions; }
  public void setCreationOptions(String creationOptions) { this.creationOptions = creationOptions; }
  public String getAdditionalClipArgs() { return additionalClipArgs; }
  public void setAdditionalClipArgs(String additionalClipArgs) { this.additionalClipArgs = additionalClipArgs; }
}
