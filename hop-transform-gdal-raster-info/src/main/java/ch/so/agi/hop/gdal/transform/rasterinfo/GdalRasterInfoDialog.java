package ch.so.agi.hop.gdal.transform.rasterinfo;

import ch.so.agi.hop.gdal.raster.core.DefaultRasterGdalClient;
import ch.so.agi.hop.gdal.raster.core.DatasetRef;
import ch.so.agi.hop.gdal.raster.core.RasterDialogUiSupport;
import ch.so.agi.hop.gdal.raster.core.RasterTransformSupport;
import ch.so.agi.hop.gdal.raster.core.RemoteAccessSpec;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.ErrorDialog;
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

public class GdalRasterInfoDialog extends BaseTransformDialog {
  private static final Class<?> PKG = GdalRasterInfoMeta.class;
  private static final String WINDOW_STATE_KEY = "hop-gdal-raster-info-dialog-v2";

  private final GdalRasterInfoMeta input;
  private final DefaultRasterGdalClient gdalClient = new DefaultRasterGdalClient();

  private ComboVar wInputSourceMode;
  private ComboVar wInputValueMode;
  private TextVar wInputValue;
  private Button wbInputValue;
  private ComboVar wInputField;
  private ComboVar wAuthType;
  private TextVar wAuthUsername;
  private TextVar wAuthPassword;
  private TextVar wBearerToken;
  private TextVar wHeaderName;
  private TextVar wHeaderValue;
  private TextVar wGdalConfigOptions;
  private TextVar wAdditionalInfoArgs;
  private Button wFailOnError;
  private Button wAddResultFields;
  private TabFolder wTabFolder;
  private List<RasterDialogUiSupport.TabSection> tabSections = List.of();

