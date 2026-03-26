package ch.so.agi.hop.gdal.transform.rasterconvert;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    String outputFormat = resolveConstant(meta.getOutputFormat());
    if (!outputFormat.isBlank()) {
      args.add("-of");
      args.add(outputFormat);
    }
    String writeMode = meta.getOutputWriteMode();
    if ("OVERWRITE".equalsIgnoreCase(writeMode)) {
      args.add("-overwrite");
    }
    if ("APPEND".equalsIgnoreCase(writeMode)) {
      args.add("-append");
    }
    for (String openOption : CreationOptionParser.parse(resolveConstant(meta.getOpenOptions()))) {
      args.add("-oo");
      args.add(openOption);
    }

    LinkedHashMap<String, String> creationOptions = new LinkedHashMap<>();
    String compressionPreset = resolveConstant(meta.getCompressionPreset());
    if (!compressionPreset.isBlank() && !"DEFAULT".equalsIgnoreCase(compressionPreset)) {
      creationOptions.put("COMPRESS", compressionPreset);
    }
    if (meta.isTiledOutput()) {
      if ("GTIFF".equalsIgnoreCase(outputFormat)) {
        creationOptions.put("TILED", "YES");
      } else if ("COG".equalsIgnoreCase(outputFormat)) {
        creationOptions.put("BLOCKSIZE", "512");
      }
    }
    creationOptions.putAll(CreationOptionParser.parseKeyValueMap(resolveConstant(meta.getCreationOptions())));

    for (var entry : creationOptions.entrySet()) {
      args.add("-co");
      args.add(entry.getKey() + "=" + entry.getValue());
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalTranslateArgs())));

    gdalClient().translate(input, output, remoteAccess, args);
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }
}
