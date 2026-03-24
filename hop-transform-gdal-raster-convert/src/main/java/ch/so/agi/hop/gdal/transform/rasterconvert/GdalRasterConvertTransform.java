package ch.so.agi.hop.gdal.transform.rasterconvert;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

public class GdalRasterConvertTransform
    extends AbstractGdalRasterTransform<GdalRasterConvertMeta, GdalRasterConvertData> {

  public GdalRasterConvertTransform(
      TransformMeta transformMeta,
      GdalRasterConvertMeta meta,
      GdalRasterConvertData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();
    DatasetRef input =
        RasterTransformSupport.resolveDatasetRef(
            meta.getInputSourceMode(), meta.getInputValueMode(), meta.getInputValue(), meta.getInputField(), row, getInputRowMeta(), this::resolveConstant);
    DatasetRef output =
        RasterTransformSupport.resolveDatasetRef(
            meta.getOutputSourceMode(), meta.getOutputValueMode(), meta.getOutputValue(), meta.getOutputField(), row, getInputRowMeta(), this::resolveConstant);
    RemoteAccessSpec remoteAccess =
        RasterTransformSupport.remoteAccessSpec(
            meta.getAuthType(), meta.getAuthUsername(), meta.getAuthPassword(), meta.getBearerToken(),
            meta.getCustomHeaderName(), meta.getCustomHeaderValue(), meta.getGdalConfigOptions(), this::resolveConstant);

    List<String> args = new ArrayList<>();
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("-of");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    if (meta.isOverwrite()) {
      args.add("-overwrite");
    }
    for (String band : splitValues(resolveConstant(meta.getBandSelection()))) {
      args.add("-b");
      args.add(band);
    }
    if (meta.getOutputDataType() != null && !meta.getOutputDataType().isBlank()) {
      args.add("-ot");
      args.add(resolveConstant(meta.getOutputDataType()));
    }
    if (meta.isScale()) {
      args.add("-scale");
    }
    if (meta.isUnscale()) {
      args.add("-unscale");
    }
    appendWindowArg(args, "-srcwin", resolveConstant(meta.getPixelWindow()));
    appendWindowArg(args, "-projwin", resolveConstant(meta.getCoordinateWindow()));
    if (meta.getOutputNoData() != null && !meta.getOutputNoData().isBlank()) {
      args.add("-a_nodata");
      args.add(resolveConstant(meta.getOutputNoData()));
    }
    for (String creationOption : CreationOptionParser.parse(resolveConstant(meta.getCreationOptions()))) {
      args.add("-co");
      args.add(creationOption);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalTranslateArgs())));

    gdalClient().translate(input, output, remoteAccess, args);
    return RasterTransformResult.success(System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }

  private static void appendWindowArg(List<String> args, String flag, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    String[] parts = value.split("[,;]");
    if (parts.length != 4) {
      throw new IllegalArgumentException("Window must contain four values: " + value);
    }
    args.add(flag);
    for (String part : parts) {
      args.add(part.trim());
    }
  }

  private static List<String> splitValues(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    for (String token : text.split("[,;]")) {
      String cleaned = token.trim();
      if (!cleaned.isBlank()) {
        values.add(cleaned);
      }
    }
    return values;
  }
}
