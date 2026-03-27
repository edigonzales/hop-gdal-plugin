package ch.so.agi.hop.gdal.raster.core;

import java.util.List;
import java.util.function.Supplier;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TextVar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public final class RasterDialogUiSupport {
  private RasterDialogUiSupport() {}

  public static Composite createRow(Composite parent, Control under, int margin) {
    Composite row = new Composite(parent, SWT.NONE);
    PropsUi.setLook(row);
    FormLayout rowLayout = new FormLayout();
    rowLayout.marginWidth = 0;
    rowLayout.marginHeight = 0;
    row.setLayout(rowLayout);

    FormData fdRow = new FormData();
    fdRow.left = new FormAttachment(0, 0);
    fdRow.right = new FormAttachment(100, 0);
    fdRow.top = under == null ? new FormAttachment(0, 0) : new FormAttachment(under, margin);
    row.setLayoutData(fdRow);
    return row;
  }

  public static void buildRowControl(
      Composite row, String labelText, Control control, int middlePct, int margin) {
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

  public static void buildRowControlWithButton(
      Composite row,
      String labelText,
      Control control,
      Button actionButton,
      String buttonLabel,
      int middlePct,
      int margin) {
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

  public static void buildRowMultiline(
      Composite row, String labelText, Control control, int middlePct, int margin, int height) {
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

  public static void setControlEnabled(Control control, boolean enabled) {
    if (control == null || control.isDisposed()) {
      return;
    }
    control.setEnabled(enabled);
  }

  public static void setTextEditable(Control control, boolean editable) {
    if (control == null || control.isDisposed()) {
      return;
    }
    if (control instanceof TextVar textVar) {
      textVar.setEnabled(true);
      textVar.setEditable(editable);
      return;
    }
    if (control instanceof Text text) {
      text.setEnabled(true);
      text.setEditable(editable);
    }
  }

  public static void setValueModeState(
      Control constantControl,
      Button browseButton,
      Control fieldControl,
      boolean constantMode,
      boolean browseEnabled) {
    setValueModeState(constantControl, browseButton, fieldControl, true, constantMode, browseEnabled);
  }

  public static void setValueModeState(
      Control constantControl,
      Button browseButton,
      Control fieldControl,
      boolean active,
      boolean constantMode,
      boolean browseEnabled) {
    if (constantControl != null) {
      setControlEnabled(constantControl, true);
      setTextEditable(constantControl, active && constantMode);
    }
    if (browseButton != null) {
      setControlEnabled(browseButton, active && constantMode && browseEnabled);
    }
    if (fieldControl != null) {
      setControlEnabled(fieldControl, active && !constantMode);
    }
  }

  public static void setAuthState(
      String authType,
      Control usernameControl,
      Control passwordControl,
      Control bearerTokenControl,
      Control headerNameControl,
      Control headerValueControl) {
    boolean basicAuth = "BASIC_AUTH".equalsIgnoreCase(authType);
    boolean bearerToken = "BEARER_TOKEN".equalsIgnoreCase(authType);
    boolean customHeader = "CUSTOM_HEADER".equalsIgnoreCase(authType);
    setTextEditable(usernameControl, basicAuth);
    setTextEditable(passwordControl, basicAuth);
    setTextEditable(bearerTokenControl, bearerToken);
    setTextEditable(headerNameControl, customHeader);
    setTextEditable(headerValueControl, customHeader);
  }

  public static void refreshCompressionPresetChoices(
      ComboVar compressionPresetControl, String outputFormat, String requestedSelection) {
    if (compressionPresetControl == null || compressionPresetControl.isDisposed()) {
      return;
    }

    java.util.List<String> choices = new java.util.ArrayList<>();
    choices.add(RasterOutputOptionsSupport.COMPRESSION_DEFAULT);
    choices.addAll(RasterFormatCatalog.compressionOptions(outputFormat));
    compressionPresetControl.setItems(choices.toArray(String[]::new));

    String selected = requestedSelection;
    if (selected == null || selected.isBlank()) {
      selected = compressionPresetControl.getText();
    }

    boolean supported = false;
    if (selected != null && !selected.isBlank()) {
      for (String choice : choices) {
        if (choice.equalsIgnoreCase(selected)) {
          supported = true;
          break;
        }
      }
    }
    if (!supported) {
      selected = RasterOutputOptionsSupport.COMPRESSION_DEFAULT;
    }
    compressionPresetControl.setText(selected);
  }

  public static void openManagedDialog(
      Shell shell,
      String stateKey,
      int minWidth,
      int minHeight,
      Runnable okAction,
      Supplier<Boolean> cancelSupplier) {
    shell.addListener(SWT.Close, e -> e.doit = cancelSupplier.get());
    shell.addListener(SWT.Dispose, e -> saveWindowState(shell, stateKey));
    BaseDialog.addDefaultListeners(shell, c -> okAction.run());
    BaseDialog.addSpacesOnTabs(shell);
    applyWindowState(shell, stateKey, minWidth, minHeight);

    shell.open();
    Display display = shell.getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }

  public static TabSection createTabSection(TabFolder folder, String title) {
    TabItem item = new TabItem(folder, SWT.NONE);
    item.setText(title);

    ScrolledComposite scroller = new ScrolledComposite(folder, SWT.V_SCROLL | SWT.H_SCROLL);
    PropsUi.setLook(scroller);
    scroller.setExpandHorizontal(true);
    scroller.setExpandVertical(true);
    scroller.setAlwaysShowScrollBars(false);
    scroller.setShowFocusedControl(true);

    Composite content = new Composite(scroller, SWT.NONE);
    PropsUi.setLook(content);
    FormLayout contentLayout = new FormLayout();
    contentLayout.marginWidth = 0;
    contentLayout.marginHeight = 0;
    content.setLayout(contentLayout);
    scroller.setContent(content);
    item.setControl(scroller);
    return new TabSection(item, scroller, content);
  }

  public static void refreshTabLayouts(TabFolder folder, Shell shell, List<TabSection> sections) {
    if (sections != null) {
      for (TabSection section : sections) {
        refreshTabLayout(section);
      }
    }
    if (folder != null && !folder.isDisposed()) {
      folder.layout(true, true);
    }
    if (shell != null && !shell.isDisposed()) {
      shell.layout(true, true);
    }
  }

  private static Label createRowLabel(Composite row, String labelText) {
    Label label = new Label(row, SWT.RIGHT);
    label.setText(labelText);
    PropsUi.setLook(label);
    return label;
  }

  private static void refreshTabLayout(TabSection section) {
    if (section == null
        || section.content() == null
        || section.content().isDisposed()
        || section.scroller() == null
        || section.scroller().isDisposed()) {
      return;
    }
    section.content().layout(true, true);
    int widthHint = section.scroller().getClientArea().width;
    Point size =
        section.content().computeSize(widthHint > 0 ? widthHint : SWT.DEFAULT, SWT.DEFAULT, true);
    section.scroller().setMinSize(size);
  }

  private static void applyWindowState(Shell shell, String stateKey, int minWidth, int minHeight) {
    PropsUi props = PropsUi.getInstance();
    WindowProperty windowProperty = props.getScreen(stateKey);
    if (windowProperty != null) {
      windowProperty.setShell(shell, minWidth, minHeight);
      return;
    }

    shell.layout(true, true);
    Rectangle bounds = shell.getBounds();
    windowProperty =
        new WindowProperty(
            stateKey,
            shell.getMaximized(),
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height);
    windowProperty.setShell(shell, minWidth, minHeight);

    Rectangle shellBounds = shell.getBounds();
    Monitor monitor = shell.getDisplay().getPrimaryMonitor();
    if (shell.getParent() != null) {
      monitor = shell.getParent().getMonitor();
    }
    Rectangle clientArea = monitor.getClientArea();
    int middleX = clientArea.x + (clientArea.width - shellBounds.width) / 2;
    int middleY = clientArea.y + (clientArea.height - shellBounds.height) / 2;
    shell.setLocation(middleX, middleY);
  }

  private static void saveWindowState(Shell shell, String stateKey) {
    if (shell == null) {
      return;
    }
    Rectangle bounds = shell.getBounds();
    PropsUi.getInstance()
        .setScreen(
            new WindowProperty(
                stateKey,
                shell.getMaximized(),
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height));
  }

  public record TabSection(TabItem item, ScrolledComposite scroller, Composite content) {}
}
