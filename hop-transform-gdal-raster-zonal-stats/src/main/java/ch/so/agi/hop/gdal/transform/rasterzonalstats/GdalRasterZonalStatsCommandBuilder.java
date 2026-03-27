package ch.so.agi.hop.gdal.transform.rasterzonalstats;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFieldDefinition;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import ch.so.agi.hop.gdal.raster.core.AdditionalArgsParser;
import ch.so.agi.hop.gdal.raster.core.CreationOptionParser;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.DefaultRasterGdalClient;
import ch.so.agi.hop.gdal.raster.core.RasterOutputOptionsSupport;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.hop.core.row.IRowMeta;

final class GdalRasterZonalStatsCommandBuilder {
  private final ZonesMetadataInspector zonesMetadataInspector;

  GdalRasterZonalStatsCommandBuilder(ZonesMetadataInspector zonesMetadataInspector) {
    this.zonesMetadataInspector = zonesMetadataInspector;
  }

  BuildRequest build(
      GdalRasterZonalStatsMeta meta,
      Object[] row,
      IRowMeta rowMeta,
      Function<String, String> constantResolver)
      throws Exception {
    DatasetRef input =
        RasterTransformSupport.resolveDatasetRef(
            meta.getInputSourceMode(),
            meta.getInputValueMode(),
            meta.getInputValue(),
            meta.getInputField(),
            row,
            rowMeta,
            constantResolver);
    DatasetRef zones =
        RasterTransformSupport.resolveDatasetRef(
            meta.getZonesSourceMode(),
            meta.getZonesValueMode(),
            meta.getZonesValue(),
            meta.getZonesField(),
            row,
            rowMeta,
            constantResolver);
    DatasetRef output =
        RasterTransformSupport.resolveOutputDatasetRef(
            meta.getOutputSourceMode(),
            meta.getOutputValueMode(),
            meta.getOutputValue(),
            meta.getOutputField(),
            row,
            rowMeta,
            constantResolver);
    RemoteAccessSpec remoteAccess =
        RasterTransformSupport.remoteAccessSpec(
            meta.getAuthType(),
            meta.getAuthUsername(),
            meta.getAuthPassword(),
            meta.getBearerToken(),
            meta.getCustomHeaderName(),
            meta.getCustomHeaderValue(),
            meta.getGdalConfigOptions(),
            constantResolver);

    ZoneLayerMetadata zoneLayerMetadata =
        zonesMetadataInspector.inspect(zones, constantResolver.apply(meta.getZonesLayer()), remoteAccess);

    List<String> args = new ArrayList<>();
    if (meta.getOutputFormat() != null && !meta.getOutputFormat().isBlank()) {
      args.add("--output-format");
      args.add(constantResolver.apply(meta.getOutputFormat()));
    }
    RasterOutputOptionsSupport.addVectorAlgorithmWriteModeArgs(args, meta.getOutputWriteMode());

    if (zoneLayerMetadata.layerName() != null && !zoneLayerMetadata.layerName().isBlank()) {
      args.add("--zones-layer");
      args.add(zoneLayerMetadata.layerName());
    }
    String outputLayer = RasterTransformSupport.trimToNull(constantResolver.apply(meta.getOutputLayer()));
    if (outputLayer != null) {
      args.add("--output-layer");
      args.add(outputLayer);
    }
    for (int band : GdalRasterZonalStatsOptions.parseBands(constantResolver.apply(meta.getBands()))) {
      args.add("--band");
      args.add(Integer.toString(band));
    }
    for (String stat : GdalRasterZonalStatsOptions.parseStats(constantResolver.apply(meta.getStats()))) {
      args.add("--stat");
      args.add(stat);
    }
    args.add("--pixels");
    args.add(GdalRasterZonalStatsOptions.normalizePixels(constantResolver.apply(meta.getPixelInclusion())));
    args.add("--strategy");
    args.add(GdalRasterZonalStatsOptions.normalizeStrategy(constantResolver.apply(meta.getStrategy())));

    List<String> includeFields =
        GdalRasterZonalStatsOptions.parseFields(constantResolver.apply(meta.getIncludeFields()));
    if (includeFields.isEmpty()) {
      includeFields = zoneLayerMetadata.fieldNames();
    } else {
      validateIncludedFields(includeFields, zoneLayerMetadata);
    }
    for (String field : includeFields) {
      args.add("--include-field");
      args.add(field);
    }

    appendKeyValueArgs(
        args,
        "--creation-option",
        CreationOptionParser.parseKeyValueMap(constantResolver.apply(meta.getCreationOptions())));
    appendKeyValueArgs(
        args,
        "--layer-creation-option",
        CreationOptionParser.parseKeyValueMap(constantResolver.apply(meta.getLayerCreationOptions())));
    args.addAll(AdditionalArgsParser.parse(constantResolver.apply(meta.getAdditionalArgs())));

    return new BuildRequest(input, zones, output, remoteAccess, List.copyOf(args));
  }

