package ch.so.agi.hop.gdal.raster.core;

import java.util.List;

public interface RasterGdalClient {
  String info(DatasetRef input, RemoteAccessSpec remoteAccess, List<String> args) throws Exception;

  void translate(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void warp(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void buildVrt(
      List<DatasetRef> inputs, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterize(DatasetRef vectorInput, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;
}
