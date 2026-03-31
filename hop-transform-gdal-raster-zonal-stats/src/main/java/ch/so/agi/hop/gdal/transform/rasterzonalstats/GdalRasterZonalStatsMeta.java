package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterMeta;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.RasterOutputOptionsSupport;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import java.util.List;
import org.apache.hop.core.CheckResult;
import org.apache.hop.core.ICheckResult;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

@Transform(
    id = "GDAL_RASTER_ZONAL_STATS_TRANSFORM",
    name = "i18n::GdalRasterZonalStatsMeta.Name",
    description = "i18n::GdalRasterZonalStatsMeta.Description",
    image = "ch/so/agi/hop/gdal/transform/rasterzonalstats/icons/raster-zonal-stats.svg",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Transform",
    documentationUrl = "/pipeline/transforms/gdal-raster-zonal-stats.html",
    classLoaderGroup = "hop-gdal-suite",
    keywords = {"raster", "zonal", "stats", "mean", "gdal"})
public class GdalRasterZonalStatsMeta
    extends AbstractGdalRasterMeta<GdalRasterZonalStatsTransform, GdalRasterZonalStatsData> {
  static final String OUTPUT_MODE_VECTOR_DATASET = "VECTOR_DATASET";
  static final String OUTPUT_MODE_ROW_FIELDS = "ROW_FIELDS";
  static final String ZONES_INPUT_MODE_DATASET_LAYER = "DATASET_LAYER";
  static final String ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD = "HOP_GEOMETRY_FIELD";

  @HopMetadataProperty private String inputSourceMode;
  @HopMetadataProperty private String inputValueMode;
  @HopMetadataProperty private String inputValue;
  @HopMetadataProperty private String inputField;
  @HopMetadataProperty private String outputMode;
  @HopMetadataProperty private String zonesInputMode;
  @HopMetadataProperty private String zonesSourceMode;
  @HopMetadataProperty private String zonesValueMode;
  @HopMetadataProperty private String zonesValue;
  @HopMetadataProperty private String zonesField;
  @HopMetadataProperty private String zonesLayer;
  @HopMetadataProperty private String geometryField;
  @HopMetadataProperty private String outputSourceMode;
  @HopMetadataProperty private String outputValueMode;
  @HopMetadataProperty private String outputValue;
  @HopMetadataProperty private String outputField;
  @HopMetadataProperty private String outputFormat;
  @HopMetadataProperty private String outputLayer;
  @HopMetadataProperty private String outputWriteMode;
  @HopMetadataProperty private String stats;
  @HopMetadataProperty private String bands;
  @HopMetadataProperty private String includeFields;
  @HopMetadataProperty private String pixelInclusion;
  @HopMetadataProperty private String strategy;
  @HopMetadataProperty private String creationOptions;
  @HopMetadataProperty private String layerCreationOptions;
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
    inputSourceMode = "LOCAL_FILE";
    inputValueMode = "CONSTANT";
    inputValue = "";
    inputField = "";
    outputMode = OUTPUT_MODE_VECTOR_DATASET;
    zonesInputMode = ZONES_INPUT_MODE_DATASET_LAYER;
    zonesSourceMode = "LOCAL_FILE";
    zonesValueMode = "CONSTANT";
    zonesValue = "";
    zonesField = "";
    zonesLayer = "";
    geometryField = "";
    outputSourceMode = "LOCAL_FILE";
    outputValueMode = "CONSTANT";
    outputValue = "";
    outputField = "";
    outputFormat = "GPKG";
    outputLayer = "stats";
    outputWriteMode = RasterOutputOptionsSupport.WRITE_MODE_FAIL_IF_EXISTS;
    stats = GdalRasterZonalStatsOptions.DEFAULT_STAT;
    bands = "";
    includeFields = "";
    pixelInclusion = GdalRasterZonalStatsOptions.DEFAULT_PIXEL_MODE;
    strategy = GdalRasterZonalStatsOptions.DEFAULT_STRATEGY;
    creationOptions = "";
    layerCreationOptions = "";
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
    if ("FIELD".equalsIgnoreCase(inputValueMode) && (inputField == null || inputField.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input field is required", transformMeta));
      return;
    }
    if (!"FIELD".equalsIgnoreCase(inputValueMode) && (inputValue == null || inputValue.isBlank())) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Input raster is required", transformMeta));
      return;
    }

    if (isRowFieldsOutputMode()) {
      if (!isHopGeometryZonesMode()) {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                "Row output mode only supports Hop geometry field zones",
                transformMeta));
        return;
      }
      if (geometryField == null || geometryField.isBlank()) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Geometry field is required", transformMeta));
        return;
      }
    } else {
      if (isHopGeometryZonesMode()) {
        remarks.add(
            new CheckResult(
                ICheckResult.TYPE_RESULT_ERROR,
                "Hop geometry field zones are only supported with Row output mode",
                transformMeta));
        return;
      }
      if ("FIELD".equalsIgnoreCase(zonesValueMode) && (zonesField == null || zonesField.isBlank())) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Zones field is required", transformMeta));
        return;
      }
      if (!"FIELD".equalsIgnoreCase(zonesValueMode) && (zonesValue == null || zonesValue.isBlank())) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Zones dataset is required", transformMeta));
        return;
      }
      if ("FIELD".equalsIgnoreCase(outputValueMode) && (outputField == null || outputField.isBlank())) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output field is required", transformMeta));
        return;
      }
      if (!"FIELD".equalsIgnoreCase(outputValueMode) && (outputValue == null || outputValue.isBlank())) {
        remarks.add(
            new CheckResult(ICheckResult.TYPE_RESULT_ERROR, "Output vector dataset is required", transformMeta));
        return;
      }
      try {
        RasterTransformSupport.validateOutputSourceMode(outputSourceMode);
      } catch (IllegalArgumentException e) {
        remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
        return;
      }
    }

    try {
      validateReferencedFields(prev);
      GdalRasterZonalStatsOptions.parseStats(stats);
      GdalRasterZonalStatsOptions.parseBands(bands);
      GdalRasterZonalStatsOptions.parseFields(includeFields);
      GdalRasterZonalStatsOptions.normalizePixels(pixelInclusion);
      GdalRasterZonalStatsOptions.normalizeStrategy(strategy);
      if (!isRowFieldsOutputMode()) {
        RasterOutputOptionsSupport.validateWriteMode(
            outputWriteMode,
            RasterOutputOptionsSupport.vectorAlgorithmWriteModes(),
            "Raster zonal stats");
        CreationOptionParser.parse(creationOptions);
        CreationOptionParser.parse(layerCreationOptions);
      }
      CreationOptionParser.parseKeyValueMap(gdalConfigOptions);
      AdditionalArgsParser.parse(additionalArgs);
      if (isRowFieldsOutputMode() && prev != null) {
        GdalRasterZonalStatsRowFields.resolveZoneAttributeNames(prev, geometryField, includeFields);
        validateNoRowFieldCollisions(prev);
      }
    } catch (IllegalArgumentException e) {
      remarks.add(new CheckResult(ICheckResult.TYPE_RESULT_ERROR, e.getMessage(), transformMeta));
      return;
    }

    remarks.add(
        new CheckResult(
            ICheckResult.TYPE_RESULT_OK,
            "Raster zonal stats configuration looks valid",
            transformMeta));
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
    if (isRowFieldsOutputMode()) {
      GdalRasterZonalStatsRowFields.renameUpstreamTechnicalValueMetas(rowMeta);
      GdalRasterZonalStatsRowFields.appendValueMetas(rowMeta, describeRowFields());
      if (isAddResultFields()) {
        GdalRasterZonalStatsRowFields.appendZonalStatsTechnicalValueMetas(rowMeta);
      }
      return;
    }
    super.getFields(rowMeta, origin, info, nextTransform, variables, metadataProvider);
  }

  List<GdalRasterZonalStatsRowFields.RowFieldSpec> describeRowFields() {
    return GdalRasterZonalStatsRowFields.describe(stats, bands);
  }

  private void validateNoRowFieldCollisions(IRowMeta prev) {
    try {
      RowMeta outputPreview = (RowMeta) prev.clone();
      GdalRasterZonalStatsRowFields.renameUpstreamTechnicalValueMetas(outputPreview);
      GdalRasterZonalStatsRowFields.appendValueMetas(outputPreview, describeRowFields());
      if (isAddResultFields()) {
        GdalRasterZonalStatsRowFields.appendZonalStatsTechnicalValueMetas(outputPreview);
      }
    } catch (HopTransformException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  private void validateReferencedFields(IRowMeta prev) {
    if (prev == null) {
      return;
    }

    if ("FIELD".equalsIgnoreCase(inputValueMode) && prev.indexOfValue(inputField) < 0) {
      throw new IllegalArgumentException("Input field was not found: " + inputField);
    }

    if (isRowFieldsOutputMode()) {
      if (prev.indexOfValue(geometryField) < 0) {
        throw new IllegalArgumentException("Geometry field was not found: " + geometryField);
      }
      return;
    }

    if ("FIELD".equalsIgnoreCase(zonesValueMode) && prev.indexOfValue(zonesField) < 0) {
      throw new IllegalArgumentException("Zones field was not found: " + zonesField);
    }
    if ("FIELD".equalsIgnoreCase(outputValueMode) && prev.indexOfValue(outputField) < 0) {
      throw new IllegalArgumentException("Output field was not found: " + outputField);
    }
  }

  public boolean isRowFieldsOutputMode() {
    return OUTPUT_MODE_ROW_FIELDS.equalsIgnoreCase(outputMode);
  }

  public boolean isHopGeometryZonesMode() {
    return ZONES_INPUT_MODE_HOP_GEOMETRY_FIELD.equalsIgnoreCase(zonesInputMode);
  }

  public String getInputSourceMode() { return inputSourceMode; }
  public void setInputSourceMode(String inputSourceMode) { this.inputSourceMode = inputSourceMode; }
  public String getInputValueMode() { return inputValueMode; }
  public void setInputValueMode(String inputValueMode) { this.inputValueMode = inputValueMode; }
  public String getInputValue() { return inputValue; }
  public void setInputValue(String inputValue) { this.inputValue = inputValue; }
  public String getInputField() { return inputField; }
  public void setInputField(String inputField) { this.inputField = inputField; }
  public String getOutputMode() { return outputMode; }
  public void setOutputMode(String outputMode) { this.outputMode = outputMode; }
  public String getZonesInputMode() { return zonesInputMode; }
  public void setZonesInputMode(String zonesInputMode) { this.zonesInputMode = zonesInputMode; }
  public String getZonesSourceMode() { return zonesSourceMode; }
  public void setZonesSourceMode(String zonesSourceMode) { this.zonesSourceMode = zonesSourceMode; }
  public String getZonesValueMode() { return zonesValueMode; }
  public void setZonesValueMode(String zonesValueMode) { this.zonesValueMode = zonesValueMode; }
  public String getZonesValue() { return zonesValue; }
  public void setZonesValue(String zonesValue) { this.zonesValue = zonesValue; }
  public String getZonesField() { return zonesField; }
  public void setZonesField(String zonesField) { this.zonesField = zonesField; }
  public String getZonesLayer() { return zonesLayer; }
  public void setZonesLayer(String zonesLayer) { this.zonesLayer = zonesLayer; }
  public String getGeometryField() { return geometryField; }
  public void setGeometryField(String geometryField) { this.geometryField = geometryField; }
  public String getOutputSourceMode() { return outputSourceMode; }
  public void setOutputSourceMode(String outputSourceMode) { this.outputSourceMode = outputSourceMode; }
  public String getOutputValueMode() { return outputValueMode; }
  public void setOutputValueMode(String outputValueMode) { this.outputValueMode = outputValueMode; }
  public String getOutputValue() { return outputValue; }
  public void setOutputValue(String outputValue) { this.outputValue = outputValue; }
  public String getOutputField() { return outputField; }
  public void setOutputField(String outputField) { this.outputField = outputField; }
  public String getOutputFormat() { return outputFormat; }
  public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
  public String getOutputLayer() { return outputLayer; }
  public void setOutputLayer(String outputLayer) { this.outputLayer = outputLayer; }
  public String getOutputWriteMode() { return outputWriteMode; }
  public void setOutputWriteMode(String outputWriteMode) {
    this.outputWriteMode = RasterOutputOptionsSupport.normalizeConfiguredWriteMode(outputWriteMode);
  }
  public String getStats() { return stats; }
  public void setStats(String stats) { this.stats = stats; }
  public String getBands() { return bands; }
  public void setBands(String bands) { this.bands = bands; }
  public String getIncludeFields() { return includeFields; }
  public void setIncludeFields(String includeFields) { this.includeFields = includeFields; }
  public String getPixelInclusion() { return pixelInclusion; }
  public void setPixelInclusion(String pixelInclusion) { this.pixelInclusion = pixelInclusion; }
  public String getStrategy() { return strategy; }
  public void setStrategy(String strategy) { this.strategy = strategy; }
  public String getCreationOptions() { return creationOptions; }
  public void setCreationOptions(String creationOptions) { this.creationOptions = creationOptions; }
  public String getLayerCreationOptions() { return layerCreationOptions; }
  public void setLayerCreationOptions(String layerCreationOptions) { this.layerCreationOptions = layerCreationOptions; }
  public String getAdditionalArgs() { return additionalArgs; }
  public void setAdditionalArgs(String additionalArgs) { this.additionalArgs = additionalArgs; }
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
}
