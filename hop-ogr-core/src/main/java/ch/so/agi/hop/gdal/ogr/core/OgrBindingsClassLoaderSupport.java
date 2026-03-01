package ch.so.agi.hop.gdal.ogr.core;

public final class OgrBindingsClassLoaderSupport {

  private OgrBindingsClassLoaderSupport() {}

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
  }

  @FunctionalInterface
  public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
  }

  public static <T, E extends Throwable> T withPluginContextClassLoader(ThrowingSupplier<T, E> supplier)
      throws E {
    ClassLoader pluginClassLoader = OgrBindingsClassLoaderSupport.class.getClassLoader();
    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();

    if (pluginClassLoader == null || pluginClassLoader == originalClassLoader) {
      return supplier.get();
    }

    currentThread.setContextClassLoader(pluginClassLoader);
    try {
      return supplier.get();
    } finally {
      currentThread.setContextClassLoader(originalClassLoader);
    }
  }

  public static <E extends Throwable> void withPluginContextClassLoader(ThrowingRunnable<E> runnable)
      throws E {
    withPluginContextClassLoader(
        () -> {
          runnable.run();
          return null;
        });
  }
}
