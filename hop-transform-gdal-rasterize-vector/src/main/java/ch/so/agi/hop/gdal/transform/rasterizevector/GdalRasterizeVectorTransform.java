package ch.so.agi.hop.gdal.transform.rasterizevector;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.locationtech.jts.geom.Geometry;

public class GdalRasterizeVectorTransform
    extends AbstractGdalRasterTransform<GdalRasterizeVectorMeta, GdalRasterizeVectorData> {

  public GdalRasterizeVectorTransform(
      TransformMeta transformMeta,
      GdalRasterizeVectorMeta meta,
      GdalRasterizeVectorData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  protected RasterTransformResult executeRasterJob(Object[] row) throws Exception {
    long start = System.currentTimeMillis();

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

    ResolvedVectorInput input = resolveVectorInput(row);
    List<String> args = new ArrayList<>();

    if (input.layerName() != null && !input.layerName().isBlank()) {
      args.add("-l");
      args.add(input.layerName());
    }
    if ("ATTRIBUTE_FIELD".equalsIgnoreCase(meta.getBurnStrategy())) {
      args.add("-a");
      args.add(input.burnAttributeName());
    } else {
      args.add("-burn");
      args.add(resolveConstant(meta.getBurnValue()));
    }

    BoundsSpec bounds = BoundsSpec.parse(resolveConstant(meta.getBounds()));
    if (bounds != null) {
      args.add("-te");
      args.add(Double.toString(bounds.minX()));
      args.add(Double.toString(bounds.minY()));
      args.add(Double.toString(bounds.maxX()));
      args.add(Double.toString(bounds.maxY()));
    }
    if (meta.getCrs() != null && !meta.getCrs().isBlank()) {
      args.add("-a_srs");
      args.add(resolveConstant(meta.getCrs()));
    }
    appendGridArgs(args);
    if (meta.getOutputDataType() != null && !meta.getOutputDataType().isBlank()) {
      args.add("-ot");
      args.add(resolveConstant(meta.getOutputDataType()));
    }
    if (meta.getInitValue() != null && !meta.getInitValue().isBlank()) {
      args.add("-init");
      args.add(resolveConstant(meta.getInitValue()));
    }
    if (meta.getNoDataValue() != null && !meta.getNoDataValue().isBlank()) {
      args.add("-a_nodata");
      args.add(resolveConstant(meta.getNoDataValue()));
    }
    if (meta.isAllTouched()) {
      args.add("-at");
    }
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("-of");
      args.add(resolveConstant(meta.getOutputFormat()));
    }
    for (String creationOption : CreationOptionParser.parse(resolveConstant(meta.getCreationOptions()))) {
      args.add("-co");
      args.add(creationOption);
    }
    args.addAll(AdditionalArgsParser.parse(resolveConstant(meta.getAdditionalArgs())));

    gdalClient().rasterize(input.datasetRef(), output, remoteAccess, args);
    return RasterTransformResult.success(
        System.currentTimeMillis() - start, input.datasetRef().value(), output.value(), "{}");
  }

  private ResolvedVectorInput resolveVectorInput(Object[] row) throws Exception {
    if ("HOP_GEOMETRY_FIELD".equalsIgnoreCase(meta.getVectorInputMode())) {
      IRowMeta rowMeta = getInputRowMeta();
      if (rowMeta == null) {
        throw new IllegalArgumentException("Input row metadata is required for Hop geometry field mode");
      }
      int geometryIndex = rowMeta.indexOfValue(meta.getGeometryField());
      if (geometryIndex < 0) {
        throw new IllegalArgumentException("Geometry field was not found: " + meta.getGeometryField());
      }

      Geometry geometry = HopGeometrySupport.toJtsGeometry(row[geometryIndex], rowMeta.getValueMeta(geometryIndex));
      if (geometry == null) {
        throw new IllegalArgumentException("Geometry field resolved to null");
      }

      Map<String, Object> attributes = new LinkedHashMap<>();
      String burnAttributeName = null;
      if ("ATTRIBUTE_FIELD".equalsIgnoreCase(meta.getBurnStrategy())) {
        int burnFieldIndex = rowMeta.indexOfValue(meta.getBurnField());
        if (burnFieldIndex < 0) {
          throw new IllegalArgumentException("Burn attribute field was not found: " + meta.getBurnField());
        }
        burnAttributeName = sanitizeFieldName(meta.getBurnField());
        attributes.put(burnAttributeName, row[burnFieldIndex]);
      }

      String layerName =
          meta.getLayerName() == null || meta.getLayerName().isBlank()
              ? "features"
              : resolveConstant(meta.getLayerName());
      DatasetRef datasetRef =
          VsiVectorDatasetSupport.writeSingleGeometryDataset(geometry, layerName, attributes);
      return new ResolvedVectorInput(datasetRef, layerName, burnAttributeName);
    }

    DatasetRef datasetRef =
        RasterTransformSupport.resolveDatasetRef(
            meta.getInputSourceMode(),
            meta.getInputValueMode(),
            meta.getInputValue(),
            meta.getInputField(),
            row,
            getInputRowMeta(),
            this::resolveConstant);
    return new ResolvedVectorInput(
        datasetRef, blankToNull(resolveConstant(meta.getLayerName())), resolveConstant(meta.getBurnField()));
  }

  private void appendGridArgs(List<String> args) {
    if ("BOUNDS_SIZE".equalsIgnoreCase(meta.getGridMode())) {
      requireValue(meta.getWidth(), "Width");
      requireValue(meta.getHeight(), "Height");
      args.add("-ts");
      args.add(resolveConstant(meta.getWidth()));
      args.add(resolveConstant(meta.getHeight()));
      return;
    }

    requireValue(meta.getResolutionX(), "Resolution X");
    requireValue(meta.getResolutionY(), "Resolution Y");
    args.add("-tr");
    args.add(resolveConstant(meta.getResolutionX()));
    args.add(resolveConstant(meta.getResolutionY()));
  }

  private static void requireValue(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(label + " is required");
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  private static String sanitizeFieldName(String value) {
    String sanitized = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9_]+", "_");
    if (sanitized.isBlank()) {
      return "burn_value";
    }
    if (Character.isDigit(sanitized.charAt(0))) {
      return "f_" + sanitized;
    }
    return sanitized;
  }

  private record ResolvedVectorInput(DatasetRef datasetRef, String layerName, String burnAttributeName) {}
}
