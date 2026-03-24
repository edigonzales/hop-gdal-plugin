package ch.so.agi.hop.gdal.transform.rasterizevector;

import ch.so.agi.hop.gdal.raster.core.RasterDialogUiSupport;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

import java.util.List;

public class GdalRasterizeVectorDialog extends BaseTransformDialog {
  private final GdalRasterizeVectorMeta input;
  private ComboVar wVectorInputMode;
  private ComboVar wInputSourceMode;
  private ComboVar wInputValueMode;
  private TextVar wInputValue;
  private Button wbInput;
  private ComboVar wInputField;
  private TextVar wLayerName;
  private ComboVar wGeometryField;
  private ComboVar wBurnStrategy;
  private TextVar wBurnValue;
  private ComboVar wBurnField;
  private ComboVar wOutputSourceMode;
  private ComboVar wOutputValueMode;
  private TextVar wOutputValue;
  private Button wbOutput;
  private ComboVar wOutputField;
  private TextVar wOutputFormat;
  private ComboVar wGridMode;
  private TextVar wBounds;
  private TextVar wCrs;
  private TextVar wResolutionX;
  private TextVar wResolutionY;
  private TextVar wWidth;
  private TextVar wHeight;
  private TextVar wOutputDataType;
  private TextVar wInitValue;
  private TextVar wNoDataValue;
  private Button wAllTouched;
  private TextVar wCreationOptions;
  private TextVar wAdditionalArgs;
  private ComboVar wAuthType;
  private TextVar wAuthUsername;
  private TextVar wAuthPassword;
  private TextVar wBearerToken;
  private TextVar wHeaderName;
  private TextVar wHeaderValue;
  private TextVar wGdalConfigOptions;
  private Button wFailOnError;
  private Button wAddResultFields;
  private TabFolder wTabFolder;
  private List<RasterDialogUiSupport.TabSection> tabSections = List.of();

  public GdalRasterizeVectorDialog(
      Shell parent, IVariables variables, GdalRasterizeVectorMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parentShell = getParent();
    shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(960, 780);
    PropsUi.setLook(shell);
    setShellImage(shell, input);
    shell.setText("Rasterize Vector");

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();
    shell.setLayout(formLayout);

    int margin = PropsUi.getMargin();

    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText("Transform name");
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(props.getMiddlePct(), -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);

    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(props.getMiddlePct(), 0);
    fdTransformName.right = new FormAttachment(100, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    wTransformName.setLayoutData(fdTransformName);

    Button wOk = new Button(shell, SWT.PUSH);
    wOk.setText("OK");
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText("Cancel");
    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    wTabFolder = new TabFolder(shell, SWT.NONE);
    PropsUi.setLook(wTabFolder);
    FormData fdMain = new FormData();
    fdMain.left = new FormAttachment(0, 0);
    fdMain.top = new FormAttachment(wTransformName, margin * 2);
    fdMain.right = new FormAttachment(100, 0);
    fdMain.bottom = new FormAttachment(wOk, -margin * 2);
    wTabFolder.setLayoutData(fdMain);

    int middle = props.getMiddlePct();
    RasterDialogUiSupport.TabSection inputTab = RasterDialogUiSupport.createTabSection(wTabFolder, "Input");
    RasterDialogUiSupport.TabSection outputTab =
        RasterDialogUiSupport.createTabSection(wTabFolder, "Output & Rasterization");
    RasterDialogUiSupport.TabSection remoteTab =
        RasterDialogUiSupport.createTabSection(wTabFolder, "Remote access");
    RasterDialogUiSupport.TabSection advancedTab =
        RasterDialogUiSupport.createTabSection(wTabFolder, "Advanced");
    tabSections = List.of(inputTab, outputTab, remoteTab, advancedTab);

    buildInputTab(inputTab.content(), middle, margin);
    buildOutputTab(outputTab.content(), middle, margin);
    buildRemoteTab(remoteTab.content(), middle, margin);
    buildAdvancedTab(advancedTab.content(), middle, margin);

    populateCombos();
    loadFields();
    getData();

    wVectorInputMode.addModifyListener(e -> refreshEnabledStates());
    wInputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wInputValueMode.addModifyListener(e -> refreshEnabledStates());
    wBurnStrategy.addModifyListener(e -> refreshEnabledStates());
    wOutputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wOutputValueMode.addModifyListener(e -> refreshEnabledStates());
    wGridMode.addModifyListener(e -> refreshEnabledStates());
    wAuthType.addModifyListener(e -> refreshEnabledStates());
    wTabFolder.addListener(SWT.Selection, e -> refreshTabLayouts());
    shell.addListener(SWT.Resize, e -> refreshTabLayouts());
    refreshEnabledStates();

    wbInput.addListener(SWT.Selection, e -> browseInput());
    wbOutput.addListener(SWT.Selection, e -> browseOutput());
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());
    return transformName;
  }

