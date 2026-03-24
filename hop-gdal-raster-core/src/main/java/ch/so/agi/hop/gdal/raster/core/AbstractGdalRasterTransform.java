package ch.so.agi.hop.gdal.raster.core;

import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.RowDataUtil;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

public abstract class AbstractGdalRasterTransform<
        M extends AbstractGdalRasterMeta<?, ?>, D extends AbstractGdalRasterData>
    extends BaseTransform<M, D> {

  private final RasterGdalClient gdalClient;

  protected AbstractGdalRasterTransform(
      TransformMeta transformMeta,
      M meta,
      D data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    this(transformMeta, meta, data, copyNr, pipelineMeta, pipeline, new DefaultRasterGdalClient());
  }

  protected AbstractGdalRasterTransform(
      TransformMeta transformMeta,
      M meta,
      D data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline,
      RasterGdalClient gdalClient) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
    this.gdalClient = gdalClient;
  }

  protected RasterGdalClient gdalClient() {
    return gdalClient;
  }

  @Override
  public boolean processRow() throws HopException {
    return OgrBindingsClassLoaderSupport.withPluginContextClassLoader(this::processRowInternal);
  }

  private boolean processRowInternal() throws HopException {
    if (!data.initialized) {
      initializeOutputRowMeta();
    }

    Object[] row = getRow();
    if (row == null) {
      if (getInputRowMeta() == null && !data.syntheticRowProduced) {
        row = RowDataUtil.allocateRowData(0);
        data.syntheticRowProduced = true;
      } else {
        setOutputDone();
        return false;
      }
    }

    RasterTransformResult result;
    try {
      result = executeRasterJob(row);
    } catch (Exception e) {
      if (meta.isFailOnError()) {
        throw e instanceof HopException
            ? (HopException) e
            : new HopTransformException(e.getMessage(), e);
      }
      result =
          RasterTransformResult.failure(
              0L, null, null, normalizeMessage(e), "{\"error\":\"" + escapeJson(normalizeMessage(e)) + "\"}");
    }

    Object[] outputRow = buildOutputRow(row, result);
    putRow(data.outputRowMeta, outputRow);
    return true;
  }

  protected abstract RasterTransformResult executeRasterJob(Object[] row) throws Exception;

  protected String normalizeMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null || current.getMessage().isBlank()
        ? current.getClass().getSimpleName()
        : current.getMessage();
  }

  protected String resolveConstant(String value) {
    return value == null ? "" : resolve(value);
  }

  private void initializeOutputRowMeta() {
    data.outputRowMeta =
        getInputRowMeta() == null ? new RowMeta() : (RowMeta) getInputRowMeta().clone();
    if (meta.isAddResultFields()) {
      data.resultFieldStartIndex = data.outputRowMeta.size();
      RasterResultFields.appendTo(data.outputRowMeta);
    }
    data.initialized = true;
  }

  private Object[] buildOutputRow(Object[] inputRow, RasterTransformResult result) {
    Object[] baseRow = inputRow == null ? RowDataUtil.allocateRowData(0) : inputRow.clone();
    Object[] outputRow = RowDataUtil.resizeArray(baseRow, data.outputRowMeta.size());
    if (meta.isAddResultFields()) {
      int i = data.resultFieldStartIndex;
      outputRow[i] = result.success();
      outputRow[i + 1] = result.message();
      outputRow[i + 2] = result.elapsedMs();
      outputRow[i + 3] = result.input();
      outputRow[i + 4] = result.output();
      outputRow[i + 5] = result.detailsJson();
    }
    return outputRow;
  }

  private static String escapeJson(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
