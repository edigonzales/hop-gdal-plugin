package ch.so.agi.hop.gdal.transform.rasterizevector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.hop.core.annotations.Transform;
import org.junit.jupiter.api.Test;

class GdalRasterizeVectorClassLoaderGroupTest {

  @Test
  void shouldUseSharedGeometryClassLoaderGroup() {
    Transform annotation = GdalRasterizeVectorMeta.class.getAnnotation(Transform.class);

    assertNotNull(annotation);
    assertEquals("sogeo-geometry", annotation.classLoaderGroup());
  }
}
