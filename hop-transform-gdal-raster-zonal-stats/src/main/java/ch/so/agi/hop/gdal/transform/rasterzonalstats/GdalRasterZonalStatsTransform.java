package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.gdal.ffm.GdalConfig;
import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.DatasetRefType;
import ch.so.agi.hop.gdal.raster.core.HopGeometrySupport;
import ch.so.agi.hop.gdal.raster.core.RasterGdalClient;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import ch.so.agi.hop.gdal.raster.core.VsiVectorDatasetSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.locationtech.jts.geom.Geometry;

public class GdalRasterZonalStatsTransform
    extends AbstractGdalRasterTransform<GdalRasterZonalStatsMeta, GdalRasterZonalStatsData> {
  private final GdalRasterZonalStatsCommandBuilder commandBuilder;
  private final RowZoneDatasetFactory rowZoneDatasetFactory;
  private final RowStatsReader rowStatsReader;

  public GdalRasterZonalStatsTransform(
      TransformMeta transformMeta,
      GdalRasterZonalStatsMeta meta,
      GdalRasterZonalStatsData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    this(
        transformMeta,
        meta,
        data,
        copyNr,
        pipelineMeta,
        pipeline,
        null,
        new OgrZonesMetadataInspector(),
        DefaultRowZoneDatasetFactory.INSTANCE,
        DefaultRowStatsReader.INSTANCE);
  }

  GdalRasterZonalStatsTransform(
      TransformMeta transformMeta,
      GdalRasterZonalStatsMeta meta,
      GdalRasterZonalStatsData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline,
      RasterGdalClient gdalClient,
      ZonesMetadataInspector zonesMetadataInspector,
      RowZoneDatasetFactory rowZoneDatasetFactory,
      RowStatsReader rowStatsReader) {
    super(
        transformMeta,
        meta,
        data,
        copyNr,
        pipelineMeta,
        pipeline,
        gdalClient == null ? new ch.so.agi.hop.gdal.raster.core.DefaultRasterGdalClient() : gdalClient);
    this.commandBuilder = new GdalRasterZonalStatsCommandBuilder(zonesMetadataInspector);
    this.rowZoneDatasetFactory = rowZoneDatasetFactory;
    this.rowStatsReader = rowStatsReader;
  }

  @Override
  public boolean processRow() throws HopException {
    if (!meta.isRowFieldsOutputMode()) {
      return super.processRow();
    }
    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(this::processRowAsFieldsInternal);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();
    GdalRasterZonalStatsCommandBuilder.BuildRequest request =
        commandBuilder.build(meta, row, getInputRowMeta(), this::resolveConstant);
    gdalClient()
        .rasterZonalStats(
            request.input(), request.zones(), request.output(), request.remoteAccess(), request.args());
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, request.input().value(), request.output().value(), "{}");
  }

  protected DatasetRef createRowModeOutputDatasetRef() {
    return new DatasetRef(
        DatasetRefType.GDAL_VSI, "/vsimem/zonal-stats-row-output-" + UUID.randomUUID() + ".geojson");
  }

  private boolean processRowAsFieldsInternal() throws HopException {
    Object[] row = getRow();
    if (row == null) {
      setOutputDone();
      return false;
    }

    if (!data.initialized) {
      initializeRowFieldOutputMeta(requireInputRowMeta());
    }

    RowFieldResult result;
    try {
      result = executeRowFieldJob(row);
    } catch (Exception e) {
      if (meta.isFailOnError()) {
        throw e instanceof HopException
            ? (HopException) e
            : new HopTransformException(e.getMessage(), e);
      }
      result = RowFieldResult.failure(normalizeMessage(e));
    }

    putRow(data.outputRowMeta, buildRowFieldOutputRow(row, result));
    return true;
  }

  private IRowMeta requireInputRowMeta() throws HopTransformException {
    IRowMeta rowMeta = getInputRowMeta();
    if (rowMeta == null) {
      throw new HopTransformException("Input row metadata is required for Row output mode");
    }
    return rowMeta;
  }

  private void initializeRowFieldOutputMeta(IRowMeta inputRowMeta) throws HopTransformException {
    data.outputRowMeta = (RowMeta) inputRowMeta.clone();
    GdalRasterZonalStatsRowFields.renameUpstreamTechnicalValueMetas(data.outputRowMeta);
    data.rowFieldSpecs = meta.describeRowFields();
    data.rowFieldStartIndex = data.outputRowMeta.size();
    GdalRasterZonalStatsRowFields.appendValueMetas(data.outputRowMeta, data.rowFieldSpecs);
    if (meta.isAddResultFields()) {
      data.resultFieldStartIndex = data.outputRowMeta.size();
      GdalRasterZonalStatsRowFields.appendZonalStatsTechnicalValueMetas(data.outputRowMeta);
    }
    data.initialized = true;
  }

  private RowFieldResult executeRowFieldJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();
    IRowMeta rowMeta = requireInputRowMeta();

    DatasetRef input =
        RasterTransformSupport.resolveDatasetRef(
            meta.getInputSourceMode(),
            meta.getInputValueMode(),
            meta.getInputValue(),
            meta.getInputField(),
            row,
            rowMeta,
            this::resolveConstant);
    RemoteAccessSpec remoteAccess =
        RasterTransformSupport.remoteAccessSpec(
            meta.getAuthType(),
            meta.getAuthUsername(),
            meta.getAuthPassword(),
            meta.getBearerToken(),
            meta.getCustomHeaderName(),
            meta.getCustomHeaderValue(),
            meta.getGdalConfigOptions(),
            this::resolveConstant);

    int geometryIndex = rowMeta.indexOfValue(meta.getGeometryField());
    if (geometryIndex < 0) {
      throw new IllegalArgumentException("Geometry field was not found: " + meta.getGeometryField());
    }
    Geometry geometry = HopGeometrySupport.toJtsGeometry(row[geometryIndex], rowMeta.getValueMeta(geometryIndex));
    if (geometry == null) {
      throw new IllegalArgumentException("Geometry field resolved to null");
    }

    List<String> attributeNames =
        GdalRasterZonalStatsRowFields.resolveZoneAttributeNames(
            rowMeta, meta.getGeometryField(), meta.getIncludeFields());
    Map<String, Object> zoneAttributes =
        GdalRasterZonalStatsRowFields.normalizeZoneAttributes(row, rowMeta, attributeNames);
    PreparedZoneDataset zonesDataset =
        rowZoneDatasetFactory.create(geometry, "zones", zoneAttributes);

    DatasetRef output = createRowModeOutputDatasetRef();
    List<String> args = buildRowFieldArgs(zonesDataset.attributeNames(), zonesDataset.layerName());
    gdalClient().rasterZonalStats(input, zonesDataset.datasetRef(), output, remoteAccess, args);

    Map<String, Object> featureAttributes = rowStatsReader.read(output);
    Map<String, Object> fieldValues =
        GdalRasterZonalStatsRowFields.extractFieldValues(data.rowFieldSpecs, featureAttributes);
    return RowFieldResult.success(
        fieldValues,
        RasterTransformResult.success(
            System.currentTimeMillis() - start,
            input.value(),
            null,
            GdalRasterZonalStatsRowFields.toJson(fieldValues)));
  }

  private List<String> buildRowFieldArgs(List<String> includeFieldNames, String zonesLayerName) {
    List<String> args = new ArrayList<>();
    args.add("--output-format");
    args.add("GeoJSON");
    if (zonesLayerName != null && !zonesLayerName.isBlank()) {
      args.add("--zones-layer");
      args.add(zonesLayerName);
    }
    for (int band : GdalRasterZonalStatsOptions.parseBands(resolveConstant(meta.getBands()))) {
      args.add("--band");
      args.add(Integer.toString(band));
    }
    for (String stat : GdalRasterZonalStatsOptions.parseStats(resolveConstant(meta.getStats()))) {
      args.add("--stat");
      args.add(stat);
    }
    args.add("--pixels");
    args.add(GdalRasterZonalStatsOptions.normalizePixels(resolveConstant(meta.getPixelInclusion())));
    args.add("--strategy");
    args.add(GdalRasterZonalStatsOptions.normalizeStrategy(resolveConstant(meta.getStrategy())));
    for (String fieldName : includeFieldNames) {
      args.add("--include-field");
      args.add(fieldName);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalArgs())));
    return args;
  }

  private Object[] buildRowFieldOutputRow(Object[] inputRow, RowFieldResult result) {
    Object[] baseRow = inputRow == null ? RowDataUtil.allocateRowData(0) : inputRow.clone();
    Object[] outputRow = RowDataUtil.resizeArray(baseRow, data.outputRowMeta.size());

    int fieldIndex = data.rowFieldStartIndex;
    for (GdalRasterZonalStatsRowFields.RowFieldSpec spec : data.rowFieldSpecs) {
      outputRow[fieldIndex++] = result.fieldValues().get(spec.fieldName());
    }

    if (meta.isAddResultFields()) {
      GdalRasterZonalStatsRowFields.applyZonalStatsTechnicalValues(
          outputRow, data.resultFieldStartIndex, result.technicalResult());
    }
    return outputRow;
  }

  interface RowZoneDatasetFactory {
    PreparedZoneDataset create(Geometry geometry, String layerName, Map<String, Object> attributes)
        throws Exception;
  }

  interface RowStatsReader {
    Map<String, Object> read(DatasetRef outputDataset) throws Exception;
  }

  record PreparedZoneDataset(DatasetRef datasetRef, String layerName, List<String> attributeNames) {}

  private record RowFieldResult(
      Map<String, Object> fieldValues, RasterTransformResult technicalResult) {
    static RowFieldResult success(Map<String, Object> fieldValues, RasterTransformResult technicalResult) {
      return new RowFieldResult(new LinkedHashMap<>(fieldValues), technicalResult);
    }

    static RowFieldResult failure(String message) {
      return new RowFieldResult(
          Map.of(),
          RasterTransformResult.failure(
              0L, null, null, message, "{\"error\":\"" + escapeJson(message) + "\"}"));
    }

    private static String escapeJson(String value) {
      return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
  }

  private static final class DefaultRowZoneDatasetFactory implements RowZoneDatasetFactory {
    private static final DefaultRowZoneDatasetFactory INSTANCE = new DefaultRowZoneDatasetFactory();

    @Override
    public PreparedZoneDataset create(Geometry geometry, String layerName, Map<String, Object> attributes)
        throws Exception {
      DatasetRef datasetRef =
          VsiVectorDatasetSupport.writeSingleGeometryDataset(geometry, layerName, attributes);
      return new PreparedZoneDataset(datasetRef, layerName, List.copyOf(attributes.keySet()));
    }
  }

  private static final class DefaultRowStatsReader implements RowStatsReader {
    private static final DefaultRowStatsReader INSTANCE = new DefaultRowStatsReader();

    @Override
    public Map<String, Object> read(DatasetRef outputDataset) throws Exception {
      try (OgrDataSource dataSource = Ogr.open(toBindingDatasetRef(outputDataset), Map.of(), GdalConfig.empty())) {
        List<OgrLayerDefinition> layers = dataSource.listLayers();
        if (layers.isEmpty()) {
          throw new IllegalArgumentException("Row output dataset does not contain any layers: " + outputDataset.value());
        }
        OgrLayerDefinition layer = layers.getFirst();
        try (var reader = dataSource.openReader(layer.name(), Map.of())) {
          var iterator = reader.iterator();
          if (!iterator.hasNext()) {
            throw new IllegalArgumentException(
                "Row output dataset does not contain any features: " + outputDataset.value());
          }
          OgrFeature feature = iterator.next();
          return new LinkedHashMap<>(feature.attributes());
        }
      }
    }

    private ch.so.agi.gdal.ffm.DatasetRef toBindingDatasetRef(DatasetRef ref) {
      return switch (ref.type()) {
        case LOCAL_FILE -> ch.so.agi.gdal.ffm.DatasetRef.local(java.nio.file.Path.of(ref.value()));
        case HTTP_URL -> ch.so.agi.gdal.ffm.DatasetRef.httpUrl(ref.value());
        case GDAL_VSI -> ch.so.agi.gdal.ffm.DatasetRef.gdalVsi(ref.value());
      };
    }
  }
}
