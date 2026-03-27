package ch.so.agi.hop.gdal.transform.rasterreproject;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterOutputOptionsSupport;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("--output-format");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    RasterOutputOptionsSupport.addRasterAlgorithmWriteModeArgs(args, meta.getOutputWriteMode());
    if (meta.getSourceCrs() != null && !meta.getSourceCrs().isBlank()) {
      args.add("--src-crs");
      args.add(resolveConstant(meta.getSourceCrs()));
    }
    if (meta.getTargetCrs() != null && !meta.getTargetCrs().isBlank()) {
      args.add("--dst-crs");
      args.add(resolveConstant(meta.getTargetCrs()));
    }
    if (meta.getBounds() != null && !meta.getBounds().isBlank()) {
      args.add("--bbox");
      args.add(BoundsSpec.parse(resolveConstant(meta.getBounds())).toCommaSeparated());
    }
    String sizingMode = resolveConstant(meta.getSizingMode()).trim().toUpperCase();
    if ("RESOLUTION".equals(sizingMode)) {
      args.add("--resolution");
      args.add(resolveConstant(meta.getResolutionX()) + "," + resolveConstant(meta.getResolutionY()));
    } else if ("SIZE".equals(sizingMode)) {
      args.add("--size");
      args.add(resolveConstant(meta.getWidth()) + "," + resolveConstant(meta.getHeight()));
    }
    if (meta.getResamplingMethod() != null && !meta.getResamplingMethod().isBlank()) {
      args.add("--resampling");
      args.add(resolveConstant(meta.getResamplingMethod()));
    }
    if (meta.getSourceNoData() != null && !meta.getSourceNoData().isBlank()) {
      args.add("--src-nodata");
      args.add(resolveConstant(meta.getSourceNoData()));
    }
    if (meta.getDestinationNoData() != null && !meta.getDestinationNoData().isBlank()) {
      args.add("--dst-nodata");
      args.add(resolveConstant(meta.getDestinationNoData()));
    }
    if (meta.isTargetAlignedPixels()) {
      args.add("--target-aligned-pixels");
    }
    LinkedHashMap<String, String> creationOptions = new LinkedHashMap<>();
    RasterOutputOptionsSupport.applyCompressionPreset(
        creationOptions, resolveConstant(meta.getCompressionPreset()));
    creationOptions.putAll(CreationOptionParser.parseKeyValueMap(resolveConstant(meta.getCreationOptions())));
    for (var entry : creationOptions.entrySet()) {
      args.add("--creation-option");
      args.add(entry.getKey() + "=" + entry.getValue());
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalReprojectArgs())));

    gdalClient().rasterReproject(input, output, remoteAccess, args);
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }
}
