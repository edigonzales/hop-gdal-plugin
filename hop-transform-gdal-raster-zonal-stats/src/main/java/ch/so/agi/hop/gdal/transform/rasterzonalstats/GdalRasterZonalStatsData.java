package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterData;
import java.util.List;

public class GdalRasterZonalStatsData extends AbstractGdalRasterData {
  int rowFieldStartIndex = -1;
  List<GdalRasterZonalStatsRowFields.RowFieldSpec> rowFieldSpecs = List.of();
}
