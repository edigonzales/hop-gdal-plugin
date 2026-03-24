package ch.so.agi.hop.gdal.raster.core;

import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.core.PropsUi;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import java.util.List;

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
    if (constantControl != null) {
      setControlEnabled(constantControl, true);
      setTextEditable(constantControl, constantMode);
    }
    if (browseButton != null) {
      setControlEnabled(browseButton, constantMode && browseEnabled);
    }
    if (fieldControl != null) {
      setControlEnabled(fieldControl, !constantMode);
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

  public record TabSection(TabItem item, ScrolledComposite scroller, Composite content) {}
}
