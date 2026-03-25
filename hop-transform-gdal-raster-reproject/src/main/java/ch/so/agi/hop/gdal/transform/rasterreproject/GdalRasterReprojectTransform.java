package ch.so.agi.hop.gdal.transform.rasterreproject;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
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

public class GdalRasterReprojectTransform
    extends AbstractGdalRasterTransform<GdalRasterReprojectMeta, GdalRasterReprojectData> {

  public GdalRasterReprojectTransform(
      TransformMeta transformMeta,
      GdalRasterReprojectMeta meta,
      GdalRasterReprojectData data,
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
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("-of");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    if (meta.isOverwrite()) {
      args.add("-overwrite");
    }
    if (meta.getSourceCrs() != null && !meta.getSourceCrs().isBlank()) {
      args.add("-s_srs");
      args.add(resolveConstant(meta.getSourceCrs()));
    }
    if (meta.getTargetCrs() != null && !meta.getTargetCrs().isBlank()) {
      args.add("-t_srs");
      args.add(resolveConstant(meta.getTargetCrs()));
    }
    if (meta.getBounds() != null && !meta.getBounds().isBlank()) {
      args.addAll(BoundsSpec.parse(resolveConstant(meta.getBounds())).toWarpArgs());
    }
    String sizingMode = resolveConstant(meta.getSizingMode()).trim().toUpperCase();
    if ("RESOLUTION".equals(sizingMode)) {
      args.add("-tr");
      args.add(resolveConstant(meta.getResolutionX()));
      args.add(resolveConstant(meta.getResolutionY()));
    } else if ("SIZE".equals(sizingMode)) {
      args.add("-ts");
      args.add(resolveConstant(meta.getWidth()));
      args.add(resolveConstant(meta.getHeight()));
    }
    if (meta.getResamplingMethod() != null && !meta.getResamplingMethod().isBlank()) {
      args.add("-r");
      args.add(resolveConstant(meta.getResamplingMethod()));
    }
    if (meta.getSourceNoData() != null && !meta.getSourceNoData().isBlank()) {
      args.add("-srcnodata");
      args.add(resolveConstant(meta.getSourceNoData()));
    }
    if (meta.getDestinationNoData() != null && !meta.getDestinationNoData().isBlank()) {
      args.add("-dstnodata");
      args.add(resolveConstant(meta.getDestinationNoData()));
    }
    if (meta.isTargetAlignedPixels()) {
      args.add("-tap");
    }
    for (String creationOption : CreationOptionParser.parse(resolveConstant(meta.getCreationOptions()))) {
      args.add("-co");
      args.add(creationOption);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalReprojectArgs())));

    gdalClient().warp(input, output, remoteAccess, args);
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }
}
