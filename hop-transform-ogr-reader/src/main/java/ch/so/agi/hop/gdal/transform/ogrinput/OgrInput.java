package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrGeometry;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.gdal.ffm.OgrReaderOptions;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import com.atolcd.hop.core.row.value.ValueMetaGeometry;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

public class OgrInput extends BaseTransform<OgrInputMeta, OgrInputData> {

  private static final Class<?> PKG = OgrInputMeta.class;
  private final WKBReader wkbReader = new WKBReader();

  public OgrInput(
      TransformMeta transformMeta,
      OgrInputMeta meta,
      OgrInputData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean processRow() throws HopException {
    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(this::processRowInternal);
  }

  private boolean processRowInternal() throws HopException {
    if (!data.initialized) {
      initializeReader();
    }

    if (data.iterator != null && data.iterator.hasNext()) {
      OgrFeature feature = data.iterator.next();
      Object[] outputRow = RowDataUtil.allocateRowData(data.outputRowMeta.size());

      if (data.fidIndex >= 0) {
        outputRow[data.fidIndex] = feature.fid();
      }

      for (Map.Entry<String, Integer> attributeIndex : data.attributeIndexes.entrySet()) {
        outputRow[attributeIndex.getValue()] = feature.attributes().get(attributeIndex.getKey());
      }

      outputRow[data.geometryIndex] = toJtsGeometry(feature.geometry());

      putRow(data.outputRowMeta, outputRow);
      return true;
    }

    closeResources();
    setOutputDone();
    return false;
  }

  private void initializeReader() throws HopException {
    String resolvedFileName = normalizeResolved(meta.getFileName());
    if (resolvedFileName.isBlank()) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrInput.Transform.FileNameEmpty"));
    }

    String resolvedLayerName = normalizeResolved(meta.getLayerName());
    String resolvedSelectedAttributes = normalizeResolved(meta.getSelectedAttributes());

    try {
      Map<String, String> openOptions = resolveOpenOptions();
      Map<String, String> readerOptions = resolveReaderOptions(resolvedSelectedAttributes);

      data.dataSource = Ogr.open(Path.of(resolvedFileName), openOptions);
      OgrLayerDefinition layerDefinition = resolveLayerDefinition(data.dataSource, resolvedLayerName);
      List<OgrFieldDefinition> projectedFields =
          selectProjectedFields(layerDefinition, resolvedSelectedAttributes);

      data.outputRowMeta = buildOutputRowMeta(projectedFields);
      data.layerReader = data.dataSource.openReader(layerDefinition.name(), readerOptions);
      data.iterator = data.layerReader.iterator();
      data.initialized = true;

      if (isBasic()) {
        logBasic(
            BaseMessages.getString(
                PKG,
                "OgrInput.Transform.OpenedDataset",
                resolvedFileName,
                layerDefinition.name()));
      }
    } catch (IllegalArgumentException e) {
      closeResources();
      throw new HopTransformException(e.getMessage(), e);
    } catch (RuntimeException e) {
      closeResources();
      throw new HopTransformException(BaseMessages.getString(PKG, "OgrInput.Transform.OpenFailed"), e);
    }
  }

  private Map<String, String> resolveOpenOptions() {
    return new LinkedHashMap<>(
        OgrInputOptionsUtil.parseKeyValueOptions(normalizeResolved(meta.getOpenOptions())));
  }

  private Map<String, String> resolveReaderOptions(String resolvedSelectedAttributes) {
    Map<String, String> readerOptions = new LinkedHashMap<>();

    String attributeFilter = normalizeResolved(meta.getAttributeFilter());
    if (!attributeFilter.isBlank()) {
      readerOptions.put(OgrReaderOptions.ATTRIBUTE_FILTER, attributeFilter);
    }

    String bbox = normalizeResolved(meta.getBbox());
    String polygonWkt = normalizeResolved(meta.getPolygonWkt());
    OgrInputOptionsUtil.validateSpatialFilterExclusivity(bbox, polygonWkt);
    if (!bbox.isBlank()) {
      readerOptions.put(OgrReaderOptions.BBOX, bbox);
    }
    if (!polygonWkt.isBlank()) {
      readerOptions.put(OgrReaderOptions.SPATIAL_FILTER_WKT, polygonWkt);
    }

    if (!resolvedSelectedAttributes.isBlank()) {
      readerOptions.put(OgrReaderOptions.SELECTED_FIELDS, resolvedSelectedAttributes);
    }

    Long featureLimit = OgrInputOptionsUtil.parsePositiveLimit(normalizeResolved(meta.getFeatureLimit()));
    if (featureLimit != null) {
      readerOptions.put(OgrReaderOptions.LIMIT, Long.toString(featureLimit));
    }

    return readerOptions;
  }

