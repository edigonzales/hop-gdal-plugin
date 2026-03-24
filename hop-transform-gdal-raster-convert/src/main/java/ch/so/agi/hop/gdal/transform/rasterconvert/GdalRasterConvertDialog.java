package ch.so.agi.hop.gdal.transform.rasterconvert;

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

public class GdalRasterConvertDialog extends BaseTransformDialog {
  private final GdalRasterConvertMeta input;
  private ComboVar wInputSourceMode;
  private ComboVar wInputValueMode;
  private TextVar wInputValue;
  private Button wbInput;
  private ComboVar wInputField;
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
  private TextVar wOutputFormat;
  private Button wOverwrite;
  private TextVar wBandSelection;
  private TextVar wOutputDataType;
  private Button wScale;
  private Button wUnscale;
  private TextVar wPixelWindow;
  private TextVar wCoordinateWindow;
  private TextVar wOutputNoData;
  private TextVar wCreationOptions;
  private TextVar wAdditionalArgs;
  private TabFolder wTabFolder;
  private List<RasterDialogUiSupport.TabSection> tabSections = List.of();

  public GdalRasterConvertDialog(
      Shell parent, IVariables variables, GdalRasterConvertMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parentShell = getParent();
    shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(980, 840);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

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
        RasterDialogUiSupport.createTabSection(wTabFolder, "Output & Translate");
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

    wInputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wInputValueMode.addModifyListener(e -> refreshEnabledStates());
    wOutputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wOutputValueMode.addModifyListener(e -> refreshEnabledStates());
    wAuthType.addModifyListener(e -> refreshEnabledStates());
    wTabFolder.addListener(SWT.Selection, e -> refreshTabLayouts());
    shell.addListener(SWT.Resize, e -> refreshTabLayouts());
    refreshEnabledStates();

    wbInput.addListener(SWT.Selection, e -> browse(wInputValue, SWT.OPEN));
    wbOutput.addListener(SWT.Selection, e -> browse(wOutputValue, SWT.SAVE));
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());
    return transformName;
  }

  private void populateCombos() {
    String[] modes = {"LOCAL_FILE", "HTTP_URL", "GDAL_VSI"};
    wInputSourceMode.setItems(modes);
    wOutputSourceMode.setItems(modes);
    wInputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wOutputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wAuthType.setItems(new String[] {"NONE", "BASIC_AUTH", "BEARER_TOKEN", "SIGNED_URL", "CUSTOM_HEADER"});
  }

  private void loadFields() {
    BaseTransformDialog.getFieldsFromPrevious(variables, wInputField, pipelineMeta, transformMeta);
    BaseTransformDialog.getFieldsFromPrevious(variables, wOutputField, pipelineMeta, transformMeta);
  }

  private void getData() {
    wInputSourceMode.setText(Utils.isEmpty(input.getInputSourceMode()) ? "LOCAL_FILE" : input.getInputSourceMode());
    wInputValueMode.setText(Utils.isEmpty(input.getInputValueMode()) ? "CONSTANT" : input.getInputValueMode());
    wInputValue.setText(Utils.isEmpty(input.getInputValue()) ? "" : input.getInputValue());
    wInputField.setText(Utils.isEmpty(input.getInputField()) ? "" : input.getInputField());
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
    wOutputFormat.setText(Utils.isEmpty(input.getOutputFormat()) ? "GTiff" : input.getOutputFormat());
    wBandSelection.setText(Utils.isEmpty(input.getBandSelection()) ? "" : input.getBandSelection());
    wOutputDataType.setText(Utils.isEmpty(input.getOutputDataType()) ? "" : input.getOutputDataType());
    wPixelWindow.setText(Utils.isEmpty(input.getPixelWindow()) ? "" : input.getPixelWindow());
    wCoordinateWindow.setText(Utils.isEmpty(input.getCoordinateWindow()) ? "" : input.getCoordinateWindow());
    wOutputNoData.setText(Utils.isEmpty(input.getOutputNoData()) ? "" : input.getOutputNoData());
    wCreationOptions.setText(Utils.isEmpty(input.getCreationOptions()) ? "" : input.getCreationOptions());
    wAdditionalArgs.setText(Utils.isEmpty(input.getAdditionalTranslateArgs()) ? "" : input.getAdditionalTranslateArgs());
    wOverwrite.setSelection(input.isOverwrite());
    wScale.setSelection(input.isScale());
    wUnscale.setSelection(input.isUnscale());
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
            w -> wInputSourceMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Input value mode",
            middle,
            margin,
            w -> wInputValueMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        rowWithButton(
            content,
            last,
            "Input raster / URL / VSI path",
            middle,
            margin,
            w -> wInputValue = (TextVar) w,
            b -> wbInput = b,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    row(
        content,
        last,
        "Input field",
        middle,
        margin,
        w -> wInputField = (ComboVar) w,
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
            "Output value mode",
            middle,
            margin,
            w -> wOutputValueMode = (ComboVar) w,
            parent -> new ComboVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        rowWithButton(
            content,
            last,
            "Output raster / VSI path",
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
            "Band selection",
            middle,
            margin,
            w -> wBandSelection = (TextVar) w,
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
            "Pixel window",
            middle,
            margin,
            w -> wPixelWindow = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Coordinate window",
            middle,
            margin,
            w -> wCoordinateWindow = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Output nodata",
            middle,
            margin,
            w -> wOutputNoData = (TextVar) w,
            parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
    last =
        row(
            content,
            last,
            "Overwrite",
            middle,
            margin,
            w -> wOverwrite = (Button) w,
            parent -> new Button(parent, SWT.CHECK));
    last =
        row(
            content,
            last,
            "Scale",
            middle,
            margin,
            w -> wScale = (Button) w,
            parent -> new Button(parent, SWT.CHECK));
    row(
        content,
        last,
        "Unscale",
        middle,
        margin,
        w -> wUnscale = (Button) w,
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
    row(
        content,
        last,
        "Additional translate args",
        middle,
        margin,
        w -> wAdditionalArgs = (TextVar) w,
        parent -> new TextVar(variables, parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER));
  }

  private void refreshEnabledStates() {
    boolean constantInput = !"FIELD".equalsIgnoreCase(wInputValueMode.getText());
    boolean constantOutput = !"FIELD".equalsIgnoreCase(wOutputValueMode.getText());
    boolean localInput = "LOCAL_FILE".equalsIgnoreCase(wInputSourceMode.getText());
    boolean localOutput = "LOCAL_FILE".equalsIgnoreCase(wOutputSourceMode.getText());
    RasterDialogUiSupport.setValueModeState(
        wInputValue, wbInput, wInputField, constantInput, localInput);
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
    input.setInputSourceMode(wInputSourceMode.getText());
    input.setInputValueMode(wInputValueMode.getText());
    input.setInputValue(wInputValue.getText());
    input.setInputField(wInputField.getText());
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
    input.setOutputFormat(wOutputFormat.getText());
    input.setOverwrite(wOverwrite.getSelection());
    input.setBandSelection(wBandSelection.getText());
    input.setOutputDataType(wOutputDataType.getText());
    input.setScale(wScale.getSelection());
    input.setUnscale(wUnscale.getSelection());
    input.setPixelWindow(wPixelWindow.getText());
    input.setCoordinateWindow(wCoordinateWindow.getText());
    input.setOutputNoData(wOutputNoData.getText());
    input.setCreationOptions(wCreationOptions.getText());
    input.setAdditionalTranslateArgs(wAdditionalArgs.getText());
    dispose();
  }

  private void cancel() {
    transformName = null;
    dispose();
  }

  private void browse(TextVar target, int mode) {
    FileDialog dialog = new FileDialog(shell, mode);
    String selected = dialog.open();
    if (selected != null) {
      target.setText(selected);
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
