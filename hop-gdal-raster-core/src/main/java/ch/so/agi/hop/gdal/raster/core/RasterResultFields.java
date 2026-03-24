package ch.so.agi.hop.gdal.raster.core;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.value.ValueMetaBoolean;
import org.apache.hop.core.row.value.ValueMetaInteger;
import org.apache.hop.core.row.value.ValueMetaString;

public final class RasterResultFields {
  public static final String SUCCESS = "gdal_success";
  public static final String MESSAGE = "gdal_message";
  public static final String ELAPSED_MS = "gdal_elapsed_ms";
  public static final String INPUT = "gdal_input";
  public static final String OUTPUT = "gdal_output";
  public static final String DETAILS_JSON = "gdal_details_json";

  private RasterResultFields() {}

  public static void appendTo(IRowMeta rowMeta) {
    rowMeta.addValueMeta(new ValueMetaBoolean(SUCCESS));
    rowMeta.addValueMeta(new ValueMetaString(MESSAGE));
    rowMeta.addValueMeta(new ValueMetaInteger(ELAPSED_MS));
    rowMeta.addValueMeta(new ValueMetaString(INPUT));
    rowMeta.addValueMeta(new ValueMetaString(OUTPUT));
    rowMeta.addValueMeta(new ValueMetaString(DETAILS_JSON));
  }
}