  private OgrLayerDefinition resolveLayerDefinition(OgrDataSource dataSource, String resolvedLayerName)
      throws HopTransformException {
    List<OgrLayerDefinition> layers = dataSource.listLayers();
    if (layers.isEmpty()) {
      throw new HopTransformException(BaseMessages.getString(PKG, "OgrInput.Transform.NoLayers"));
    }

    if (resolvedLayerName.isBlank()) {
      return layers.getFirst();
    }

    for (OgrLayerDefinition layer : layers) {
      if (layer.name().equals(resolvedLayerName)) {
        return layer;
      }
    }
    for (OgrLayerDefinition layer : layers) {
      if (layer.name().equalsIgnoreCase(resolvedLayerName)) {
        return layer;
      }
    }

    throw new HopTransformException(
        BaseMessages.getString(PKG, "OgrInput.Transform.LayerNotFound", resolvedLayerName));
  }

  static List<OgrFieldDefinition> selectProjectedFields(
      OgrLayerDefinition layerDefinition, String selectedAttributesRaw) {
    List<String> selectedAttributes = OgrInputOptionsUtil.splitCsvOrSemicolon(selectedAttributesRaw);
    if (selectedAttributes.isEmpty()) {
      return layerDefinition.fields();
    }

    Set<String> selectedLower = new LinkedHashSet<>();
    for (String selectedAttribute : selectedAttributes) {
      selectedLower.add(selectedAttribute.toLowerCase(Locale.ROOT));
    }

    Set<String> unknown = new LinkedHashSet<>(selectedLower);
    for (OgrFieldDefinition field : layerDefinition.fields()) {
      unknown.remove(field.name().toLowerCase(Locale.ROOT));
    }
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
          BaseMessages.getString(PKG, "OgrInput.Transform.UnknownSelectedAttributes", unknown));
    }

    return layerDefinition.fields().stream()
        .filter(field -> selectedLower.contains(field.name().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private RowMeta buildOutputRowMeta(List<OgrFieldDefinition> projectedFields) {
    RowMeta rowMeta = new RowMeta();
    data.attributeIndexes.clear();

    if (meta.isIncludeFid()) {
      data.fidIndex = rowMeta.size();
      rowMeta.addValueMeta(new ValueMetaInteger(resolveFieldName(meta.getFidFieldName(), "fid")));
    } else {
      data.fidIndex = -1;
    }

    for (OgrFieldDefinition projectedField : projectedFields) {
      int index = rowMeta.size();
      rowMeta.addValueMeta(toHopValueMeta(projectedField));
      data.attributeIndexes.put(projectedField.name(), index);
    }

    data.geometryIndex = rowMeta.size();
    rowMeta.addValueMeta(new ValueMetaGeometry(resolveFieldName(meta.getGeometryFieldName(), "geometry")));

    return rowMeta;
  }

  static org.apache.hop.core.row.IValueMeta toHopValueMeta(OgrFieldDefinition fieldDefinition) {
    return switch (fieldDefinition.type()) {
      case INTEGER, INTEGER64 -> new ValueMetaInteger(fieldDefinition.name());
      case REAL -> new ValueMetaNumber(fieldDefinition.name());
      default -> new ValueMetaString(fieldDefinition.name());
    };
  }

  private String resolveFieldName(String fieldName, String defaultName) {
    String trimmed = OgrInputOptionsUtil.trimToNull(fieldName);
    return trimmed == null ? defaultName : trimmed;
  }

  @Override
  public void dispose() {
    closeResources();
    super.dispose();
  }

  private void closeResources() {
    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(this::closeResourcesInternal);
  }

  private void closeResourcesInternal() {
    if (data.layerReader != null) {
      try {
        data.layerReader.close();
      } catch (Exception e) {
        logError(BaseMessages.getString(PKG, "OgrInput.Transform.CloseReaderFailed"), e);
      }
      data.layerReader = null;
    }

    if (data.dataSource != null) {
      try {
        data.dataSource.close();
      } catch (Exception e) {
        logError(BaseMessages.getString(PKG, "OgrInput.Transform.CloseDatasourceFailed"), e);
      }
      data.dataSource = null;
    }
  }

  private Geometry toJtsGeometry(OgrGeometry ogrGeometry) throws HopTransformException {
    if (ogrGeometry == null) {
      return null;
    }

    try {
      Geometry geometry = wkbReader.read(ogrGeometry.ewkb());
      ogrGeometry.srid().ifPresent(geometry::setSRID);
      return geometry;
    } catch (ParseException e) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrInput.Transform.GeometryParseFailed"), e);
    }
  }

  private String normalizeResolved(String value) {
    return value == null ? "" : resolve(value).trim();
  }
}
