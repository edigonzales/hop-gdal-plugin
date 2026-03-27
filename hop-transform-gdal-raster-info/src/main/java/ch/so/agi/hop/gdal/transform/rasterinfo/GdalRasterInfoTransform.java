package ch.so.agi.hop.gdal.transform.rasterinfo;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

public class GdalRasterInfoTransform
    extends AbstractGdalRasterTransform<GdalRasterInfoMeta, GdalRasterInfoData> {

  public GdalRasterInfoTransform(
      TransformMeta transformMeta,
      GdalRasterInfoMeta meta,
      GdalRasterInfoData data,
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
            meta.getInputSourceMode(),
            meta.getInputValueMode(),
            meta.getInputValue(),
            meta.getInputField(),
            row,
            getInputRowMeta(),
            this::resolveConstant);
    RemoteAccessSpec remoteAccess =
        RasterTransformSupport.remoteAccessSpec(
            meta.getAuthType(),
            meta.getAuthUsername(),
            meta.getAuthPassword(),
            meta.getBearerToken(),
            meta.getCustomHeaderName(),
            meta.getCustomHeaderValue(),
            meta.getGdalConfigOptions(),
            this::resolveConstant);

    List<String> args = new ArrayList<>(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalInfoArgs())));
    if (!args.contains("--output-format") && !args.contains("-f") && !args.contains("--format")) {
      args.add(0, "json");
      args.add(0, "--output-format");
    }

    String json = gdalClient().rasterInfo(input, remoteAccess, args);
    return RasterTransformResult.success(System.currentTimeMillis() - start, input.value(), null, json);
  }
}
