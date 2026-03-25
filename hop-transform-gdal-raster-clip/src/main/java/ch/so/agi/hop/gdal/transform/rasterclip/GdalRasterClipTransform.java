package ch.so.agi.hop.gdal.transform.rasterclip;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.DatasetRefType;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import ch.so.agi.hop.gdal.raster.core.VsiVectorDatasetSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.locationtech.jts.io.WKTReader;

public class GdalRasterClipTransform
    extends AbstractGdalRasterTransform<GdalRasterClipMeta, GdalRasterClipData> {
  private static final WKTReader WKT_READER = new WKTReader();

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

    String clipMode = resolveConstant(meta.getClipMode()).trim().toUpperCase();
    List<String> args = new ArrayList<>();
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("-of");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    if (meta.isOverwrite()) {
      args.add("-overwrite");
    }
    for (String creationOption : CreationOptionParser.parse(resolveConstant(meta.getCreationOptions()))) {
      args.add("-co");
      args.add(creationOption);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalClipArgs())));

    switch (clipMode) {
      case "PIXEL_WINDOW" -> {
        appendWindowArg(args, resolveConstant(meta.getPixelWindow()));
        gdalClient().translate(input, output, remoteAccess, args);
      }
      case "BOUNDING_BOX" -> {
        BoundsSpec bounds = BoundsSpec.parse(resolveConstant(meta.getBounds()));
        args.addAll(bounds.toWarpArgs());
        if (meta.isAddAlpha()) {
          args.add("-dstalpha");
        }
        gdalClient().warp(input, output, remoteAccess, args);
      }
      case "INLINE_GEOMETRY" -> {
        DatasetRef cutline =
            VsiVectorDatasetSupport.writeSingleGeometryDataset(
                WKT_READER.read(resolveConstant(meta.getInlineGeometry())), "clip", Map.of());
        args.add("-cutline");
        args.add(cutline.value());
        args.add("-crop_to_cutline");
        if (meta.isAddAlpha()) {
          args.add("-dstalpha");
        }
        gdalClient().warp(input, output, remoteAccess, args);
      }
      case "TEMPLATE_DATASET" -> {
        DatasetRef templateDataset =
            new DatasetRef(
                DatasetRefType.fromValue(resolveConstant(meta.getTemplateSourceMode())),
                resolveConstant(meta.getTemplateDatasetValue()));
        args.add("-cutline");
        args.add(templateDataset.value());
        if (meta.getTemplateLayerName() != null && !meta.getTemplateLayerName().isBlank()) {
          args.add("-cl");
          args.add(resolveConstant(meta.getTemplateLayerName()));
        }
        args.add("-crop_to_cutline");
        if (meta.isAddAlpha()) {
          args.add("-dstalpha");
        }
        gdalClient().warp(input, output, remoteAccess, args);
      }
      default -> throw new IllegalArgumentException("Unsupported clip mode: " + clipMode);
    }

    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }

  private static void appendWindowArg(List<String> args, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Pixel window is required");
    }
    String[] parts = value.split("[,;]");
    if (parts.length != 4) {
      throw new IllegalArgumentException("Pixel window must contain four values: xoff,yoff,xsize,ysize");
    }
    args.add("-srcwin");
    for (String part : parts) {
      args.add(part.trim());
    }
  }
}
