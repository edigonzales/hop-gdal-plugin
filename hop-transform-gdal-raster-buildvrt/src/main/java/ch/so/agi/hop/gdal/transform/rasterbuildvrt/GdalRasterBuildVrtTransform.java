package ch.so.agi.hop.gdal.transform.rasterbuildvrt;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

public class GdalRasterBuildVrtTransform
    extends AbstractGdalRasterTransform<GdalRasterBuildVrtMeta, GdalRasterBuildVrtData> {

  public GdalRasterBuildVrtTransform(
      TransformMeta transformMeta,
      GdalRasterBuildVrtMeta meta,
      GdalRasterBuildVrtData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();

    List<DatasetRef> inputs =
        RasterTransformSupport.resolveDatasetRefs(
            meta.getInputInterpretationMode(),
            meta.getInputListValueMode(),
            meta.getInputListValue(),
            meta.getInputListField(),
            row,
            getInputRowMeta(),
            this::resolveConstant);
    DatasetRef output =
        RasterTransformSupport.resolveDatasetRef(
            meta.getOutputSourceMode(),
            meta.getOutputValueMode(),
            meta.getOutputValue(),
            meta.getOutputField(),
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

    List<String> args = new ArrayList<>();
    if (meta.getResolutionStrategy() != null && !meta.getResolutionStrategy().isBlank()) {
      args.add("-resolution");
      args.add(resolveConstant(meta.getResolutionStrategy()).toLowerCase());
    }
    if (meta.isSeparateBands()) {
      args.add("-separate");
    }
    if (meta.getSrcNoData() != null && !meta.getSrcNoData().isBlank()) {
      args.add("-srcnodata");
      args.add(resolveConstant(meta.getSrcNoData()));
    }
    if (meta.getVrtNoData() != null && !meta.getVrtNoData().isBlank()) {
      args.add("-vrtnodata");
      args.add(resolveConstant(meta.getVrtNoData()));
    }
    if (meta.isAllowProjectionDifference()) {
      args.add("-allow_projection_difference");
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalArgs())));

    gdalClient().buildVrt(inputs, output, remoteAccess, args);
    String inputSummary =
        inputs.stream().map(DatasetRef::value).collect(Collectors.joining(System.lineSeparator()));
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, inputSummary, output.value(), "{}");
  }
}
