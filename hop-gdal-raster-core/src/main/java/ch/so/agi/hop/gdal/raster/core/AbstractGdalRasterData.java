package ch.so.agi.hop.gdal.raster.core;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;

public class AbstractGdalRasterData extends BaseTransformData {
  public boolean initialized;
  public boolean syntheticRowProduced;
  public IRowMeta outputRowMeta;
  public int resultFieldStartIndex = -1;
}