  private void populateCombos() {
    String[] datasetModes = {"LOCAL_FILE", "HTTP_URL", "GDAL_VSI"};
    wVectorInputMode.setItems(new String[] {"DATASET_LAYER", "HOP_GEOMETRY_FIELD"});
    wInputSourceMode.setItems(datasetModes);
    wInputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wBurnStrategy.setItems(new String[] {"CONSTANT_VALUE", "ATTRIBUTE_FIELD"});
    wOutputSourceMode.setItems(datasetModes);
    wOutputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wGridMode.setItems(new String[] {"BOUNDS_RESOLUTION", "BOUNDS_SIZE"});
    wAuthType.setItems(new String[] {"NONE", "BASIC_AUTH", "BEARER_TOKEN", "SIGNED_URL", "CUSTOM_HEADER"});
  }

  private void loadFields() {
    BaseTransformDialog.getFieldsFromPrevious(variables, wInputField, pipelineMeta, transformMeta);
    BaseTransformDialog.getFieldsFromPrevious(variables, wGeometryField, pipelineMeta, transformMeta);
    BaseTransformDialog.getFieldsFromPrevious(variables, wBurnField, pipelineMeta, transformMeta);
    BaseTransformDialog.getFieldsFromPrevious(variables, wOutputField, pipelineMeta, transformMeta);
  }

  private void getData() {
    wVectorInputMode.setText(Utils.isEmpty(input.getVectorInputMode()) ? "DATASET_LAYER" : input.getVectorInputMode());
    wInputSourceMode.setText(Utils.isEmpty(input.getInputSourceMode()) ? "LOCAL_FILE" : input.getInputSourceMode());
    wInputValueMode.setText(Utils.isEmpty(input.getInputValueMode()) ? "CONSTANT" : input.getInputValueMode());
    wInputValue.setText(Utils.isEmpty(input.getInputValue()) ? "" : input.getInputValue());
    wInputField.setText(Utils.isEmpty(input.getInputField()) ? "" : input.getInputField());
    wLayerName.setText(Utils.isEmpty(input.getLayerName()) ? "" : input.getLayerName());
    wGeometryField.setText(Utils.isEmpty(input.getGeometryField()) ? "" : input.getGeometryField());
    wBurnStrategy.setText(Utils.isEmpty(input.getBurnStrategy()) ? "CONSTANT_VALUE" : input.getBurnStrategy());
    wBurnValue.setText(Utils.isEmpty(input.getBurnValue()) ? "1" : input.getBurnValue());
    wBurnField.setText(Utils.isEmpty(input.getBurnField()) ? "" : input.getBurnField());
    wOutputSourceMode.setText(Utils.isEmpty(input.getOutputSourceMode()) ? "LOCAL_FILE" : input.getOutputSourceMode());
    wOutputValueMode.setText(Utils.isEmpty(input.getOutputValueMode()) ? "CONSTANT" : input.getOutputValueMode());
    wOutputValue.setText(Utils.isEmpty(input.getOutputValue()) ? "" : input.getOutputValue());
    wOutputField.setText(Utils.isEmpty(input.getOutputField()) ? "" : input.getOutputField());
    wOutputFormat.setText(Utils.isEmpty(input.getOutputFormat()) ? "GTiff" : input.getOutputFormat());
    wGridMode.setText(Utils.isEmpty(input.getGridMode()) ? "BOUNDS_RESOLUTION" : input.getGridMode());
    wBounds.setText(Utils.isEmpty(input.getBounds()) ? "" : input.getBounds());
    wCrs.setText(Utils.isEmpty(input.getCrs()) ? "" : input.getCrs());
    wResolutionX.setText(Utils.isEmpty(input.getResolutionX()) ? "" : input.getResolutionX());
    wResolutionY.setText(Utils.isEmpty(input.getResolutionY()) ? "" : input.getResolutionY());
    wWidth.setText(Utils.isEmpty(input.getWidth()) ? "" : input.getWidth());
    wHeight.setText(Utils.isEmpty(input.getHeight()) ? "" : input.getHeight());
    wOutputDataType.setText(Utils.isEmpty(input.getOutputDataType()) ? "" : input.getOutputDataType());
    wInitValue.setText(Utils.isEmpty(input.getInitValue()) ? "" : input.getInitValue());
    wNoDataValue.setText(Utils.isEmpty(input.getNoDataValue()) ? "" : input.getNoDataValue());
    wCreationOptions.setText(Utils.isEmpty(input.getCreationOptions()) ? "" : input.getCreationOptions());
    wAdditionalArgs.setText(Utils.isEmpty(input.getAdditionalArgs()) ? "" : input.getAdditionalArgs());
    wAuthType.setText(Utils.isEmpty(input.getAuthType()) ? "NONE" : input.getAuthType());
    wAuthUsername.setText(Utils.isEmpty(input.getAuthUsername()) ? "" : input.getAuthUsername());
    wAuthPassword.setText(Utils.isEmpty(input.getAuthPassword()) ? "" : input.getAuthPassword());
    wBearerToken.setText(Utils.isEmpty(input.getBearerToken()) ? "" : input.getBearerToken());
    wHeaderName.setText(Utils.isEmpty(input.getCustomHeaderName()) ? "" : input.getCustomHeaderName());
    wHeaderValue.setText(Utils.isEmpty(input.getCustomHeaderValue()) ? "" : input.getCustomHeaderValue());
    wGdalConfigOptions.setText(Utils.isEmpty(input.getGdalConfigOptions()) ? "" : input.getGdalConfigOptions());
    wAllTouched.setSelection(input.isAllTouched());
    wFailOnError.setSelection(input.isFailOnError());
    wAddResultFields.setSelection(input.isAddResultFields());
  }

