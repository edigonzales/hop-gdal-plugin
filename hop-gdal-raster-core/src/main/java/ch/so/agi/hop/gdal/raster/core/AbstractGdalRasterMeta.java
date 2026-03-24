package ch.so.agi.hop.gdal.raster.core;

import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.TransformMeta;

public abstract class AbstractGdalRasterMeta<
        T extends BaseTransform<?, ?>, D extends BaseTransformData>
    extends BaseTransformMeta<T, D> {
  @org.apache.hop.metadata.api.HopMetadataProperty private boolean failOnError = true;
  @org.apache.hop.metadata.api.HopMetadataProperty private boolean addResultFields = true;

  @Override
  public void getFields(
      IRowMeta rowMeta,
      String origin,
      IRowMeta[] info,
      TransformMeta nextTransform,
      IVariables variables,
      IHopMetadataProvider metadataProvider)
      throws HopTransformException {
    if (addResultFields) {
      RasterResultFields.appendTo(rowMeta);
    }
  }

  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  public boolean isAddResultFields() {
    return addResultFields;
  }

  public void setAddResultFields(boolean addResultFields) {
    this.addResultFields = addResultFields;
  }
}
