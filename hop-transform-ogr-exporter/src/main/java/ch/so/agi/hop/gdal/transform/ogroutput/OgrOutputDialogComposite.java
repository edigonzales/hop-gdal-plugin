package ch.so.agi.hop.gdal.transform.ogroutput;

import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class OgrOutputDialogComposite extends Composite {

  private static final Class<?> PKG = OgrOutputMeta.class;

  private final IVariables variables;
  private final int middlePct;
  private final int margin;

  private TextVar fileName;
  private Button browseFileButton;
  private ComboVar format;
  private TextVar layerName;
  private ComboVar writeMode;
  private ComboVar geometryField;
  private TextVar selectedAttributes;
  private TextVar datasetCreationOptions;
  private TextVar layerCreationOptions;
  private ComboVar forceGeometryType;

  public OgrOutputDialogComposite(Composite parent, int style) {
    this(parent, style, Variables.getADefaultVariableSpace(), 35);
  }

  OgrOutputDialogComposite(Composite parent, int style, IVariables variables, int middlePct) {
    super(parent, style);
    this.variables = variables == null ? Variables.getADefaultVariableSpace() : variables;
    this.middlePct = middlePct;
    this.margin = PropsUi.getMargin();
    buildUi();
  }

  private void buildUi() {
    FormLayout layout = new FormLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    setLayout(layout);
    PropsUi.setLook(this);

    Composite lastRow = null;

    Composite fileRow = createRow(lastRow);
    fileName = new TextVar(variables, fileRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    browseFileButton = new Button(fileRow, SWT.PUSH | SWT.CENTER);
    buildRowControlWithButton(
        fileRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.FileName.Label"),
        fileName,
        browseFileButton,
        BaseMessages.getString(PKG, "System.Button.Browse"));
    lastRow = fileRow;

    Composite formatRow = createRow(lastRow);
    format = new ComboVar(variables, formatRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(formatRow, BaseMessages.getString(PKG, "OgrOutputDialog.Format.Label"), format);
    lastRow = formatRow;

    Composite layerRow = createRow(lastRow);
    layerName = new TextVar(variables, layerRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(layerRow, BaseMessages.getString(PKG, "OgrOutputDialog.LayerName.Label"), layerName);
    lastRow = layerRow;

    Composite writeModeRow = createRow(lastRow);
    writeMode = new ComboVar(variables, writeModeRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        writeModeRow, BaseMessages.getString(PKG, "OgrOutputDialog.WriteMode.Label"), writeMode);
    lastRow = writeModeRow;

    Composite geometryFieldRow = createRow(lastRow);
    geometryField = new ComboVar(variables, geometryFieldRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        geometryFieldRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.GeometryField.Label"),
        geometryField);
    lastRow = geometryFieldRow;

    Composite selectedAttributesRow = createRow(lastRow);
    selectedAttributes = new TextVar(variables, selectedAttributesRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        selectedAttributesRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.SelectedAttributes.Label"),
        selectedAttributes);
    lastRow = selectedAttributesRow;

    Composite datasetOptionsRow = createRow(lastRow);
    datasetCreationOptions = new TextVar(variables, datasetOptionsRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        datasetOptionsRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.DatasetOptions.Label"),
        datasetCreationOptions);
    lastRow = datasetOptionsRow;

    Composite layerOptionsRow = createRow(lastRow);
    layerCreationOptions = new TextVar(variables, layerOptionsRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        layerOptionsRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.LayerOptions.Label"),
        layerCreationOptions);
    lastRow = layerOptionsRow;

    Composite forceGeometryTypeRow = createRow(lastRow);
    forceGeometryType = new ComboVar(variables, forceGeometryTypeRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        forceGeometryTypeRow,
        BaseMessages.getString(PKG, "OgrOutputDialog.ForceGeometryType.Label"),
        forceGeometryType);
  }

  private Composite createRow(Composite underRow) {
    Composite row = new Composite(this, SWT.NONE);
    PropsUi.setLook(row);
    FormLayout rowLayout = new FormLayout();
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    row.setLayout(rowLayout);

    FormData fdRow = new FormData();
    fdRow.left = new FormAttachment(0, 0);
    fdRow.right = new FormAttachment(100, 0);
    fdRow.top = underRow == null ? new FormAttachment(0, 0) : new FormAttachment(underRow, margin);
    row.setLayoutData(fdRow);
    return row;
  }

  private void buildRowControlWithButton(
      Composite row,
      String labelText,
      Control control,
      Button actionButton,
      String buttonLabel) {
    Label label = createRowLabel(row, labelText);

    PropsUi.setLook(actionButton);
    actionButton.setText(buttonLabel);
    FormData fdAction = new FormData();
    fdAction.right = new FormAttachment(100, 0);
    fdAction.top = new FormAttachment(0, 0);
    actionButton.setLayoutData(fdAction);

    PropsUi.setLook(control);
    FormData fdControl = new FormData();
    fdControl.left = new FormAttachment(middlePct, 0);
    fdControl.right = new FormAttachment(actionButton, -margin);
    fdControl.top = new FormAttachment(0, margin);
    control.setLayoutData(fdControl);

    FormData fdLabel = new FormData();
    fdLabel.left = new FormAttachment(0, 0);
    fdLabel.right = new FormAttachment(middlePct, -margin);
    fdLabel.top = new FormAttachment(control, 0, SWT.TOP);
    label.setLayoutData(fdLabel);
  }

  private void buildRowControl(Composite row, String labelText, Control control) {
    Label label = createRowLabel(row, labelText);

    PropsUi.setLook(control);
    FormData fdControl = new FormData();
    fdControl.left = new FormAttachment(middlePct, 0);
    fdControl.right = new FormAttachment(100, 0);
    fdControl.top = new FormAttachment(0, 0);
    control.setLayoutData(fdControl);

    FormData fdLabel = new FormData();
    fdLabel.left = new FormAttachment(0, 0);
    fdLabel.right = new FormAttachment(middlePct, -margin);
    fdLabel.top = new FormAttachment(control, 0, SWT.TOP);
    label.setLayoutData(fdLabel);
  }

  private Label createRowLabel(Composite row, String labelText) {
    Label label = new Label(row, SWT.RIGHT);
    label.setText(labelText);
    PropsUi.setLook(label);
    return label;
  }

  TextVar getFileName() {
    return fileName;
  }

  Button getBrowseFileButton() {
    return browseFileButton;
  }

  ComboVar getFormat() {
    return format;
  }

  TextVar getLayerName() {
    return layerName;
  }

  ComboVar getWriteMode() {
    return writeMode;
  }

  ComboVar getGeometryField() {
    return geometryField;
  }

  TextVar getSelectedAttributes() {
    return selectedAttributes;
  }

  TextVar getDatasetCreationOptions() {
    return datasetCreationOptions;
  }

  TextVar getLayerCreationOptions() {
    return layerCreationOptions;
  }

  ComboVar getForceGeometryType() {
    return forceGeometryType;
  }
}
