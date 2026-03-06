package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import ch.so.agi.gdal.ffm.OgrGeometry;
import ch.so.agi.gdal.ffm.OgrLayerWriteSpec;
import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

public class OgrOutput extends BaseTransform<OgrOutputMeta, OgrOutputData> {

  private static final Class<?> PKG = OgrOutputMeta.class;

  private final WKBWriter wkbWriter = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false);
  private final WKBReader wkbReader = new WKBReader();

  public OgrOutput(
      TransformMeta transformMeta,
      OgrOutputMeta meta,
      OgrOutputData data,
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
    Object[] row = getRow();
    if (row == null) {
      closeResources();
      setOutputDone();
      return false;
    }

    if (!data.initialized) {
      initializeWriter(row);
    }

    writeRow(row);
    incrementLinesOutput();
    return true;
  }

  private void initializeWriter(Object[] firstRow) throws HopTransformException {
    data.inputRowMeta = getInputRowMeta();
    if (data.inputRowMeta == null) {
      throw new HopTransformException(BaseMessages.getString(PKG, "OgrOutput.Transform.NoInputMetadata"));
    }

    String resolvedFileName = normalizeResolved(meta.getFileName());
    if (resolvedFileName.isBlank()) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrOutput.Transform.FileNameEmpty"));
    }

    String resolvedFormat = normalizeResolved(meta.getFormat());
    if (resolvedFormat.isBlank()) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrOutput.Transform.FormatEmpty"));
    }

    String resolvedGeometryField = normalizeResolved(meta.getGeometryField());
    if (resolvedGeometryField.isBlank()) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrOutput.Transform.GeometryFieldEmpty"));
    }

    data.geometryFieldIndex = data.inputRowMeta.indexOfValue(resolvedGeometryField);
    if (data.geometryFieldIndex < 0) {
      throw new HopTransformException(
          BaseMessages.getString(
              PKG, "OgrOutput.Transform.GeometryFieldNotFound", resolvedGeometryField));
    }

    List<String> attributeNames =
        resolveAttributeNames(data.inputRowMeta, resolvedGeometryField, normalizeResolved(meta.getSelectedAttributes()));

    data.attributeNames = new ArrayList<>(attributeNames.size());
    data.attributeIndexes = new ArrayList<>(attributeNames.size());
    data.attributeValueMetas = new ArrayList<>(attributeNames.size());
    List<OgrFieldDefinition> schema = new ArrayList<>(attributeNames.size());

    for (String attributeName : attributeNames) {
      int fieldIndex = data.inputRowMeta.indexOfValue(attributeName);
      if (fieldIndex < 0) {
        throw new HopTransformException(
            BaseMessages.getString(PKG, "OgrOutput.Transform.AttributeNotFound", attributeName));
      }

      IValueMeta valueMeta = data.inputRowMeta.getValueMeta(fieldIndex);
      data.attributeNames.add(attributeName);
      data.attributeIndexes.add(fieldIndex);
      data.attributeValueMetas.add(valueMeta);
      schema.add(new OgrFieldDefinition(attributeName, mapHopTypeToOgrType(valueMeta)));
    }

    String resolvedLayerName = normalizeResolved(meta.getLayerName());
    if (resolvedLayerName.isBlank()) {
      resolvedLayerName = Utils.isEmpty(getTransformName()) ? "layer" : getTransformName();
    }
    data.layerName = resolvedLayerName;

    OgrWriteMode writeMode = OgrOutputOptionsUtil.parseWriteMode(normalizeResolved(meta.getWriteMode()));
    Map<String, String> datasetOptions =
        OgrOutputOptionsUtil.parseKeyValueOptions(normalizeResolved(meta.getDatasetCreationOptions()));
    Map<String, String> layerOptions =
        OgrOutputOptionsUtil.parseKeyValueOptions(normalizeResolved(meta.getLayerCreationOptions()));
    Map<String, String> effectiveLayerOptions = resolveEffectiveLayerOptions(resolvedFormat, layerOptions);

    data.writeGeometry = shouldWriteGeometry(resolvedFormat, effectiveLayerOptions);

    Geometry firstGeometry = data.writeGeometry ? toJtsGeometry(firstRow[data.geometryFieldIndex]) : null;
    int geometryTypeCode = resolveGeometryTypeCode(meta.getForceGeometryType(), firstGeometry);
    int effectiveGeometryTypeCode =
        resolveEffectiveGeometryTypeCode(resolvedFormat, effectiveLayerOptions, geometryTypeCode);

    data.dataSource = Ogr.create(Path.of(resolvedFileName), resolvedFormat, writeMode, datasetOptions);

    OgrLayerWriteSpec writeSpec =
        new OgrLayerWriteSpec(
            resolvedLayerName,
            effectiveGeometryTypeCode,
            schema,
            writeMode,
            datasetOptions,
            effectiveLayerOptions,
            null,
            null);
    data.writer = data.dataSource.openWriter(writeSpec);
    data.initialized = true;

    if (isBasic()) {
      logBasic(
          BaseMessages.getString(
              PKG,
              "OgrOutput.Transform.OpenedDataset",
              resolvedFileName,
              resolvedLayerName,
              resolvedFormat));
    }
  }

  private void writeRow(Object[] row) throws HopTransformException {
    Map<String, Object> attributes = new LinkedHashMap<>(data.attributeNames.size());
    for (int i = 0; i < data.attributeNames.size(); i++) {
      String attributeName = data.attributeNames.get(i);
      int fieldIndex = data.attributeIndexes.get(i);
      IValueMeta valueMeta = data.attributeValueMetas.get(i);
      Object value = row[fieldIndex];
      attributes.put(attributeName, normalizeAttributeValue(valueMeta, value));
    }

    OgrGeometry geometry = data.writeGeometry ? toOgrGeometry(row[data.geometryFieldIndex]) : null;

    try {
      data.writer.write(new OgrFeature(-1L, attributes, geometry));
    } catch (RuntimeException e) {
      throw new HopTransformException(BaseMessages.getString(PKG, "OgrOutput.Transform.WriteFailed"), e);
    }
  }

  static List<String> resolveAttributeNames(
      IRowMeta rowMeta, String geometryFieldName, String selectedAttributesRaw) {
    List<String> selected = OgrOutputOptionsUtil.splitCsvOrSemicolon(selectedAttributesRaw);
    Set<String> selectedLower = new java.util.LinkedHashSet<>();
    for (String value : selected) {
      selectedLower.add(value.toLowerCase(Locale.ROOT));
    }

    String geometryFieldLower = geometryFieldName.toLowerCase(Locale.ROOT);
    List<String> names = new ArrayList<>();

    if (selected.isEmpty()) {
      for (int i = 0; i < rowMeta.size(); i++) {
        String name = rowMeta.getValueMeta(i).getName();
        if (!name.equalsIgnoreCase(geometryFieldName)) {
          names.add(name);
        }
      }
      return List.copyOf(names);
    }

    for (int i = 0; i < rowMeta.size(); i++) {
      String name = rowMeta.getValueMeta(i).getName();
      String lower = name.toLowerCase(Locale.ROOT);
      if (lower.equals(geometryFieldLower)) {
        continue;
      }
      if (selectedLower.contains(lower)) {
        names.add(name);
      }
    }

    Set<String> knownLower = new java.util.LinkedHashSet<>();
    for (String name : names) {
      knownLower.add(name.toLowerCase(Locale.ROOT));
    }
    Set<String> unknown = new java.util.LinkedHashSet<>(selectedLower);
    unknown.removeAll(knownLower);
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException("Selected attributes were not found in input row: " + unknown);
    }

    return List.copyOf(names);
  }

  static Map<String, String> resolveEffectiveLayerOptions(
      String format, Map<String, String> layerOptions) {
    return OgrOutputOptionsUtil.resolveEffectiveLayerCreationOptions(format, layerOptions);
  }

  static boolean shouldWriteGeometry(String format, Map<String, String> layerOptions) {
    return OgrOutputOptionsUtil.shouldWriteGeometry(format, layerOptions);
  }

  static int resolveEffectiveGeometryTypeCode(
      String format, Map<String, String> layerOptions, int geometryTypeCode) {
    return shouldWriteGeometry(format, layerOptions) ? geometryTypeCode : 0;
  }

  static OgrFieldType mapHopTypeToOgrType(IValueMeta valueMeta) {
    return switch (valueMeta.getType()) {
      case IValueMeta.TYPE_INTEGER -> OgrFieldType.INTEGER64;
      case IValueMeta.TYPE_NUMBER, IValueMeta.TYPE_BIGNUMBER -> OgrFieldType.REAL;
      case IValueMeta.TYPE_BOOLEAN -> OgrFieldType.INTEGER;
      case IValueMeta.TYPE_DATE, IValueMeta.TYPE_TIMESTAMP -> OgrFieldType.STRING;
      default -> OgrFieldType.STRING;
    };
  }

  static int resolveGeometryTypeCode(String forcedGeometryTypeRaw, Geometry geometry) {
    int forcedCode = OgrOutputOptionsUtil.parseForcedGeometryTypeCode(forcedGeometryTypeRaw);
    if (forcedCode >= 0) {
      return forcedCode;
    }

    if (geometry == null) {
      return 0;
    }
    if (geometry instanceof Point) {
      return 1;
    }
    if (geometry instanceof LineString) {
      return 2;
    }
    if (geometry instanceof Polygon) {
      return 3;
    }
    if (geometry instanceof MultiPoint) {
      return 4;
    }
    if (geometry instanceof MultiLineString) {
      return 5;
    }
    if (geometry instanceof MultiPolygon) {
      return 6;
    }
    if (geometry instanceof GeometryCollection) {
      return 7;
    }
    return 0;
  }

  private Object normalizeAttributeValue(IValueMeta valueMeta, Object value) {
    if (value == null) {
      return null;
    }

    return switch (valueMeta.getType()) {
      case IValueMeta.TYPE_DATE, IValueMeta.TYPE_TIMESTAMP ->
          value instanceof java.util.Date date ? date.toInstant().toString() : value.toString();
      case IValueMeta.TYPE_BIGNUMBER, IValueMeta.TYPE_NUMBER ->
          value instanceof Number number ? number.doubleValue() : value;
      case IValueMeta.TYPE_INTEGER -> value instanceof Number number ? number.longValue() : value;
      default -> value;
    };
  }

  private OgrGeometry toOgrGeometry(Object value) throws HopTransformException {
    if (value == null) {
      return null;
    }

    if (value instanceof OgrGeometry ogrGeometry) {
      return ogrGeometry;
    }

    if (value instanceof byte[] ewkb) {
      return OgrGeometry.fromEwkb(ewkb);
    }

    Geometry geometry = toJtsGeometry(value);
    if (geometry == null) {
      return null;
    }

    byte[] wkb = wkbWriter.write(geometry);
    int srid = geometry.getSRID();
    if (srid > 0) {
      return OgrGeometry.fromWkb(wkb, srid);
    }
    return OgrGeometry.fromWkb(wkb);
  }

  private Geometry toJtsGeometry(Object value) throws HopTransformException {
    if (value == null) {
      return null;
    }
    if (value instanceof Geometry geometry) {
      return geometry;
    }
    if (value instanceof byte[] bytes) {
      try {
        return wkbReader.read(bytes);
      } catch (ParseException e) {
        throw new HopTransformException(
            BaseMessages.getString(PKG, "OgrOutput.Transform.GeometryParseFailed"), e);
      }
    }
    throw new HopTransformException(
        BaseMessages.getString(PKG, "OgrOutput.Transform.UnsupportedGeometryValue", value.getClass().getName()));
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
    if (data.writer != null) {
      try {
        data.writer.close();
      } catch (Exception e) {
        logError(BaseMessages.getString(PKG, "OgrOutput.Transform.CloseWriterFailed"), e);
      }
      data.writer = null;
    }

    if (data.dataSource != null) {
      try {
        data.dataSource.close();
      } catch (Exception e) {
        logError(BaseMessages.getString(PKG, "OgrOutput.Transform.CloseDatasourceFailed"), e);
      }
      data.dataSource = null;
    }
  }

  private String normalizeResolved(String value) {
    return value == null ? "" : resolve(value).trim();
  }
}
