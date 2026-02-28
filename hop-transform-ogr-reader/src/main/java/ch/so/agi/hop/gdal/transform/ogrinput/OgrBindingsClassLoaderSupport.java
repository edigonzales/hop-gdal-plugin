package ch.so.agi.hop.gdal.transform.ogrinput;

final class OgrBindingsClassLoaderSupport {

  private OgrBindingsClassLoaderSupport() {}

  @FunctionalInterface
  interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
  }

  @FunctionalInterface
  interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
  }

  static <T, E extends Throwable> T withPluginContextClassLoader(ThrowingSupplier<T, E> supplier)
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

  static <E extends Throwable> void withPluginContextClassLoader(ThrowingRunnable<E> runnable)
      throws E {
    withPluginContextClassLoader(
        () -> {
          runnable.run();
          return null;
        });
  }
}
