package ch.so.agi.hop.gdal.transform.ogrinput;

import ch.so.agi.gdal.ffm.OgrLayerDefinition;
import ch.so.agi.gdal.ffm.OgrOpenOptions;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class OgrInputDialog extends BaseTransformDialog {

  private static final Class<?> PKG = OgrInputMeta.class;
  private static final String OGR_CLASS_NAME = "ch.so.agi.gdal.ffm.Ogr";

  private final OgrInputMeta input;

  private TextVar wFileName;
  private Button wbFileName;
  private ComboVar wLayerName;
  private Button wbLoadLayers;
  private Text wAvailableFieldsPreview;
  private Button wbRefreshFields;

  private Button wIncludeFid;
  private Text wFidFieldName;
  private Text wGeometryFieldName;
  private TextVar wSelectedAttributes;
  private TextVar wAttributeFilter;
  private TextVar wBbox;
  private TextVar wPolygonWkt;
  private TextVar wFeatureLimit;
  private TextVar wAllowedDrivers;
  private TextVar wOpenOptions;

  public OgrInputDialog(
      Shell parent, IVariables variables, OgrInputMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(860, 760);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();
    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "OgrInputDialog.Shell.Title"));

    int margin = PropsUi.getMargin();

    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.TransformName.Label"));
    wlTransformName.setToolTipText(BaseMessages.getString(PKG, "System.TransformName.Tooltip"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(props.getMiddlePct(), -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);

    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    wTransformName.addModifyListener(e -> input.setChanged());
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(props.getMiddlePct(), 0);
    fdTransformName.right = new FormAttachment(100, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    wTransformName.setLayoutData(fdTransformName);

    Button wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdMain = new FormData();
    fdMain.left = new FormAttachment(0, 0);
    fdMain.top = new FormAttachment(wTransformName, margin * 2);
    fdMain.right = new FormAttachment(100, 0);
    fdMain.bottom = new FormAttachment(wOk, -margin * 2);

    OgrInputDialogComposite content =
        new OgrInputDialogComposite(shell, SWT.NONE, variables, props.getMiddlePct());
    content.setLayoutData(fdMain);

    wFileName = content.getFileName();
    wbFileName = content.getBrowseFileButton();
    wLayerName = content.getLayerName();
    wbLoadLayers = content.getLoadLayersButton();
    wAvailableFieldsPreview = content.getAvailableFieldsPreview();
    wbRefreshFields = content.getRefreshFieldsButton();
    wSelectedAttributes = content.getSelectedAttributes();
    wAttributeFilter = content.getAttributeFilter();
    wBbox = content.getBbox();
    wPolygonWkt = content.getPolygonWkt();
    wFeatureLimit = content.getFeatureLimit();
    wAllowedDrivers = content.getAllowedDrivers();
    wOpenOptions = content.getOpenOptions();
    wIncludeFid = content.getIncludeFid();
    wFidFieldName = content.getFidFieldName();
    wGeometryFieldName = content.getGeometryFieldName();

    wFileName.addModifyListener(
        e -> {
          input.setChanged();
          resetAvailableFieldsPreview();
        });
    wbFileName.addListener(SWT.Selection, e -> browseFile(wFileName));

    wLayerName.addModifyListener(
        e -> {
          input.setChanged();
          resetAvailableFieldsPreview();
        });
    wbLoadLayers.addListener(SWT.Selection, e -> loadLayersAndPreview(true));
    wbRefreshFields.addListener(SWT.Selection, e -> refreshFieldsPreview(true));

    wSelectedAttributes.addModifyListener(e -> input.setChanged());
    wAttributeFilter.addModifyListener(e -> input.setChanged());
    wBbox.addModifyListener(e -> input.setChanged());
    wPolygonWkt.addModifyListener(e -> input.setChanged());
    wFeatureLimit.addModifyListener(e -> input.setChanged());
    wAllowedDrivers.addModifyListener(
        e -> {
          input.setChanged();
          resetAvailableFieldsPreview();
        });
    wOpenOptions.addModifyListener(
        e -> {
          input.setChanged();
          resetAvailableFieldsPreview();
        });
    wIncludeFid.addListener(
        SWT.Selection,
        e -> {
          input.setChanged();
          enableDisableControls();
        });
    wFidFieldName.addModifyListener(e -> input.setChanged());
    wGeometryFieldName.addModifyListener(e -> input.setChanged());

    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());

    getData();
    if (isGdalBindingsAvailable()) {
      refreshFieldsPreview(false);
    } else {
      disableSchemaButtons();
      showRuntimePrerequisitePreview();
    }
    enableDisableControls();
    input.setChanged(changed);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void browseFile(TextVar target) {
    FileDialog dialog = new FileDialog(shell, SWT.OPEN);
    String current = target.getText();
    if (!Utils.isEmpty(current)) {
      dialog.setFilterPath(current);
    }
    String selected = dialog.open();
    if (selected != null) {
      target.setText(selected);
      if (isGdalBindingsAvailable()) {
        loadLayersAndPreview(false);
      } else {
        showRuntimePrerequisitePreview();
      }
    }
  }

  private void enableDisableControls() {
    boolean includeFid = wIncludeFid.getSelection();
    wFidFieldName.setEnabled(includeFid);
  }

  private void getData() {
    wFileName.setText(Utils.isEmpty(input.getFileName()) ? "" : input.getFileName());
    wLayerName.setText(Utils.isEmpty(input.getLayerName()) ? "" : input.getLayerName());
    wIncludeFid.setSelection(input.isIncludeFid());
    wFidFieldName.setText(Utils.isEmpty(input.getFidFieldName()) ? "fid" : input.getFidFieldName());
    wGeometryFieldName.setText(
        Utils.isEmpty(input.getGeometryFieldName()) ? "geometry" : input.getGeometryFieldName());
    wSelectedAttributes.setText(
        Utils.isEmpty(input.getSelectedAttributes()) ? "" : input.getSelectedAttributes());
    wAttributeFilter.setText(
        Utils.isEmpty(input.getAttributeFilter()) ? "" : input.getAttributeFilter());
    wBbox.setText(Utils.isEmpty(input.getBbox()) ? "" : input.getBbox());
    wPolygonWkt.setText(Utils.isEmpty(input.getPolygonWkt()) ? "" : input.getPolygonWkt());
    wFeatureLimit.setText(Utils.isEmpty(input.getFeatureLimit()) ? "" : input.getFeatureLimit());
    wAllowedDrivers.setText(
        Utils.isEmpty(input.getAllowedDrivers()) ? "" : input.getAllowedDrivers());
    wOpenOptions.setText(Utils.isEmpty(input.getOpenOptions()) ? "" : input.getOpenOptions());
    resetAvailableFieldsPreview();
    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    transformName = wTransformName.getText();

    input.setFileName(wFileName.getText());
    input.setLayerName(wLayerName.getText());
    input.setIncludeFid(wIncludeFid.getSelection());
    input.setFidFieldName(wFidFieldName.getText());
    input.setGeometryFieldName(wGeometryFieldName.getText());
    input.setSelectedAttributes(wSelectedAttributes.getText());
    input.setAttributeFilter(wAttributeFilter.getText());
    input.setBbox(wBbox.getText());
    input.setPolygonWkt(wPolygonWkt.getText());
    input.setFeatureLimit(wFeatureLimit.getText());
    input.setAllowedDrivers(wAllowedDrivers.getText());
    input.setOpenOptions(wOpenOptions.getText());

    dispose();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void loadLayersAndPreview(boolean showErrors) {
    try {
      List<OgrLayerDefinition> layers = loadLayerDefinitions();
      populateLayerCombo(layers);
      refreshFieldsPreview(false);
    } catch (Throwable e) {
      if (showErrors) {
        showSchemaReadError(e);
      } else if (isRuntimePrerequisiteError(e)) {
        showRuntimePrerequisitePreview();
      } else {
        resetAvailableFieldsPreview();
      }
    }
  }

  private void refreshFieldsPreview(boolean showErrors) {
    try {
      List<OgrLayerDefinition> layers = loadLayerDefinitions();
      if (layers.isEmpty()) {
        wAvailableFieldsPreview.setText(
            BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.NoLayers"));
        return;
      }

      OgrLayerDefinition selectedLayer = OgrSchemaProbe.resolveLayer(layers, resolveUiValue(wLayerName.getText()));
      if (!selectedLayer.name().equals(wLayerName.getText())) {
        wLayerName.setText(selectedLayer.name());
      }
      wAvailableFieldsPreview.setText(OgrSchemaProbe.formatFieldPreview(selectedLayer));
    } catch (Throwable e) {
      if (showErrors) {
        showSchemaReadError(e);
      } else if (isRuntimePrerequisiteError(e)) {
        showRuntimePrerequisitePreview();
      } else {
        resetAvailableFieldsPreview();
      }
    }
  }

  private List<OgrLayerDefinition> loadLayerDefinitions() {
    String resolvedFileName = resolveUiValue(wFileName.getText());
    if (resolvedFileName.isBlank()) {
      throw new IllegalArgumentException(
          BaseMessages.getString(PKG, "OgrInputDialog.Error.FileRequired"));
    }

    return OgrSchemaProbe.readLayers(Path.of(resolvedFileName), resolveOpenOptionsForProbe());
  }

  private Map<String, String> resolveOpenOptionsForProbe() {
    Map<String, String> openOptions =
        new LinkedHashMap<>(
            OgrInputOptionsUtil.parseKeyValueOptions(resolveUiValue(wOpenOptions.getText())));

    String allowedDrivers = resolveUiValue(wAllowedDrivers.getText());
    if (!allowedDrivers.isBlank()) {
      openOptions.put(OgrOpenOptions.ALLOWED_DRIVERS, allowedDrivers);
    }
    return openOptions;
  }

  private void populateLayerCombo(List<OgrLayerDefinition> layers) {
    String currentLayerText = resolveUiValue(wLayerName.getText());
    wLayerName.removeAll();
    for (OgrLayerDefinition layer : layers) {
      wLayerName.add(layer.name());
    }

    if (layers.isEmpty()) {
      wLayerName.setText("");
      return;
    }

    try {
      OgrLayerDefinition selectedLayer = OgrSchemaProbe.resolveLayer(layers, currentLayerText);
      wLayerName.setText(selectedLayer.name());
    } catch (IllegalArgumentException ignored) {
      wLayerName.setText(layers.getFirst().name());
    }
  }

  private void resetAvailableFieldsPreview() {
    if (wAvailableFieldsPreview != null && !wAvailableFieldsPreview.isDisposed()) {
      wAvailableFieldsPreview.setText(
          BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.Empty"));
    }
  }

  private void showRuntimePrerequisitePreview() {
    if (wAvailableFieldsPreview != null && !wAvailableFieldsPreview.isDisposed()) {
      if (isJavaTooOld()) {
        wAvailableFieldsPreview.setText(
            BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.JavaTooOld"));
      } else {
        wAvailableFieldsPreview.setText(
            BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.BindingsMissing"));
      }
    }
  }

  private void disableSchemaButtons() {
    if (wbLoadLayers != null && !wbLoadLayers.isDisposed()) {
      wbLoadLayers.setEnabled(false);
    }
    if (wbRefreshFields != null && !wbRefreshFields.isDisposed()) {
      wbRefreshFields.setEnabled(false);
    }
  }

  private boolean isGdalBindingsAvailable() {
    try {
      Class.forName(OGR_CLASS_NAME, false, getClass().getClassLoader());
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private boolean isRuntimePrerequisiteError(Throwable throwable) {
    if (isJavaVersionMismatchError(throwable)) {
      return true;
    }

    Throwable current = throwable;
    while (current != null) {
      if (current instanceof NoClassDefFoundError || current instanceof ClassNotFoundException) {
        String message = current.getMessage();
        if (message != null
            && (message.contains("ch/so/agi/gdal/ffm")
                || message.contains("ch.so.agi.gdal.ffm"))) {
          return true;
        }
      }
      current = current.getCause();
    }
    return !isGdalBindingsAvailable();
  }

  private boolean isJavaVersionMismatchError(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof UnsupportedClassVersionError) {
        return true;
      }
      String message = current.getMessage();
      if (message != null
          && (message.contains("class file version") || message.contains("UnsupportedClassVersionError"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private boolean isJavaTooOld() {
    try {
      int feature = Runtime.version().feature();
      return feature < 23;
    } catch (Throwable ignored) {
      return true;
    }
  }

  private String resolveUiValue(String value) {
    return value == null ? "" : variables.resolve(value).trim();
  }

  private void showSchemaReadError(Throwable e) {
    String messageKey;
    if (isJavaVersionMismatchError(e) || isJavaTooOld()) {
      messageKey = "OgrInputDialog.Error.JavaTooOld.Message";
    } else if (isRuntimePrerequisiteError(e)) {
      messageKey = "OgrInputDialog.Error.BindingsMissing.Message";
    } else {
      messageKey = "OgrInputDialog.Error.ReadSchema.Message";
    }
    new ErrorDialog(
        shell,
        BaseMessages.getString(PKG, "OgrInputDialog.Error.ReadSchema.Title"),
        BaseMessages.getString(PKG, messageKey),
        e);
  }
}