  public GdalRasterInfoDialog(
      Shell parent, IVariables variables, GdalRasterInfoMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);
    this.input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();
    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    shell.setMinimumSize(820, 660);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();
    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "GdalRasterInfoDialog.Shell.Title"));

    int margin = PropsUi.getMargin();

    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.TransformName.Label"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(props.getMiddlePct(), -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);

    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(props.getMiddlePct(), 0);
    fdTransformName.right = new FormAttachment(100, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    wTransformName.setLayoutData(fdTransformName);

    Button wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    Button wTest = new Button(shell, SWT.PUSH);
    wTest.setText(BaseMessages.getString(PKG, "GdalRasterInfoDialog.Test.Button"));

    setButtonPositions(new Button[] {wOk, wCancel, wTest}, margin, null);

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
    RasterDialogUiSupport.TabSection remoteTab =
        RasterDialogUiSupport.createTabSection(wTabFolder, "Remote access");
    RasterDialogUiSupport.TabSection advancedTab =
        RasterDialogUiSupport.createTabSection(wTabFolder, "Advanced");
    tabSections = List.of(inputTab, remoteTab, advancedTab);

    buildInputTab(inputTab.content(), middle, margin);
    buildRemoteTab(remoteTab.content(), middle, margin);
    buildAdvancedTab(advancedTab.content(), middle, margin);

    populateCombos();
    getData();
    loadFieldNames();

    wInputSourceMode.addModifyListener(e -> refreshEnabledStates());
    wInputValueMode.addModifyListener(e -> refreshEnabledStates());
    wAuthType.addModifyListener(e -> refreshEnabledStates());
    wTabFolder.addListener(SWT.Selection, e -> refreshTabLayouts());
    shell.addListener(SWT.Resize, e -> refreshTabLayouts());
    refreshEnabledStates();

    wbInputValue.addListener(SWT.Selection, e -> browseInput());
    wOk.addListener(SWT.Selection, e -> ok());
    wCancel.addListener(SWT.Selection, e -> cancel());
    wTest.addListener(SWT.Selection, e -> testDataset());

    RasterDialogUiSupport.openManagedDialog(
        shell, WINDOW_STATE_KEY, 820, 660, this::ok, () -> {
          cancel();
          return true;
        });
    return transformName;
  }

  private void populateCombos() {
    wInputSourceMode.setItems(new String[] {"LOCAL_FILE", "HTTP_URL", "GDAL_VSI"});
    wInputValueMode.setItems(new String[] {"CONSTANT", "FIELD"});
    wAuthType.setItems(new String[] {"NONE", "BASIC_AUTH", "BEARER_TOKEN", "SIGNED_URL", "CUSTOM_HEADER"});
  }

  private void loadFieldNames() {
    BaseTransformDialog.getFieldsFromPrevious(variables, wInputField, pipelineMeta, transformMeta);
  }

  private void browseInput() {
    if (!"LOCAL_FILE".equalsIgnoreCase(wInputSourceMode.getText())) {
      return;
    }
    FileDialog dialog = new FileDialog(shell, SWT.OPEN);
    String selected = dialog.open();
    if (selected != null) {
      wInputValue.setText(selected);
    }
  }

  private void getData() {
    wInputSourceMode.setText(Utils.isEmpty(input.getInputSourceMode()) ? "LOCAL_FILE" : input.getInputSourceMode());
    wInputValueMode.setText(Utils.isEmpty(input.getInputValueMode()) ? "CONSTANT" : input.getInputValueMode());
    wInputValue.setText(Utils.isEmpty(input.getInputValue()) ? "" : input.getInputValue());
    wInputField.setText(Utils.isEmpty(input.getInputField()) ? "" : input.getInputField());
    wAuthType.setText(Utils.isEmpty(input.getAuthType()) ? "NONE" : input.getAuthType());
    wAuthUsername.setText(Utils.isEmpty(input.getAuthUsername()) ? "" : input.getAuthUsername());
    wAuthPassword.setText(Utils.isEmpty(input.getAuthPassword()) ? "" : input.getAuthPassword());
    wBearerToken.setText(Utils.isEmpty(input.getBearerToken()) ? "" : input.getBearerToken());
    wHeaderName.setText(Utils.isEmpty(input.getCustomHeaderName()) ? "" : input.getCustomHeaderName());
    wHeaderValue.setText(Utils.isEmpty(input.getCustomHeaderValue()) ? "" : input.getCustomHeaderValue());
    wGdalConfigOptions.setText(Utils.isEmpty(input.getGdalConfigOptions()) ? "" : input.getGdalConfigOptions());
    wAdditionalInfoArgs.setText(Utils.isEmpty(input.getAdditionalInfoArgs()) ? "" : input.getAdditionalInfoArgs());
    wFailOnError.setSelection(input.isFailOnError());
    wAddResultFields.setSelection(input.isAddResultFields());
    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void buildInputTab(Composite content, int middle, int margin) {
    Composite lastRow = null;

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wInputSourceMode = new ComboVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.InputSourceMode.Label"),
        wInputSourceMode,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wInputValueMode = new ComboVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.InputValueMode.Label"),
        wInputValueMode,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wInputValue = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wbInputValue = new Button(lastRow, SWT.PUSH | SWT.CENTER);
    RasterDialogUiSupport.buildRowControlWithButton(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.InputValue.Label"),
        wInputValue,
        wbInputValue,
        BaseMessages.getString(PKG, "System.Button.Browse"),
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wInputField = new ComboVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.InputField.Label"),
        wInputField,
        middle,
        margin);
  }

  private void buildRemoteTab(Composite content, int middle, int margin) {
    Composite lastRow = null;

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wAuthType = new ComboVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow, BaseMessages.getString(PKG, "GdalRasterInfoDialog.AuthType.Label"), wAuthType, middle, margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wAuthUsername = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow, BaseMessages.getString(PKG, "GdalRasterInfoDialog.AuthUser.Label"), wAuthUsername, middle, margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wAuthPassword = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.PASSWORD);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.AuthPassword.Label"),
        wAuthPassword,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wBearerToken = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.BearerToken.Label"),
        wBearerToken,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wHeaderName = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.HeaderName.Label"),
        wHeaderName,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wHeaderValue = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.HeaderValue.Label"),
        wHeaderValue,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wGdalConfigOptions = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.GdalConfig.Label"),
        wGdalConfigOptions,
        middle,
        margin);
  }

  private void buildAdvancedTab(Composite content, int middle, int margin) {
    Composite lastRow = null;

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wAdditionalInfoArgs = new TextVar(variables, lastRow, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.AdditionalArgs.Label"),
        wAdditionalInfoArgs,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wFailOnError = new Button(lastRow, SWT.CHECK);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.FailOnError.Label"),
        wFailOnError,
        middle,
        margin);

    lastRow = RasterDialogUiSupport.createRow(content, lastRow, margin);
    wAddResultFields = new Button(lastRow, SWT.CHECK);
    RasterDialogUiSupport.buildRowControl(
        lastRow,
        BaseMessages.getString(PKG, "GdalRasterInfoDialog.AddResultFields.Label"),
        wAddResultFields,
        middle,
        margin);
  }

  private void refreshEnabledStates() {
    boolean constantInput = !"FIELD".equalsIgnoreCase(wInputValueMode.getText());
    boolean localInput = "LOCAL_FILE".equalsIgnoreCase(wInputSourceMode.getText());
    RasterDialogUiSupport.setValueModeState(
        wInputValue, wbInputValue, wInputField, constantInput, localInput);
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
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }
    transformName = wTransformName.getText();
    input.setInputSourceMode(wInputSourceMode.getText());
    input.setInputValueMode(wInputValueMode.getText());
    input.setInputValue(wInputValue.getText());
    input.setInputField(wInputField.getText());
    input.setAuthType(wAuthType.getText());
    input.setAuthUsername(wAuthUsername.getText());
    input.setAuthPassword(wAuthPassword.getText());
    input.setBearerToken(wBearerToken.getText());
    input.setCustomHeaderName(wHeaderName.getText());
    input.setCustomHeaderValue(wHeaderValue.getText());
    input.setGdalConfigOptions(wGdalConfigOptions.getText());
    input.setAdditionalInfoArgs(wAdditionalInfoArgs.getText());
    input.setFailOnError(wFailOnError.getSelection());
    input.setAddResultFields(wAddResultFields.getSelection());
    dispose();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void testDataset() {
    try {
      DatasetRef datasetRef =
          RasterTransformSupport.resolveDatasetRef(
              wInputSourceMode.getText(),
              wInputValueMode.getText(),
              wInputValue.getText(),
              wInputField.getText(),
              new Object[0],
              null,
              value -> value);
      RemoteAccessSpec remoteAccess =
          RasterTransformSupport.remoteAccessSpec(
              wAuthType.getText(),
              wAuthUsername.getText(),
              wAuthPassword.getText(),
              wBearerToken.getText(),
              wHeaderName.getText(),
              wHeaderValue.getText(),
              wGdalConfigOptions.getText(),
              value -> value);
      String json =
          gdalClient.rasterInfo(datasetRef, remoteAccess, java.util.List.of("--output-format", "json"));
      TextDialog.show(shell, BaseMessages.getString(PKG, "GdalRasterInfoDialog.Test.Title"), json);
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "GdalRasterInfoDialog.Test.ErrorTitle"),
          BaseMessages.getString(PKG, "GdalRasterInfoDialog.Test.ErrorMessage"),
          e);
    }
  }

  private static final class TextDialog {
    private TextDialog() {}

    static void show(Shell parent, String title, String value) {
      Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX);
      dialog.setText(title);
      dialog.setLayout(new FormLayout());
      dialog.setSize(760, 520);
      Text text = new Text(dialog, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      text.setEditable(false);
      text.setText(value == null ? "" : value);
      FormData fdText = new FormData();
      fdText.left = new FormAttachment(0, 8);
      fdText.top = new FormAttachment(0, 8);
      fdText.right = new FormAttachment(100, -8);
      fdText.bottom = new FormAttachment(100, -8);
      text.setLayoutData(fdText);
      dialog.open();
      while (!dialog.isDisposed()) {
        if (!dialog.getDisplay().readAndDispatch()) {
          dialog.getDisplay().sleep();
        }
      }
    }
  }
}
