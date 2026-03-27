package ch.so.agi.hop.gdal.transform.rasterbuildvrt;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterOutputOptionsSupport;
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

    List<DatasetRef> inputs;
    if ("DIRECTORY_GLOB".equalsIgnoreCase(meta.getInputCollectionMode())) {
      inputs =
          RasterTransformSupport.resolveLocalDirectoryGlobDatasetRefs(
              meta.getDirectoryParameterSource(),
              meta.getInputDirectory(),
              meta.getDirectoryField(),
              meta.getGlobPattern(),
              row,
              getInputRowMeta(),
              this::resolveConstant);
    } else {
      inputs =
          RasterTransformSupport.resolveDatasetRefs(
              meta.getInputInterpretationMode(),
              meta.getInputListValueMode(),
              meta.getInputListValue(),
              meta.getInputListField(),
              row,
              getInputRowMeta(),
              this::resolveConstant);
    }
    DatasetRef output =
        RasterTransformSupport.resolveOutputDatasetRef(
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
    args.add("--output-format");
    args.add("VRT");
    RasterOutputOptionsSupport.addRasterAlgorithmWriteModeArgs(args, meta.getOutputWriteMode());
    if (meta.getResolutionStrategy() != null && !meta.getResolutionStrategy().isBlank()) {
      args.add("--resolution");
      args.add(resolveConstant(meta.getResolutionStrategy()).toLowerCase());
    }
    if (meta.getBounds() != null && !meta.getBounds().isBlank()) {
      args.add("--bbox");
      args.add(BoundsSpec.parse(resolveConstant(meta.getBounds())).toCommaSeparated());
    }
    if (meta.getSrcNoData() != null && !meta.getSrcNoData().isBlank()) {
      args.add("--src-nodata");
      args.add(resolveConstant(meta.getSrcNoData()));
    }
    if (meta.getVrtNoData() != null && !meta.getVrtNoData().isBlank()) {
      args.add("--dst-nodata");
      args.add(resolveConstant(meta.getVrtNoData()));
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalArgs())));

    gdalClient().rasterMosaic(inputs, output, remoteAccess, args);
    String inputSummary =
        inputs.stream().map(DatasetRef::value).collect(Collectors.joining(System.lineSeparator()));
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, inputSummary, output.value(), "{}");
  }
}
