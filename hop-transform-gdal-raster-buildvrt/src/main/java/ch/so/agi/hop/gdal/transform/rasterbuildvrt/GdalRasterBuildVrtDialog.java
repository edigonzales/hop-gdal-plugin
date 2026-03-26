package ch.so.agi.hop.gdal.transform.rasterbuildvrt;

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

public class GdalRasterBuildVrtDialog extends BaseTransformDialog {
  private final GdalRasterBuildVrtMeta input;
  private ComboVar wInputInterpretationMode;
  private ComboVar wInputListValueMode;
  private TextVar wInputListValue;
  private Button wbInputList;
  private ComboVar wInputListField;
  private ComboVar wOutputSourceMode;
  private ComboVar wOutputValueMode;
  private TextVar wOutputValue;
  private Button wbOutput;
  private ComboVar wOutputField;
  private ComboVar wAuthType;
  private TextVar wAuthUsername;
  private TextVar wAuthPassword;
  private TextVar wBearerToken;
  private TextVar wHeaderName;
  private TextVar wHeaderValue;
  private TextVar wGdalConfigOptions;
  private ComboVar wResolutionStrategy;
  private TextVar wBounds;
  private Button wSeparateBands;
  private TextVar wSrcNoData;
  private TextVar wVrtNoData;
  private Button wAllowProjectionDifference;
  private TextVar wAdditionalArgs;
  private Button wFailOnError;
  private Button wAddResultFields;
  private TabFolder wTabFolder;
  private List<RasterDialogUiSupport.TabSection> tabSections = List.of();

