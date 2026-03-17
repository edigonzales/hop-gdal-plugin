package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrFieldType;
import ch.so.agi.gdal.ffm.OgrGeometry;
import ch.so.agi.gdal.ffm.OgrLayerWriteSpec;
import ch.so.agi.gdal.ffm.OgrLayerWriter;
import ch.so.agi.gdal.ffm.OgrWriteMode;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
  private static final String JTS_GEOMETRY_CLASS_NAME = "org.locationtech.jts.geom.Geometry";
  private static final String JTS_WKB_WRITER_CLASS_NAME = "org.locationtech.jts.io.WKBWriter";

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
      incrementLinesOutput(finalizePendingRows());
      closeResources();
      setOutputDone();
      return false;
    }

    prepareWriterDefinition();
    incrementLinesOutput(handleRow(row));
    return true;
  }

  private int handleRow(Object[] row) throws HopTransformException {
    Geometry geometry = data.writeGeometry ? toJtsGeometry(row[data.geometryFieldIndex]) : null;
    if (!data.initialized) {
      if (shouldDelayWriterInitialization(
          data.writeGeometry, data.forcedGeometryTypeCode, geometry)) {
        data.pendingRows.add(copyRow(row));
        return 0;
      }

      openWriter(geometry);
      int flushedRows = flushPendingRows();
      writeRow(row, geometry);
      return flushedRows + 1;
    }

    writeRow(row, geometry);
    return 1;
  }

  private void prepareWriterDefinition() throws HopTransformException {
    if (data.definitionPrepared) {
      return;
    }

    data.inputRowMeta = getInputRowMeta();
    if (data.inputRowMeta == null) {
      throw new HopTransformException(BaseMessages.getString(PKG, "OgrOutput.Transform.NoInputMetadata"));
    }

    data.resolvedFileName = normalizeResolved(meta.getFileName());
    if (data.resolvedFileName.isBlank()) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrOutput.Transform.FileNameEmpty"));
    }

    data.resolvedFormat = normalizeResolved(meta.getFormat());
    if (data.resolvedFormat.isBlank()) {
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
    data.schema = new ArrayList<>(attributeNames.size());

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
      data.schema.add(new OgrFieldDefinition(attributeName, mapHopTypeToOgrType(valueMeta)));
    }

    String resolvedLayerName = normalizeResolved(meta.getLayerName());
    if (resolvedLayerName.isBlank()) {
      resolvedLayerName = Utils.isEmpty(getTransformName()) ? "layer" : getTransformName();
    }
    data.layerName = resolvedLayerName;

    data.writeMode = OgrOutputOptionsUtil.parseWriteMode(normalizeResolved(meta.getWriteMode()));
    data.datasetOptions =
        OgrOutputOptionsUtil.parseKeyValueOptions(normalizeResolved(meta.getDatasetCreationOptions()));
    Map<String, String> layerOptions =
        OgrOutputOptionsUtil.parseKeyValueOptions(normalizeResolved(meta.getLayerCreationOptions()));
    data.effectiveLayerOptions = resolveEffectiveLayerOptions(data.resolvedFormat, layerOptions);

    data.writeGeometry = shouldWriteGeometry(data.resolvedFormat, data.effectiveLayerOptions);
    data.forcedGeometryTypeCode =
        OgrOutputOptionsUtil.parseForcedGeometryTypeCode(meta.getForceGeometryType());
    data.definitionPrepared = true;
  }

  private void writeRow(Object[] row) throws HopTransformException {
    Geometry geometry = data.writeGeometry ? toJtsGeometry(row[data.geometryFieldIndex]) : null;
    writeRow(row, geometry);
  }

  private void writeRow(Object[] row, Geometry geometry) throws HopTransformException {
    Map<String, Object> attributes = new LinkedHashMap<>(data.attributeNames.size());
    for (int i = 0; i < data.attributeNames.size(); i++) {
      String attributeName = data.attributeNames.get(i);
      int fieldIndex = data.attributeIndexes.get(i);
      IValueMeta valueMeta = data.attributeValueMetas.get(i);
      Object value = row[fieldIndex];
      attributes.put(attributeName, normalizeAttributeValue(valueMeta, value));
    }

    OgrGeometry ogrGeometry = data.writeGeometry ? toOgrGeometry(geometry) : null;

    try {
      data.writer.write(new OgrFeature(-1L, attributes, ogrGeometry));
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
    return resolveGeometryTypeCode(
        OgrOutputOptionsUtil.parseForcedGeometryTypeCode(forcedGeometryTypeRaw), geometry);
  }

  static int resolveGeometryTypeCode(int forcedCode, Geometry geometry) {
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

  static boolean shouldDelayWriterInitialization(
      boolean writeGeometry, int forcedGeometryTypeCode, Geometry geometry) {
    return writeGeometry && forcedGeometryTypeCode < 0 && geometry == null;
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

  Geometry toJtsGeometry(Object value) throws HopTransformException {
    if (value == null) {
      return null;
    }

    if (value instanceof Geometry geometry) {
      return geometry;
    }

    if (value instanceof OgrGeometry ogrGeometry) {
      return toJtsGeometry(ogrGeometry);
    }

    if (value instanceof byte[] bytes) {
      return readGeometry(bytes);
    }

    if (isJtsGeometryObject(value.getClass())) {
      return toLocalJtsGeometry(value);
    }

    throw unsupportedGeometryValue(value);
  }

  private Geometry toJtsGeometry(OgrGeometry ogrGeometry) throws HopTransformException {
    if (ogrGeometry == null) {
      return null;
    }

    Geometry geometry = readGeometry(ogrGeometry.ewkb());
    ogrGeometry.srid().ifPresent(geometry::setSRID);
    return geometry;
  }

  private OgrGeometry toOgrGeometry(Geometry geometry) {
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

  private int finalizePendingRows() throws HopTransformException {
    if (!data.definitionPrepared || data.initialized || data.pendingRows.isEmpty()) {
      return 0;
    }

    openWriter(null);
    return flushPendingRows();
  }

  private int flushPendingRows() throws HopTransformException {
    int writtenRows = 0;
    for (Object[] pendingRow : data.pendingRows) {
      writeRow(pendingRow);
      writtenRows++;
    }
    data.pendingRows.clear();
    return writtenRows;
  }

  private void openWriter(Geometry geometry) throws HopTransformException {
    int geometryTypeCode = resolveGeometryTypeCode(data.forcedGeometryTypeCode, geometry);
    int effectiveGeometryTypeCode =
        resolveEffectiveGeometryTypeCode(
            data.resolvedFormat, data.effectiveLayerOptions, geometryTypeCode);

    data.dataSource =
        createDataSource(data.resolvedFileName, data.resolvedFormat, data.writeMode, data.datasetOptions);

    OgrLayerWriteSpec writeSpec =
        new OgrLayerWriteSpec(
            data.layerName,
            effectiveGeometryTypeCode,
            data.schema,
            data.writeMode,
            data.datasetOptions,
            data.effectiveLayerOptions,
            null,
            null);
    data.writer = openWriter(data.dataSource, writeSpec);
    data.initialized = true;

    if (isBasic()) {
      logBasic(
          BaseMessages.getString(
              PKG,
              "OgrOutput.Transform.OpenedDataset",
              data.resolvedFileName,
              data.layerName,
              data.resolvedFormat));
    }
  }

  protected OgrDataSource createDataSource(
      String resolvedFileName,
      String resolvedFormat,
      OgrWriteMode writeMode,
      Map<String, String> datasetOptions) {
    return Ogr.create(Path.of(resolvedFileName), resolvedFormat, writeMode, datasetOptions);
  }

  protected OgrLayerWriter openWriter(OgrDataSource dataSource, OgrLayerWriteSpec writeSpec) {
    return dataSource.openWriter(writeSpec);
  }

  private Geometry readGeometry(byte[] bytes) throws HopTransformException {
    try {
      return wkbReader.read(bytes);
    } catch (ParseException e) {
      throw new HopTransformException(
          BaseMessages.getString(PKG, "OgrOutput.Transform.GeometryParseFailed"), e);
    }
  }

  private Geometry toLocalJtsGeometry(Object geometryValue) throws HopTransformException {
    try {
      Class<?> geometryClass = findJtsGeometryClass(geometryValue.getClass());
      if (geometryClass == null) {
        throw unsupportedGeometryValue(geometryValue);
      }

      ClassLoader foreignClassLoader = geometryValue.getClass().getClassLoader();
      Class<?> foreignWkbWriterClass =
          Class.forName(JTS_WKB_WRITER_CLASS_NAME, true, foreignClassLoader);
      Constructor<?> constructor = foreignWkbWriterClass.getConstructor();
      Object foreignWkbWriter = constructor.newInstance();
      Method writeMethod = foreignWkbWriterClass.getMethod("write", geometryClass);
      byte[] wkb = (byte[]) writeMethod.invoke(foreignWkbWriter, geometryValue);

      Geometry geometry = readGeometry(wkb);
      Method getSridMethod = geometryClass.getMethod("getSRID");
      Object sridValue = getSridMethod.invoke(geometryValue);
      if (sridValue instanceof Number number && number.intValue() > 0) {
        geometry.setSRID(number.intValue());
      }
      return geometry;
    } catch (HopTransformException e) {
      throw e;
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new HopTransformException(
          BaseMessages.getString(
              PKG,
              "OgrOutput.Transform.GeometryReflectionFailed",
              describeGeometryValue(geometryValue)),
          e);
    }
  }

  private HopTransformException unsupportedGeometryValue(Object value) {
    return new HopTransformException(
        BaseMessages.getString(
            PKG,
            "OgrOutput.Transform.UnsupportedGeometryValue",
            describeGeometryValue(value)));
  }

  static boolean isJtsGeometryObject(Class<?> type) {
    return findJtsGeometryClass(type) != null;
  }

  private static Class<?> findJtsGeometryClass(Class<?> type) {
    Class<?> current = type;
    while (current != null) {
      if (JTS_GEOMETRY_CLASS_NAME.equals(current.getName())) {
        return current;
      }
      current = current.getSuperclass();
    }
    return null;
  }

  static String describeGeometryValue(Object value) {
    if (value == null) {
      return "null";
    }
    return value.getClass().getName()
        + " (classLoader="
        + describeClassLoader(value.getClass().getClassLoader())
        + ")";
  }

  private static String describeClassLoader(ClassLoader classLoader) {
    if (classLoader == null) {
      return "bootstrap";
    }
    return classLoader.getClass().getName()
        + "@"
        + Integer.toHexString(System.identityHashCode(classLoader));
  }

  private static Object[] copyRow(Object[] row) {
    return row == null ? null : row.clone();
  }

  private void incrementLinesOutput(int count) {
    for (int i = 0; i < count; i++) {
      incrementLinesOutput();
    }
  }

  private String normalizeResolved(String value) {
    return value == null ? "" : resolve(value).trim();
  }
}
