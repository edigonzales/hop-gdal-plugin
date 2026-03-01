package ch.so.agi.hop.gdal.ogr.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.URLClassLoader;
import org.junit.jupiter.api.Test;

class OgrBindingsClassLoaderSupportTest {

  @Test
  void shouldUsePluginClassLoaderInsideScopeAndRestoreOriginalAfterwards() throws Exception {
    Thread currentThread = Thread.currentThread();
    ClassLoader original = currentThread.getContextClassLoader();
    ClassLoader foreign = new URLClassLoader(new URL[0], original);

    try {
      currentThread.setContextClassLoader(foreign);

      ClassLoader used =
          OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
              () -> Thread.currentThread().getContextClassLoader());

      assertSame(OgrBindingsClassLoaderSupport.class.getClassLoader(), used);
      assertSame(foreign, currentThread.getContextClassLoader());
    } finally {
      currentThread.setContextClassLoader(original);
    }
  }

  @Test
  void shouldRestoreOriginalClassLoaderWhenScopedCodeThrows() {
    Thread currentThread = Thread.currentThread();
    ClassLoader original = currentThread.getContextClassLoader();
    ClassLoader foreign = new URLClassLoader(new URL[0], original);

    try {
      currentThread.setContextClassLoader(foreign);

      IllegalStateException thrown =
          assertThrows(
              IllegalStateException.class,
              () ->
                  OgrBindingsClassLoaderSupport.withPluginContextClassLoader(
                      () -> {
                        throw new IllegalStateException("boom");
                      }));

      assertEquals("boom", thrown.getMessage());
      assertSame(foreign, currentThread.getContextClassLoader());
    } finally {
      currentThread.setContextClassLoader(original);
    }
  }
}