  private static void validateIncludedFields(List<String> includeFields, ZoneLayerMetadata metadata) {
    for (String field : includeFields) {
      if (!metadata.fieldNames().contains(field)) {
        throw new IllegalArgumentException(
            "Zone field was not found in layer '" + metadata.layerName() + "': " + field);
      }
    }
  }

  private static void appendKeyValueArgs(List<String> args, String argName, Map<String, String> values) {
    LinkedHashMap<String, String> ordered = new LinkedHashMap<>(values);
    for (var entry : ordered.entrySet()) {
      args.add(argName);
      args.add(entry.getKey() + "=" + entry.getValue());
    }
  }

  record BuildRequest(
      DatasetRef input,
      DatasetRef zones,
      DatasetRef output,
      RemoteAccessSpec remoteAccess,
      List<String> args) {}
}

interface ZonesMetadataInspector {
  ZoneLayerMetadata inspect(DatasetRef zones, String requestedLayerName, RemoteAccessSpec remoteAccess)
      throws Exception;
}

record ZoneLayerMetadata(String layerName, List<String> fieldNames) {
  ZoneLayerMetadata {
    fieldNames = List.copyOf(fieldNames);
  }
}

final class OgrZonesMetadataInspector implements ZonesMetadataInspector {
  private final DefaultRasterGdalClient bindingAdapter = new DefaultRasterGdalClient();

  @Override
  public ZoneLayerMetadata inspect(
      DatasetRef zones, String requestedLayerName, RemoteAccessSpec remoteAccess) throws Exception {
    String normalizedRequestedLayer = RasterTransformSupport.trimToNull(requestedLayerName);
    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () -> {
          try (OgrDataSource dataSource =
              Ogr.open(
                  bindingAdapter.toBindingDatasetRef(zones),
                  Map.of(),
                  bindingAdapter.toBindingConfig(remoteAccess))) {
            List<OgrLayerDefinition> layers = dataSource.listLayers();
            if (layers.isEmpty()) {
              throw new IllegalArgumentException(
                  "Zones dataset does not contain any vector layers: " + zones.value());
            }

            OgrLayerDefinition selectedLayer;
            if (normalizedRequestedLayer == null) {
              if (layers.size() > 1) {
                throw new IllegalArgumentException(
                    "Zones layer is required because the dataset contains multiple layers: "
                        + zones.value());
              }
              selectedLayer = layers.getFirst();
            } else {
              selectedLayer =
                  layers.stream()
                      .filter(layer -> layer.name().equals(normalizedRequestedLayer))
                      .findFirst()
                      .orElseThrow(
                          () ->
                              new IllegalArgumentException(
                                  "Zones layer was not found in dataset "
                                      + zones.value()
                                      + ": "
                                      + normalizedRequestedLayer));
            }

            return new ZoneLayerMetadata(
                selectedLayer.name(),
                selectedLayer.fields().stream().map(OgrFieldDefinition::name).toList());
          }
        });
  }
}