  private void buildInputTab(Composite content, int middle, int margin) {
    Composite last = null;
    last =
        row(
            content,
            last,
            "Input source mode",
            middle,
            margin,
            w -> wVectorInputMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Dataset reference mode",
            middle,
            margin,
            w -> wInputSourceMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Dataset parameter source",
            middle,
            margin,
            w -> wInputValueMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        rowWithButton(
            content,
            last,
            "Input dataset / URL / VSI path",
            middle,
            margin,
            w -> wInputValue = (TextVar) w,
            b -> wbInput = b,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Input dataset field",
            middle,
            margin,
            w -> wInputField = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Layer name",
            middle,
            margin,
            w -> wLayerName = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    row(
        content,
        last,
        "Geometry field",
        middle,
        margin,
        w -> wGeometryField = (ComboVar) w,
        parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
  }

  private void buildOutputTab(Composite content, int middle, int margin) {
    Composite last = null;
    last =
        row(
            content,
            last,
            "Burn strategy",
            middle,
            margin,
            w -> wBurnStrategy = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Burn value",
            middle,
            margin,
            w -> wBurnValue = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Burn attribute field",
            middle,
            margin,
            w -> wBurnField = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output source mode",
            middle,
            margin,
            w -> wOutputSourceMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output parameter source",
            middle,
            margin,
            w -> wOutputValueMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        rowWithButton(
            content,
            last,
            "Output raster",
            middle,
            margin,
            w -> wOutputValue = (TextVar) w,
            b -> wbOutput = b,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output field",
            middle,
            margin,
            w -> wOutputField = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output format",
            middle,
            margin,
            w -> wOutputFormat = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Grid mode",
            middle,
            margin,
            w -> wGridMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Bounds",
            middle,
            margin,
            w -> wBounds = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "CRS",
            middle,
            margin,
            w -> wCrs = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Resolution X",
            middle,
            margin,
            w -> wResolutionX = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Resolution Y",
            middle,
            margin,
            w -> wResolutionY = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Width",
            middle,
            margin,
            w -> wWidth = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Height",
            middle,
            margin,
            w -> wHeight = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output data type",
            middle,
            margin,
            w -> wOutputDataType = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Init value",
            middle,
            margin,
            w -> wInitValue = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "NoData value",
            middle,
            margin,
            w -> wNoDataValue = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    row(
        content,
        last,
        "All touched",
        middle,
        margin,
        w -> wAllTouched = (Button) w,
        parent -> new Button(parent, SWT.CHECK));
  }

  private void buildRemoteTab(Composite content, int middle, int margin) {
    Composite last = null;
    last =
        row(
            content,
            last,
            "Authentication type",
            middle,
            margin,
            w -> wAuthType = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Basic auth user",
            middle,
            margin,
            w -> wAuthUsername = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Basic auth password",
            middle,
            margin,
            w -> wAuthPassword = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.PASSWORD));
    last =
        row(
            content,
            last,
            "Bearer token",
            middle,
            margin,
            w -> wBearerToken = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Custom header name",
            middle,
            margin,
            w -> wHeaderName = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Custom header value",
            middle,
            margin,
            w -> wHeaderValue = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    row(
        content,
        last,
        "GDAL/VSI config options",
        middle,
        margin,
        w -> wGdalConfigOptions = (TextVar) w,
        parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
  }

  private void buildAdvancedTab(Composite content, int middle, int margin) {
    Composite last = null;
    last =
        row(
            content,
            last,
            "Creation options",
            middle,
            margin,
            w -> wCreationOptions = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Additional rasterize args",
            middle,
            margin,
            w -> wAdditionalArgs = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Fail pipeline on first error",
            middle,
            margin,
            w -> wFailOnError = (Button) w,
            parent -> new Button(parent, SWT.CHECK));
    row(
        content,
        last,
        "Add result fields",
        middle,
        margin,
        w -> wAddResultFields = (Button) w,
        parent -> new Button(parent, SWT.CHECK));
  }

  private void refreshEnabledStates() {
    boolean datasetLayerMode = !"HOP_GEOMETRY_FIELD".equalsIgnoreCase(wVectorInputMode.getText());
    boolean constantInput = !"FIELD".equalsIgnoreCase(wInputValueMode.getText());
    boolean constantOutput = !"FIELD".equalsIgnoreCase(wOutputValueMode.getText());
    boolean localInput = "LOCAL_FILE".equalsIgnoreCase(wInputSourceMode.getText());
    boolean localOutput = "LOCAL_FILE".equalsIgnoreCase(wOutputSourceMode.getText());
    boolean constantBurn = !"ATTRIBUTE_FIELD".equalsIgnoreCase(wBurnStrategy.getText());
    boolean resolutionGrid = !"BOUNDS_SIZE".equalsIgnoreCase(wGridMode.getText());

    RasterDialogUiSupport.setControlEnabled(wInputSourceMode, datasetLayerMode);
    RasterDialogUiSupport.setControlEnabled(wInputValueMode, datasetLayerMode);
    RasterDialogUiSupport.setValueModeState(
        wInputValue, wbInput, wInputField, datasetLayerMode && constantInput, datasetLayerMode && localInput);
    if (!datasetLayerMode) {
      RasterDialogUiSupport.setControlEnabled(wInputField, false);
      RasterDialogUiSupport.setTextEditable(wInputValue, false);
      RasterDialogUiSupport.setControlEnabled(wbInput, false);
    }
    RasterDialogUiSupport.setTextEditable(wLayerName, datasetLayerMode);
    RasterDialogUiSupport.setControlEnabled(wGeometryField, !datasetLayerMode);

    RasterDialogUiSupport.setTextEditable(wBurnValue, constantBurn);
    RasterDialogUiSupport.setControlEnabled(wBurnField, !constantBurn);

    RasterDialogUiSupport.setValueModeState(
        wOutputValue, wbOutput, wOutputField, constantOutput, localOutput);

    RasterDialogUiSupport.setTextEditable(wResolutionX, resolutionGrid);
    RasterDialogUiSupport.setTextEditable(wResolutionY, resolutionGrid);
    RasterDialogUiSupport.setTextEditable(wWidth, !resolutionGrid);
    RasterDialogUiSupport.setTextEditable(wHeight, !resolutionGrid);

    RasterDialogUiSupport.setAuthState(
        wAuthType.getText(),
        wAuthUsername,
        wAuthPassword,
        wBearerToken,
        wHeaderName,
        wHeaderValue);
    RasterDialogUiSupport.refreshTabLayouts(wTabFolder, shell, tabSections);
  }

  private void ok() {
    transformName = wTransformName.getText();
    input.setVectorInputMode(wVectorInputMode.getText());
    input.setInputSourceMode(wInputSourceMode.getText());
    input.setInputValueMode(wInputValueMode.getText());
    input.setInputValue(wInputValue.getText());
    input.setInputField(wInputField.getText());
    input.setLayerName(wLayerName.getText());
    input.setGeometryField(wGeometryField.getText());
    input.setBurnStrategy(wBurnStrategy.getText());
    input.setBurnValue(wBurnValue.getText());
    input.setBurnField(wBurnField.getText());
    input.setOutputSourceMode(wOutputSourceMode.getText());
    input.setOutputValueMode(wOutputValueMode.getText());
    input.setOutputValue(wOutputValue.getText());
    input.setOutputField(wOutputField.getText());
    input.setOutputFormat(wOutputFormat.getText());
    input.setGridMode(wGridMode.getText());
    input.setBounds(wBounds.getText());
    input.setCrs(wCrs.getText());
    input.setResolutionX(wResolutionX.getText());
    input.setResolutionY(wResolutionY.getText());
    input.setWidth(wWidth.getText());
    input.setHeight(wHeight.getText());
    input.setOutputDataType(wOutputDataType.getText());
    input.setInitValue(wInitValue.getText());
    input.setNoDataValue(wNoDataValue.getText());
    input.setAllTouched(wAllTouched.getSelection());
    input.setCreationOptions(wCreationOptions.getText());
    input.setAdditionalArgs(wAdditionalArgs.getText());
    input.setAuthType(wAuthType.getText());
    input.setAuthUsername(wAuthUsername.getText());
    input.setAuthPassword(wAuthPassword.getText());
    input.setBearerToken(wBearerToken.getText());
    input.setCustomHeaderName(wHeaderName.getText());
    input.setCustomHeaderValue(wHeaderValue.getText());
    input.setGdalConfigOptions(wGdalConfigOptions.getText());
    input.setFailOnError(wFailOnError.getSelection());
    input.setAddResultFields(wAddResultFields.getSelection());
    dispose();
  }

  private void cancel() {
    transformName = null;
    dispose();
  }

  private void browseInput() {
    if (!"LOCAL_FILE".equalsIgnoreCase(wInputSourceMode.getText())
        || !"CONSTANT".equalsIgnoreCase(wInputValueMode.getText())) {
      return;
    }
    FileDialog dialog = new FileDialog(shell, SWT.OPEN);
    String selected = dialog.open();
    if (selected != null) {
      wInputValue.setText(selected);
    }
  }

  private void browseOutput() {
    if (!"LOCAL_FILE".equalsIgnoreCase(wOutputSourceMode.getText())
        || !"CONSTANT".equalsIgnoreCase(wOutputValueMode.getText())) {
      return;
    }
    FileDialog dialog = new FileDialog(shell, SWT.SAVE);
    String selected = dialog.open();
    if (selected != null) {
      wOutputValue.setText(selected);
    }
  }

  private void refreshTabLayouts() {
    RasterDialogUiSupport.refreshTabLayouts(wTabFolder, shell, tabSections);
  }

  private interface ControlConsumer<T extends org.eclipse.swt.widgets.Control> {
    void accept(T control);
  }

  private interface ControlFactory<T extends org.eclipse.swt.widgets.Control> {
    T create(Composite parent);
  }

  private Composite row(
      Composite parent,
      Composite under,
      String label,
      int middle,
      int margin,
      ControlConsumer<org.eclipse.swt.widgets.Control> setter,
      ControlFactory<? extends org.eclipse.swt.widgets.Control> controlFactory) {
    Composite row = RasterDialogUiSupport.createRow(parent, under, margin);
    org.eclipse.swt.widgets.Control control = controlFactory.create(row);
    setter.accept(control);
    RasterDialogUiSupport.buildRowControl(row, label, control, middle, margin);
    return row;
  }

  private Composite rowWithButton(
      Composite parent,
      Composite under,
      String label,
      int middle,
      int margin,
      ControlConsumer<org.eclipse.swt.widgets.Control> setter,
      java.util.function.Consumer<Button> buttonSetter,
      ControlFactory<TextVar> controlFactory) {
    Composite row = RasterDialogUiSupport.createRow(parent, under, margin);
    Button button = new Button(row, SWT.PUSH | SWT.CENTER);
    TextVar control = controlFactory.create(row);
    setter.accept(control);
    buttonSetter.accept(button);
    RasterDialogUiSupport.buildRowControlWithButton(row, label, control, button, "Browse", middle, margin);
    return row;
  }
}
