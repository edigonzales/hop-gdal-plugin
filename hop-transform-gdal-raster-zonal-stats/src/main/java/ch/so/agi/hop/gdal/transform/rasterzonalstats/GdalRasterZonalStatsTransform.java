package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.hop.gdal.raster.core.AbstractGdalRasterTransform;
import ch.so.agi.hop.gdal.raster.core.RasterGdalClient;
import ch.so.agi.hop.gdal.raster.core.RasterTransformResult;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

public class GdalRasterZonalStatsTransform
    extends AbstractGdalRasterTransform<GdalRasterZonalStatsMeta, GdalRasterZonalStatsData> {
  private final GdalRasterZonalStatsCommandBuilder commandBuilder;

  public GdalRasterZonalStatsTransform(
      TransformMeta transformMeta,
      GdalRasterZonalStatsMeta meta,
      GdalRasterZonalStatsData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    this(
        transformMeta,
        meta,
        data,
        copyNr,
        pipelineMeta,
        pipeline,
        null,
        new OgrZonesMetadataInspector());
  }

  GdalRasterZonalStatsTransform(
      TransformMeta transformMeta,
      GdalRasterZonalStatsMeta meta,
      GdalRasterZonalStatsData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline,
      RasterGdalClient gdalClient,
      ZonesMetadataInspector zonesMetadataInspector) {
    super(
        transformMeta,
        meta,
        data,
        copyNr,
        pipelineMeta,
        pipeline,
        gdalClient == null ? new ch.so.agi.hop.gdal.raster.core.DefaultRasterGdalClient() : gdalClient);
    this.commandBuilder = new GdalRasterZonalStatsCommandBuilder(zonesMetadataInspector);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();
    GdalRasterZonalStatsCommandBuilder.BuildRequest request =
        commandBuilder.build(meta, row, getInputRowMeta(), this::resolveConstant);
    gdalClient()
        .rasterZonalStats(
            request.input(), request.zones(), request.output(), request.remoteAccess(), request.args());
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, request.input().value(), request.output().value(), "{}");
  }
}
