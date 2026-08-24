import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDataSource;
import ch.so.agi.gdal.ffm.OgrFeature;
import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.gdal.ffm.OgrLayerReader;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class WindowsVectorDistributionSmoke {
  private WindowsVectorDistributionSmoke() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected exactly one argument: <vector-dataset>");
    }

    Path dataset = Path.of(args[0]).toAbsolutePath().normalize();
    if (!Files.isRegularFile(dataset)) {
      throw new IllegalArgumentException("Smoke dataset does not exist: " + dataset);
    }

    System.out.println("java.version=" + System.getProperty("java.version"));
    System.out.println("java.vendor=" + System.getProperty("java.vendor"));
    System.out.println("java.home=" + System.getProperty("java.home"));
    System.out.println("os.name=" + System.getProperty("os.name"));
    System.out.println("os.arch=" + System.getProperty("os.arch"));
    System.out.println("dataset=" + dataset);

    OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
        () -> {
          try (OgrDataSource dataSource = Ogr.open(dataset, Map.of())) {
            List<OgrLayerDefinition> layers = dataSource.listLayers();
            if (layers.isEmpty()) {
              throw new IllegalStateException("OGR smoke dataset has no layers: " + dataset);
            }

            OgrLayerDefinition layer = layers.getFirst();
            if (layer.fields().isEmpty()) {
              throw new IllegalStateException("OGR smoke layer has no fields: " + layer.name());
            }

            try (OgrLayerReader reader = dataSource.openReader(layer.name(), Map.of())) {
              Iterator<OgrFeature> iterator = reader.iterator();
              if (!iterator.hasNext()) {
                throw new IllegalStateException("OGR smoke reader returned no features: " + dataset);
              }

              OgrFeature feature = iterator.next();
              if (feature.attributes().isEmpty()) {
                throw new IllegalStateException("OGR smoke feature has no attributes: " + dataset);
              }
              if (feature.geometry() == null) {
                throw new IllegalStateException("OGR smoke feature has no geometry: " + dataset);
              }

              System.out.println("layer=" + layer.name());
              System.out.println("fields=" + layer.fields().size());
              System.out.println("feature.attributes=" + feature.attributes().size());
              System.out.println("feature.geometry.bytes=" + feature.geometry().ewkb().length);
            }
          }
          return null;
        });

    System.out.println("OK");
  }
}
