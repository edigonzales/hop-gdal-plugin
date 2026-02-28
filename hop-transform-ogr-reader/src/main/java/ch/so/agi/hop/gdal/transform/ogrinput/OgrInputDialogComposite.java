package ch.so.agi.hop.gdal.transform.ogrinput;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class OgrInputDialogComposite extends Composite {

  private static final Class<?> PKG = OgrInputMeta.class;

  private final IVariables variables;
  private final int middlePct;
  private final int margin;

  private TextVar fileName;
  private Button browseFileButton;
  private ComboVar layerName;
  private Button loadLayersButton;
  private Text availableFieldsPreview;
  private Button refreshFieldsButton;
  private TextVar selectedAttributes;
  private TextVar attributeFilter;
  private TextVar bbox;
  private TextVar polygonWkt;
  private TextVar featureLimit;
  private TextVar allowedDrivers;
  private TextVar openOptions;
  private Button includeFid;
  private Text fidFieldName;
  private Text geometryFieldName;

  public OgrInputDialogComposite(Composite parent, int style) {
    this(parent, style, Variables.getADefaultVariableSpace(), 35);
  }

  OgrInputDialogComposite(Composite parent, int style, IVariables variables, int middlePct) {
    super(parent, style);
    this.variables =
        variables == null ? Variables.getADefaultVariableSpace() : variables;
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

    Control lastControl = null;

    Composite fileRow =
        createLabeledRow(
            BaseMessages.getString(PKG, "OgrInputDialog.FileName.Label"), lastControl);
    fileName = new TextVar(variables, fileRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    browseFileButton = new Button(fileRow, SWT.PUSH | SWT.CENTER);
    configureSingleLineRow(
        fileRow,
        fileName,
        browseFileButton,
        BaseMessages.getString(PKG, "System.Button.Browse"));
    lastControl = fileRow;

    Composite layerRow =
        createLabeledRow(
            BaseMessages.getString(PKG, "OgrInputDialog.LayerName.Label"), lastControl);
    layerName = new ComboVar(variables, layerRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    loadLayersButton = new Button(layerRow, SWT.PUSH | SWT.CENTER);
    configureSingleLineRow(
        layerRow,
        layerName,
        loadLayersButton,
        BaseMessages.getString(PKG, "OgrInputDialog.LoadLayers.Button"));
    lastControl = layerRow;

    Composite fieldsRow =
        createLabeledRow(
            BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.Label"), lastControl);
    availableFieldsPreview =
        new Text(fieldsRow, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    availableFieldsPreview.setEditable(false);
    refreshFieldsButton = new Button(fieldsRow, SWT.PUSH | SWT.CENTER);
    configureMultiLineRow(
        fieldsRow,
        availableFieldsPreview,
        140,
        refreshFieldsButton,
        BaseMessages.getString(PKG, "OgrInputDialog.RefreshFields.Button"));
    lastControl = fieldsRow;

    selectedAttributes = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.SelectedAttributes.Label"),
        selectedAttributes,
        lastControl);
    lastControl = selectedAttributes;

    attributeFilter = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.AttributeFilter.Label"),
        attributeFilter,
        lastControl);
    lastControl = attributeFilter;

    bbox = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(BaseMessages.getString(PKG, "OgrInputDialog.Bbox.Label"), bbox, lastControl);
    lastControl = bbox;

    polygonWkt = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.PolygonWkt.Label"), polygonWkt, lastControl);
    lastControl = polygonWkt;

    featureLimit = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.FeatureLimit.Label"), featureLimit, lastControl);
    lastControl = featureLimit;

    allowedDrivers = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.AllowedDrivers.Label"),
        allowedDrivers,
        lastControl);
    lastControl = allowedDrivers;

    openOptions = new TextVar(variables, this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.OpenOptions.Label"),
        openOptions,
        lastControl);
    lastControl = openOptions;

    includeFid = new Button(this, SWT.CHECK);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.IncludeFid.Label"), includeFid, lastControl);
    lastControl = includeFid;

    fidFieldName = new Text(this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.FidFieldName.Label"), fidFieldName, lastControl);
    lastControl = fidFieldName;

    geometryFieldName = new Text(this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    placeControl(
        BaseMessages.getString(PKG, "OgrInputDialog.GeometryFieldName.Label"),
        geometryFieldName,
        lastControl);
  }

  private Composite createLabeledRow(String labelText, Control under) {
    Label label = new Label(this, SWT.RIGHT);
    label.setText(labelText);
    PropsUi.setLook(label);

    FormData fdLabel = new FormData();
    fdLabel.left = new FormAttachment(0, 0);
    fdLabel.right = new FormAttachment(middlePct, -margin);
    fdLabel.top = under == null ? new FormAttachment(0, 0) : new FormAttachment(under, margin);
    label.setLayoutData(fdLabel);

    Composite row = new Composite(this, SWT.NONE);
    PropsUi.setLook(row);

    FormData fdRow = new FormData();
    fdRow.left = new FormAttachment(middlePct, 0);
    fdRow.right = new FormAttachment(100, 0);
    fdRow.top = under == null ? new FormAttachment(0, 0) : new FormAttachment(under, margin);
    row.setLayoutData(fdRow);
    return row;
  }

  private void configureSingleLineRow(
      Composite row, Control control, Button actionButton, String buttonLabel) {
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    rowLayout.horizontalSpacing = margin;
    row.setLayout(rowLayout);

    PropsUi.setLook(control);
    GridData gdControl = new GridData(SWT.FILL, SWT.CENTER, true, false);
    control.setLayoutData(gdControl);

    PropsUi.setLook(actionButton);
    actionButton.setText(buttonLabel);
    GridData gdButton = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    actionButton.setLayoutData(gdButton);
  }

  private void configureMultiLineRow(
      Composite row, Control control, int height, Button actionButton, String buttonLabel) {
    GridLayout rowLayout = new GridLayout(2, false);
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    rowLayout.horizontalSpacing = margin;
    row.setLayout(rowLayout);

    PropsUi.setLook(control);
    GridData gdControl = new GridData(SWT.FILL, SWT.FILL, true, true);
    gdControl.heightHint = height;
    control.setLayoutData(gdControl);

    PropsUi.setLook(actionButton);
    actionButton.setText(buttonLabel);
    GridData gdButton = new GridData(SWT.RIGHT, SWT.BEGINNING, false, false);
    actionButton.setLayoutData(gdButton);
  }

  private void placeControl(String labelText, Control control, Control under) {
    Label label = new Label(this, SWT.RIGHT);
    label.setText(labelText);
    PropsUi.setLook(label);

    FormData fdLabel = new FormData();
    fdLabel.left = new FormAttachment(0, 0);
    fdLabel.right = new FormAttachment(middlePct, -margin);
    fdLabel.top = under == null ? new FormAttachment(0, 0) : new FormAttachment(under, margin);
    label.setLayoutData(fdLabel);

    PropsUi.setLook(control);
    FormData fdControl = new FormData();
    fdControl.left = new FormAttachment(middlePct, 0);
    fdControl.right = new FormAttachment(100, 0);
    fdControl.top = under == null ? new FormAttachment(0, 0) : new FormAttachment(under, margin);
    control.setLayoutData(fdControl);
  }

  TextVar getFileName() {
    return fileName;
  }

  Button getBrowseFileButton() {
    return browseFileButton;
  }

  ComboVar getLayerName() {
    return layerName;
  }

  Button getLoadLayersButton() {
    return loadLayersButton;
  }

  Text getAvailableFieldsPreview() {
    return availableFieldsPreview;
  }

  Button getRefreshFieldsButton() {
    return refreshFieldsButton;
  }

  TextVar getSelectedAttributes() {
    return selectedAttributes;
  }

  TextVar getAttributeFilter() {
    return attributeFilter;
  }

  TextVar getBbox() {
    return bbox;
  }

  TextVar getPolygonWkt() {
    return polygonWkt;
  }

  TextVar getFeatureLimit() {
    return featureLimit;
  }

  TextVar getAllowedDrivers() {
    return allowedDrivers;
  }

  TextVar getOpenOptions() {
    return openOptions;
  }

  Button getIncludeFid() {
    return includeFid;
  }

  Text getFidFieldName() {
    return fidFieldName;
  }

  Text getGeometryFieldName() {
    return geometryFieldName;
  }
}
