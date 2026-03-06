package ch.so.agi.hop.gdal.transform.ogroutput;

import ch.so.agi.gdal.ffm.Ogr;
import ch.so.agi.gdal.ffm.OgrDriverInfo;
import ch.so.agi.hop.gdal.ogr.core.OgrBindingsClassLoaderSupport;
import java.util.List;
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

public class OgrOutputDialog extends BaseTransformDialog {

  private static final Class<?> PKG = OgrOutputMeta.class;
  private static final String OGR_CLASS_NAME = "ch.so.agi.gdal.ffm.Ogr";

  private final OgrOutputMeta input;

  private TextVar wFileName;
  private Button wbFileName;
  private ComboVar wFormat;
  private TextVar wLayerName;
  private ComboVar wWriteMode;
  private ComboVar wGeometryField;
  private TextVar wSelectedAttributes;
  private TextVar wDatasetCreationOptions;
  private TextVar wLayerCreationOptions;
  private ComboVar wForceGeometryType;

  private Button wOk;

  public OgrOutputDialog(
      Shell parent, IVariables variables, OgrOutputMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(860, 620);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();
    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "OgrOutputDialog.Shell.Title"));

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

    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    FormData fdMain = new FormData();
    fdMain.left = new FormAttachment(0, 0);
    fdMain.top = new FormAttachment(wTransformName, margin * 2);
    fdMain.right = new FormAttachment(100, 0);
    fdMain.bottom = new FormAttachment(wOk, -margin * 2);

    OgrOutputDialogComposite content =
        new OgrOutputDialogComposite(shell, SWT.NONE, variables, props.getMiddlePct());
    content.setLayoutData(fdMain);

    wFileName = content.getFileName();
    wbFileName = content.getBrowseFileButton();
    wFormat = content.getFormat();
    wLayerName = content.getLayerName();
    wWriteMode = content.getWriteMode();
    wGeometryField = content.getGeometryField();
    wSelectedAttributes = content.getSelectedAttributes();
    wDatasetCreationOptions = content.getDatasetCreationOptions();
    wLayerCreationOptions = content.getLayerCreationOptions();
    wForceGeometryType = content.getForceGeometryType();

    wbFileName.addListener(SWT.Selection, e -> browseFile(wFileName));

    wFileName.addModifyListener(e -> input.setChanged());
    wFormat.addModifyListener(e -> input.setChanged());
    wLayerName.addModifyListener(e -> input.setChanged());
    wWriteMode.addModifyListener(e -> input.setChanged());
    wGeometryField.addModifyListener(e -> input.setChanged());
    wSelectedAttributes.addModifyListener(e -> input.setChanged());
    wDatasetCreationOptions.addModifyListener(e -> input.setChanged());
    wLayerCreationOptions.addModifyListener(e -> input.setChanged());
    wForceGeometryType.addModifyListener(e -> input.setChanged());

    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());

    loadWriteModeValues();
    loadForceGeometryTypeValues();
    loadGeometryFields();
    getData();
    loadFormats();

    input.setChanged(changed);
    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  private void loadWriteModeValues() {
    wWriteMode.removeAll();
    wWriteMode.add("FAIL_IF_EXISTS");
    wWriteMode.add("OVERWRITE");
    wWriteMode.add("APPEND");
  }

  private void loadForceGeometryTypeValues() {
    wForceGeometryType.removeAll();
    for (String value : OgrOutputOptionsUtil.FORCE_GEOMETRY_VALUES) {
      wForceGeometryType.add(value);
    }
  }

  private void loadGeometryFields() {
    wGeometryField.removeAll();
    BaseTransformDialog.getFieldsFromPrevious(variables, wGeometryField, pipelineMeta, transformMeta);
  }

  private void loadFormats() {
    if (!isGdalBindingsAvailable()) {
      disableOkWithBindingsError();
      return;
    }

    try {
      List<OgrDriverInfo> drivers =
          OgrBindingsClassLoaderSupport.withPluginContextClassLoader(Ogr::listWritableVectorDrivers);
      String currentFormat = wFormat.getText();

      wFormat.removeAll();
      for (OgrDriverInfo driver : drivers) {
        wFormat.add(driver.shortName());
      }

      if (drivers.isEmpty()) {
        disableOkWithBindingsError();
        return;
      }

      wFormat.setText(
          OgrOutputOptionsUtil.resolveFormatSelection(
              currentFormat, drivers.stream().map(OgrDriverInfo::shortName).toList()));
      wOk.setEnabled(true);
    } catch (Throwable e) {
      disableOkWithBindingsError();
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "OgrOutputDialog.Error.LoadFormats.Title"),
          BaseMessages.getString(PKG, "OgrOutputDialog.Error.LoadFormats.Message"),
          e);
    }
  }

  private void disableOkWithBindingsError() {
    if (wOk != null && !wOk.isDisposed()) {
      wOk.setEnabled(false);
    }
    wFormat.removeAll();
    wFormat.setText("");
  }

  private void browseFile(TextVar target) {
    FileDialog dialog = new FileDialog(shell, SWT.SAVE);
    String current = target.getText();
    if (!Utils.isEmpty(current)) {
      dialog.setFileName(current);
    }
    String selected = dialog.open();
    if (selected != null) {
      target.setText(selected);
    }
  }

  private void getData() {
    wFileName.setText(Utils.isEmpty(input.getFileName()) ? "" : input.getFileName());
    wFormat.setText(Utils.isEmpty(input.getFormat()) ? "" : input.getFormat());
    wLayerName.setText(Utils.isEmpty(input.getLayerName()) ? "" : input.getLayerName());
    wWriteMode.setText(Utils.isEmpty(input.getWriteMode()) ? "FAIL_IF_EXISTS" : input.getWriteMode());
    wGeometryField.setText(Utils.isEmpty(input.getGeometryField()) ? "" : input.getGeometryField());
    wSelectedAttributes.setText(
        Utils.isEmpty(input.getSelectedAttributes()) ? "" : input.getSelectedAttributes());
    wDatasetCreationOptions.setText(
        Utils.isEmpty(input.getDatasetCreationOptions()) ? "" : input.getDatasetCreationOptions());
    wLayerCreationOptions.setText(
        Utils.isEmpty(input.getLayerCreationOptions()) ? "" : input.getLayerCreationOptions());
    wForceGeometryType.setText(
        Utils.isEmpty(input.getForceGeometryType())
            ? OgrOutputOptionsUtil.FORCE_GEOMETRY_AUTO
            : input.getForceGeometryType());
    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    if (Utils.isEmpty(wFileName.getText())
        || Utils.isEmpty(wFormat.getText())
        || Utils.isEmpty(wGeometryField.getText())) {
      return;
    }

    transformName = wTransformName.getText();
    input.setFileName(wFileName.getText());
    input.setFormat(wFormat.getText());
    input.setLayerName(wLayerName.getText());
    input.setWriteMode(wWriteMode.getText());
    input.setGeometryField(wGeometryField.getText());
    input.setSelectedAttributes(wSelectedAttributes.getText());
    input.setDatasetCreationOptions(wDatasetCreationOptions.getText());
    input.setLayerCreationOptions(wLayerCreationOptions.getText());
    input.setForceGeometryType(wForceGeometryType.getText());

    dispose();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private boolean isGdalBindingsAvailable() {
    try {
      Class.forName(OGR_CLASS_NAME, false, getClass().getClassLoader());
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }
}
