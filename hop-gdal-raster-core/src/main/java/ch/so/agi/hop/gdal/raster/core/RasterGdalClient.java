package ch.so.agi.hop.gdal.raster.core;

import java.util.List;

public interface RasterGdalClient {
  String rasterInfo(DatasetRef input, RemoteAccessSpec remoteAccess, List<String> args) throws Exception;

  void rasterConvert(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterClip(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterReproject(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterResize(DatasetRef input, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterMosaic(
      List<DatasetRef> inputs, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;

  void rasterZonalStats(
      DatasetRef rasterInput,
      DatasetRef zonesInput,
      DatasetRef output,
      RemoteAccessSpec remoteAccess,
      List<String> args)
      throws Exception;

  void vectorRasterize(
      DatasetRef vectorInput, DatasetRef output, RemoteAccessSpec remoteAccess, List<String> args)
      throws Exception;
}
