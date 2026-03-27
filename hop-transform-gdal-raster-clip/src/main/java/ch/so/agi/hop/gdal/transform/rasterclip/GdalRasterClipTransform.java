package ch.so.agi.hop.gdal.transform.rasterclip;

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

public class GdalRasterClipTransform
    extends AbstractGdalRasterTransform<GdalRasterClipMeta, GdalRasterClipData> {
  public GdalRasterClipTransform(
      TransformMeta transformMeta,
      GdalRasterClipMeta meta,
      GdalRasterClipData data,
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

    String clipMode = resolveConstant(meta.getClipMode()).trim().toUpperCase();
    String clipParameterSourceMode = meta.getClipParameterSourceMode();
    List<String> args = new ArrayList<>();
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("--output-format");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    RasterOutputOptionsSupport.addRasterAlgorithmWriteModeArgs(args, meta.getOutputWriteMode());
    LinkedHashMap<String, String> creationOptions = new LinkedHashMap<>();
    RasterOutputOptionsSupport.applyCompressionPreset(
        creationOptions, resolveConstant(meta.getCompressionPreset()));
    creationOptions.putAll(CreationOptionParser.parseKeyValueMap(resolveConstant(meta.getCreationOptions())));
    for (var entry : creationOptions.entrySet()) {
      args.add("--creation-option");
      args.add(entry.getKey() + "=" + entry.getValue());
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalClipArgs())));

    switch (clipMode) {
      case "PIXEL_WINDOW" -> {
        args.add("--window");
        args.add(
            normalizeCommaSeparatedValue(
                RasterTransformSupport.resolveRequiredValue(
                    clipParameterSourceMode,
                    meta.getPixelWindow(),
                    meta.getPixelWindowField(),
                    row,
                    getInputRowMeta(),
                    this::resolveConstant,
                    "Pixel window")));
      }
      case "BOUNDING_BOX" -> {
        args.add("--bbox");
        args.add(
            BoundsSpec.parse(
                    RasterTransformSupport.resolveRequiredValue(
                        clipParameterSourceMode,
                        meta.getBounds(),
                        meta.getBoundsField(),
                        row,
                        getInputRowMeta(),
                        this::resolveConstant,
                        "Bounds"))
                .toCommaSeparated());
        if (meta.isAddAlpha()) {
          args.add("--add-alpha");
        }
      }
      case "INLINE_GEOMETRY" -> {
        args.add("--geometry");
        args.add(
            RasterTransformSupport.resolveRequiredValue(
                clipParameterSourceMode,
                meta.getInlineGeometry(),
                meta.getInlineGeometryField(),
                row,
                getInputRowMeta(),
                this::resolveConstant,
                "Inline geometry"));
        if (meta.isAddAlpha()) {
          args.add("--add-alpha");
        }
      }
      case "TEMPLATE_DATASET" -> {
        String templateDatasetValue =
            RasterTransformSupport.resolveRequiredValue(
                clipParameterSourceMode,
                meta.getTemplateDatasetValue(),
                meta.getTemplateDatasetField(),
                row,
                getInputRowMeta(),
                this::resolveConstant,
                "Template dataset");
        DatasetRef templateDataset =
            "FIELD".equalsIgnoreCase(clipParameterSourceMode)
                ? inferDatasetRef(templateDatasetValue)
                : RasterTransformSupport.resolveConstantDatasetRef(
                    resolveConstant(meta.getTemplateSourceMode()), templateDatasetValue);
        args.add("--like");
        args.add(templateDataset.toGdalIdentifier());
        String templateLayer = RasterTransformSupport.trimToNull(resolveConstant(meta.getTemplateLayerName()));
        if (templateLayer != null && !templateLayer.isBlank()) {
          args.add("--like-layer");
          args.add(templateLayer);
        }
        if (meta.isAddAlpha()) {
          args.add("--add-alpha");
        }
      }
      default -> throw new IllegalArgumentException("Unsupported clip mode: " + clipMode);
    }

    gdalClient().rasterClip(input, output, remoteAccess, args);

    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }

  private static String normalizeCommaSeparatedValue(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Pixel window is required");
    }
    String[] parts = value.split("[,;]");
    if (parts.length != 4) {
      throw new IllegalArgumentException("Pixel window must contain four values: xoff,yoff,xsize,ysize");
    }
    return String.join(",", parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
  }

  private static DatasetRef inferDatasetRef(String value) {
    return RasterTransformSupport.inferDatasetRef(value);
  }
}