  public GdalRasterBuildVrtDialog(
      Shell parent, IVariables variables, GdalRasterBuildVrtMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parentShell = getParent();
    shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(980, 860);
    PropsUi.setLook(shell);
    setShellImage(shell, input);
    shell.setText("Raster Mosaic");

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
        RasterDialogUiSupport.createTabSection(wTabFolder, "Output & Mosaic options");
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

    wInputInterpretationMode.addModifyListener(e -> refreshEnabledStates());
    wInputListValueMode.addModifyListener(e -> refreshEnabledStates());
    wOutputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wOutputValueMode.addModifyListener(e -> refreshEnabledStates());
    wAuthType.addModifyListener(e -> refreshEnabledStates());
    wTabFolder.addListener(SWT.Selection, e -> refreshTabLayouts());
    shell.addListener(SWT.Resize, e -> refreshTabLayouts());
    refreshEnabledStates();

    wbInputList.addListener(SWT.Selection, e -> browseInputList());
    wbOutput.addListener(SWT.Selection, e -> browseOutput());
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());
    return transformName;
  }

  private void populateCombos() {
    String[] inputModes = {"LOCAL_FILE", "HTTP_URL", "GDAL_VSI"};
    String[] outputModes = {"LOCAL_FILE", "GDAL_VSI"};
    wInputInterpretationMode.setItems(inputModes);
    wOutputSourceMode.setItems(outputModes);
    wInputListValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wOutputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wAuthType.setItems(new String[] {"NONE", "BASIC_AUTH", "BEARER_TOKEN", "SIGNED_URL", "CUSTOM_HEADER"});
    wResolutionStrategy.setItems(new String[] {"AVERAGE", "HIGHEST", "LOWEST"});
  }

  private void loadFields() {
    BaseTransformDialog.getFieldsFromPrevious(variables, wInputListField, pipelineMeta, transformMeta);
    BaseTransformDialog.getFieldsFromPrevious(variables, wOutputField, pipelineMeta, transformMeta);
  }

  private void getData() {
    wInputInterpretationMode.setText(
        Utils.isEmpty(input.getInputInterpretationMode()) ? "LOCAL_FILE" : input.getInputInterpretationMode());
    wInputListValueMode.setText(
        Utils.isEmpty(input.getInputListValueMode()) ? "CONSTANT" : input.getInputListValueMode());
    wInputListValue.setText(Utils.isEmpty(input.getInputListValue()) ? "" : input.getInputListValue());
    wInputListField.setText(Utils.isEmpty(input.getInputListField()) ? "" : input.getInputListField());
    wOutputSourceMode.setText(Utils.isEmpty(input.getOutputSourceMode()) ? "LOCAL_FILE" : input.getOutputSourceMode());
    wOutputValueMode.setText(Utils.isEmpty(input.getOutputValueMode()) ? "CONSTANT" : input.getOutputValueMode());
    wOutputValue.setText(Utils.isEmpty(input.getOutputValue()) ? "" : input.getOutputValue());
    wOutputField.setText(Utils.isEmpty(input.getOutputField()) ? "" : input.getOutputField());
    wAuthType.setText(Utils.isEmpty(input.getAuthType()) ? "NONE" : input.getAuthType());
    wAuthUsername.setText(Utils.isEmpty(input.getAuthUsername()) ? "" : input.getAuthUsername());
    wAuthPassword.setText(Utils.isEmpty(input.getAuthPassword()) ? "" : input.getAuthPassword());
    wBearerToken.setText(Utils.isEmpty(input.getBearerToken()) ? "" : input.getBearerToken());
    wHeaderName.setText(Utils.isEmpty(input.getCustomHeaderName()) ? "" : input.getCustomHeaderName());
    wHeaderValue.setText(Utils.isEmpty(input.getCustomHeaderValue()) ? "" : input.getCustomHeaderValue());
    wGdalConfigOptions.setText(Utils.isEmpty(input.getGdalConfigOptions()) ? "" : input.getGdalConfigOptions());
    wResolutionStrategy.setText(
        Utils.isEmpty(input.getResolutionStrategy()) ? "AVERAGE" : input.getResolutionStrategy());
    wBounds.setText(Utils.isEmpty(input.getBounds()) ? "" : input.getBounds());
    wSrcNoData.setText(Utils.isEmpty(input.getSrcNoData()) ? "" : input.getSrcNoData());
    wVrtNoData.setText(Utils.isEmpty(input.getVrtNoData()) ? "" : input.getVrtNoData());
    wAdditionalArgs.setText(Utils.isEmpty(input.getAdditionalArgs()) ? "" : input.getAdditionalArgs());
    wSeparateBands.setSelection(input.isSeparateBands());
    wAllowProjectionDifference.setSelection(input.isAllowProjectionDifference());
    wFailOnError.setSelection(input.isFailOnError());
    wAddResultFields.setSelection(input.isAddResultFields());
  }

  private void buildInputTab(Composite content, int middle, int margin) {
    Composite last = null;
    last =
        row(
            content,
            last,
            "Input source interpretation",
            middle,
            margin,
            w -> wInputInterpretationMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Input list parameter source",
            middle,
            margin,
            w -> wInputListValueMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        rowWithButton(
            content,
            last,
            "Input raster list",
            middle,
            margin,
            w -> wInputListValue = (TextVar) w,
            b -> wbInputList = b,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    row(
        content,
        last,
        "Input list field",
        middle,
        margin,
        w -> wInputListField = (ComboVar) w,
        parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
  }

  private void buildOutputTab(Composite content, int middle, int margin) {
    Composite last = null;
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
            "Output mosaic path",
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
            "Resolution strategy",
            middle,
            margin,
            w -> wResolutionStrategy = (ComboVar) w,
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
            "Source nodata",
            middle,
            margin,
            w -> wSrcNoData = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "VRT nodata",
            middle,
            margin,
            w -> wVrtNoData = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Separate bands",
            middle,
            margin,
            w -> wSeparateBands = (Button) w,
            parent -> new Button(parent, SWT.CHECK));
    row(
        content,
        last,
        "Allow projection difference",
        middle,
        margin,
        w -> wAllowProjectionDifference = (Button) w,
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
            "Additional mosaic args",
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
    boolean constantInput = !"FIELD".equalsIgnoreCase(wInputListValueMode.getText());
    boolean constantOutput = !"FIELD".equalsIgnoreCase(wOutputValueMode.getText());
    boolean localInput = "LOCAL_FILE".equalsIgnoreCase(wInputInterpretationMode.getText());
    boolean localOutput = "LOCAL_FILE".equalsIgnoreCase(wOutputSourceMode.getText());
    RasterDialogUiSupport.setValueModeState(
        wInputListValue, wbInputList, wInputListField, constantInput, localInput);
    RasterDialogUiSupport.setValueModeState(
        wOutputValue, wbOutput, wOutputField, constantOutput, localOutput);
    RasterDialogUiSupport.setAuthState(
        wAuthType.getText(),
        wAuthUsername,
        wAuthPassword,
        wBearerToken,
        wHeaderName,
        wHeaderValue);
    refreshTabLayouts();
  }

  private void refreshTabLayouts() {
    RasterDialogUiSupport.refreshTabLayouts(wTabFolder, shell, tabSections);
  }

  private void ok() {
    transformName = wTransformName.getText();
    input.setInputInterpretationMode(wInputInterpretationMode.getText());
    input.setInputListValueMode(wInputListValueMode.getText());
    input.setInputListValue(wInputListValue.getText());
    input.setInputListField(wInputListField.getText());
    input.setOutputSourceMode(wOutputSourceMode.getText());
    input.setOutputValueMode(wOutputValueMode.getText());
    input.setOutputValue(wOutputValue.getText());
    input.setOutputField(wOutputField.getText());
    input.setAuthType(wAuthType.getText());
    input.setAuthUsername(wAuthUsername.getText());
    input.setAuthPassword(wAuthPassword.getText());
    input.setBearerToken(wBearerToken.getText());
    input.setCustomHeaderName(wHeaderName.getText());
    input.setCustomHeaderValue(wHeaderValue.getText());
    input.setGdalConfigOptions(wGdalConfigOptions.getText());
    input.setResolutionStrategy(wResolutionStrategy.getText());
    input.setBounds(wBounds.getText());
    input.setSeparateBands(wSeparateBands.getSelection());
    input.setSrcNoData(wSrcNoData.getText());
    input.setVrtNoData(wVrtNoData.getText());
    input.setAllowProjectionDifference(wAllowProjectionDifference.getSelection());
    input.setAdditionalArgs(wAdditionalArgs.getText());
    input.setFailOnError(wFailOnError.getSelection());
    input.setAddResultFields(wAddResultFields.getSelection());
    dispose();
  }

  private void cancel() {
    transformName = null;
    dispose();
  }

  private void browseInputList() {
    if (!"LOCAL_FILE".equalsIgnoreCase(wInputInterpretationMode.getText())
        || !"CONSTANT".equalsIgnoreCase(wInputListValueMode.getText())) {
      return;
    }
    FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
    String selected = dialog.open();
    if (selected == null) {
      return;
    }
    StringBuilder buffer = new StringBuilder(selected);
    String basePath = dialog.getFilterPath();
    String[] files = dialog.getFileNames();
    for (int i = 1; i < files.length; i++) {
      buffer.append(';').append(basePath).append('/').append(files[i]);
    }
    wInputListValue.setText(buffer.toString());
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
