package ch.so.agi.hop.gdal.transform.rasterwarp;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.BoundsSpec;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.HopGeometrySupport;
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

public class GdalRasterWarpTransform
    extends AbstractGdalRasterTransform<GdalRasterWarpMeta, GdalRasterWarpData> {
  private static final WKTReader WKT_READER = new WKTReader();

  public GdalRasterWarpTransform(
      TransformMeta transformMeta,
      GdalRasterWarpMeta meta,
      GdalRasterWarpData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();
    DatasetRef input = RasterTransformSupport.resolveDatasetRef(
        meta.getInputSourceMode(), meta.getInputValueMode(), meta.getInputValue(), meta.getInputField(), row, getInputRowMeta(), this::resolveConstant);
    DatasetRef output = RasterTransformSupport.resolveOutputDatasetRef(
        meta.getOutputSourceMode(), meta.getOutputValueMode(), meta.getOutputValue(), meta.getOutputField(), row, getInputRowMeta(), this::resolveConstant);
    RemoteAccessSpec remoteAccess = RasterTransformSupport.remoteAccessSpec(
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
    if (meta.getSourceCrs() != null && !meta.getSourceCrs().isBlank()) {
      args.add("-s_srs");
      args.add(resolveConstant(meta.getSourceCrs()));
    }
    if (meta.getTargetCrs() != null && !meta.getTargetCrs().isBlank()) {
      args.add("-t_srs");
      args.add(resolveConstant(meta.getTargetCrs()));
    }
    BoundsSpec bounds = BoundsSpec.parse(resolveConstant(meta.getBounds()));
    if (bounds != null) {
      args.addAll(bounds.toWarpArgs());
    }

    String aoiMode = resolveConstant(meta.getAoiMode());
    if ("INLINE_POLYGON".equalsIgnoreCase(aoiMode) || "BOUNDS_AND_INLINE_POLYGON".equalsIgnoreCase(aoiMode)) {
      String inlinePolygon = resolveConstant(meta.getInlinePolygon());
      if (inlinePolygon != null && !inlinePolygon.isBlank()) {
        DatasetRef cutline = VsiVectorDatasetSupport.writeSingleGeometryDataset(
            WKT_READER.read(inlinePolygon), "cutline", Map.of());
        args.add("-cutline");
        args.add(cutline.value());
        args.add("-crop_to_cutline");
      }
    } else if ("DATASET_LAYER".equalsIgnoreCase(aoiMode) || "BOUNDS_AND_DATASET_LAYER".equalsIgnoreCase(aoiMode)) {
      DatasetRef cutline = new DatasetRef(ch.so.agi.hop.gdal.raster.core.DatasetRefType.fromValue(meta.getAoiDatasetSourceMode()), resolveConstant(meta.getAoiDatasetValue()));
      args.add("-cutline");
      args.add(cutline.value());
      if (meta.getAoiDatasetLayer() != null && !meta.getAoiDatasetLayer().isBlank()) {
        args.add("-cl");
        args.add(resolveConstant(meta.getAoiDatasetLayer()));
      }
      args.add("-crop_to_cutline");
    }

    if (meta.getResolutionX() != null && !meta.getResolutionX().isBlank()
        && meta.getResolutionY() != null && !meta.getResolutionY().isBlank()) {
      args.add("-tr");
      args.add(resolveConstant(meta.getResolutionX()));
      args.add(resolveConstant(meta.getResolutionY()));
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
    for (String creationOption : CreationOptionParser.parse(resolveConstant(meta.getCreationOptions()))) {
      args.add("-co");
      args.add(creationOption);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalWarpArgs())));

    gdalClient().warp(input, output, remoteAccess, args);
    return RasterTransformResult.success(System.currentTimeMillis() - start, input.value(), output.value(), "{}");
  }
}
