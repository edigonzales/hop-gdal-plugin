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
  private Text availableFieldsPreview;
  private TextVar selectedAttributes;
  private TextVar attributeFilter;
  private TextVar bbox;
  private TextVar polygonWkt;
  private TextVar featureLimit;
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

    Composite lastRow = null;

    Composite fileRow = createRow(lastRow);
    fileName = new TextVar(variables, fileRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    browseFileButton = new Button(fileRow, SWT.PUSH | SWT.CENTER);
    buildRowControlWithButton(
        fileRow,
        BaseMessages.getString(PKG, "OgrInputDialog.FileName.Label"),
        fileName,
        browseFileButton,
        BaseMessages.getString(PKG, "System.Button.Browse"));
    lastRow = fileRow;

    Composite layerRow = createRow(lastRow);
    layerName = new ComboVar(variables, layerRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(layerRow, BaseMessages.getString(PKG, "OgrInputDialog.LayerName.Label"), layerName);
    lastRow = layerRow;

    Composite fieldsRow = createRow(lastRow);
    availableFieldsPreview =
        new Text(fieldsRow, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    availableFieldsPreview.setEditable(false);
    buildRowMultiline(
        fieldsRow,
        BaseMessages.getString(PKG, "OgrInputDialog.AvailableFields.Label"),
        availableFieldsPreview,
        140);
    lastRow = fieldsRow;

    Composite selectedAttributesRow = createRow(lastRow);
    selectedAttributes = new TextVar(variables, selectedAttributesRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        selectedAttributesRow,
        BaseMessages.getString(PKG, "OgrInputDialog.SelectedAttributes.Label"),
        selectedAttributes);
    lastRow = selectedAttributesRow;

    Composite attributeFilterRow = createRow(lastRow);
    attributeFilter = new TextVar(variables, attributeFilterRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        attributeFilterRow,
        BaseMessages.getString(PKG, "OgrInputDialog.AttributeFilter.Label"),
        attributeFilter);
    lastRow = attributeFilterRow;

    Composite bboxRow = createRow(lastRow);
    bbox = new TextVar(variables, bboxRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(bboxRow, BaseMessages.getString(PKG, "OgrInputDialog.Bbox.Label"), bbox);
    lastRow = bboxRow;

    Composite polygonWktRow = createRow(lastRow);
    polygonWkt = new TextVar(variables, polygonWktRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        polygonWktRow,
        BaseMessages.getString(PKG, "OgrInputDialog.PolygonWkt.Label"),
        polygonWkt);
    lastRow = polygonWktRow;

    Composite featureLimitRow = createRow(lastRow);
    featureLimit = new TextVar(variables, featureLimitRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        featureLimitRow,
        BaseMessages.getString(PKG, "OgrInputDialog.FeatureLimit.Label"),
        featureLimit);
    lastRow = featureLimitRow;

    Composite openOptionsRow = createRow(lastRow);
    openOptions = new TextVar(variables, openOptionsRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        openOptionsRow,
        BaseMessages.getString(PKG, "OgrInputDialog.OpenOptions.Label"),
        openOptions);
    lastRow = openOptionsRow;

    Composite includeFidRow = createRow(lastRow);
    includeFid = new Button(includeFidRow, SWT.CHECK);
    buildRowControl(
        includeFidRow, BaseMessages.getString(PKG, "OgrInputDialog.IncludeFid.Label"), includeFid);
    lastRow = includeFidRow;

    Composite fidFieldNameRow = createRow(lastRow);
    fidFieldName = new Text(fidFieldNameRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        fidFieldNameRow,
        BaseMessages.getString(PKG, "OgrInputDialog.FidFieldName.Label"),
        fidFieldName);
    lastRow = fidFieldNameRow;

    Composite geometryFieldNameRow = createRow(lastRow);
    geometryFieldName = new Text(geometryFieldNameRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    buildRowControl(
        geometryFieldNameRow,
        BaseMessages.getString(PKG, "OgrInputDialog.GeometryFieldName.Label"),
        geometryFieldName);
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
//    String bar
    //var foo = b;
  }

  private void buildRowMultiline(
      Composite row, String labelText, Control control, int height) {
    Label label = createRowLabel(row, labelText);

    PropsUi.setLook(control);
    FormData fdControl = new FormData();
    fdControl.left = new FormAttachment(middlePct, 0);
    fdControl.right = new FormAttachment(100, 0);
    fdControl.top = new FormAttachment(0, 0);
    fdControl.height = height;
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

  ComboVar getLayerName() {
    return layerName;
  }

  Text getAvailableFieldsPreview() {
    return availableFieldsPreview;
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
