package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrLayerWriter;
import ch.so.agi.gdal.ffm.OgrWriteMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

public class OgrOutputData extends BaseTransformData implements ITransformData {
  boolean definitionPrepared;
  boolean initialized;
  boolean writeGeometry = true;
  OgrDataSource dataSource;
  OgrLayerWriter writer;
  IRowMeta inputRowMeta;
  int geometryFieldIndex = -1;
  List<String> attributeNames = new ArrayList<>();
  List<Integer> attributeIndexes = new ArrayList<>();
  List<IValueMeta> attributeValueMetas = new ArrayList<>();
  List<OgrFieldDefinition> schema = new ArrayList<>();
  List<Object[]> pendingRows = new ArrayList<>();
  Map<String, String> datasetOptions = Map.of();
  Map<String, String> effectiveLayerOptions = Map.of();
  OgrWriteMode writeMode;
  String resolvedFileName;
  String resolvedFormat;
  String layerName;
  int forcedGeometryTypeCode = -1;
}
