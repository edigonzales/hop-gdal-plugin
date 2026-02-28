package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrLayerReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

public class OgrInputData extends BaseTransformData implements ITransformData {
  IRowMeta outputRowMeta;
  boolean initialized;
  OgrDataSource dataSource;
  OgrLayerReader layerReader;
  Iterator<OgrFeature> iterator;
  int fidIndex = -1;
  int geometryIndex = -1;
  Map<String, Integer> attributeIndexes = new LinkedHashMap<>();
}
