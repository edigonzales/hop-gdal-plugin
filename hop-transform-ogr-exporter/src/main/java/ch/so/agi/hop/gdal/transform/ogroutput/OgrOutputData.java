package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrLayerWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

public class OgrOutputData extends BaseTransformData implements ITransformData {
  boolean initialized;
  boolean writeGeometry = true;
  OgrDataSource dataSource;
  OgrLayerWriter writer;
  IRowMeta inputRowMeta;
  int geometryFieldIndex = -1;
  List<String> attributeNames = new ArrayList<>();
  List<Integer> attributeIndexes = new ArrayList<>();
  List<IValueMeta> attributeValueMetas = new ArrayList<>();
  String layerName;
}
