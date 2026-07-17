/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package TimeTracker.gui;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import TimeTracker.Defaults;
import TimeTracker.Registry;
import TimeTracker.data.Configuration;
import TimeTracker.data.Session;
import TimeTracker.util.GlobalHotkey;
import TimeTracker.util.Language;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * FXML Controller class.<p>
 *
 * @author Matthias Grimm 
 */
public class MainWindowController
extends WindowFX
{
    @FXML  private Button  btnHide;
    @FXML  private Label  errorMsg;
    @FXML  private MenuItem miOpen;
    @FXML  private MenuItem miExit;
    @FXML  private MenuItem miExport;
    @FXML  private MenuItem miAbout;

    @FXML  private TextField textSessionDay;
    @FXML  private TextField textSessionDate;
    @FXML  private TextField textSessionStart;
    @FXML  private TextField textSessionEnd;
    @FXML  private TextField textSessionDur;

    // Tab View: Week
    @FXML  private TableView<Session> tableWeek;
    @FXML  private TableColumn<Session, String> colDay;
    @FXML  private TableColumn<Session, LocalDate> colDate;
    @FXML  private TableColumn<Session, LocalTime> colStart;
    @FXML  private TableColumn<Session, LocalTime> colEnd;
    @FXML  private TableColumn<Session, Duration> colDuration;
    @FXML  private TableColumn<Session, Duration> colWorkTime;

    // Tab View: Config
    @FXML  private TextField cfgDBPath;
    @FXML  private TextField cfgHotkey;
    @FXML  private TextField cfgBreakTime;
    @FXML  private TextField cfgBreakLength;
    @FXML  private ToggleButton btnLearnHotkey;
    @FXML  private CheckBox checkHideAtStart;
    @FXML  private CheckBox checkWDSaturday;
    @FXML  private CheckBox checkWDSunday;
    @FXML  private CheckBox checkHasBreak;

    private final MainWindowData dataModel;
    private final DoubleProperty weekTableBarWidthProperty = new SimpleDoubleProperty();

    /** True while {@link #btnLearnHotkey} is toggled and we record a new combo. */
    private boolean learningHotkey = false;

    /** Active native-key recorder while learning; null when not learning. */
    private GlobalHotkey.Learner learner;

    public MainWindowController(Stage stage) throws IOException
    {
        super(stage, "MainWindow.fxml", "timetracker.css");
        stage.setTitle(String.format("Time Tracker V%d.%d%s",
                Defaults.APP_VERSION, Defaults.APP_REVISION, Defaults.APP_SUFFIX));
        stage.setResizable(true);
        setMsgLabel(errorMsg);
        
        dataModel = new MainWindowData(); // create dialogue data model

        dataModel.errorMsgProperty().addListener(
            (ObservableValue<? extends String> ov, String oldVal, String newVal) -> {
                showError(newVal);
            });
        dataModel.successMsgProperty().addListener(
            (ObservableValue<? extends String> ov, String oldVal, String newVal) -> {
                showSuccess(newVal);
            });
     
        // Configuration
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();
        Language i18n = Reg.getI18N();

        cfgDBPath.setText(Config.getDBPath().toString());
        cfgHotkey.setText(GlobalHotkey.format(Config.getHotkey()));
        cfgBreakTime.setText(String.format("%.1f", Config.getBreakTime()));
        cfgBreakLength.setText(String.format("%d",Config.getBreakLength()));
        checkHideAtStart.setSelected(Config.getHideAtStart());
        checkHasBreak.setSelected(Config.hasBreak());

        UnaryOperator<Change> filterFloat = change -> {
                String text = change.getText();
                char sep = DecimalFormatSymbols.getInstance(i18n.locale()).getDecimalSeparator();
                return (text.matches("[0-9\\" + String.valueOf(sep) + "]*")) ? change : null;
            };
        cfgBreakTime.setTextFormatter(new TextFormatter<String>(filterFloat));
        cfgBreakTime.focusedProperty().addListener((obs, hadFocus, hasFocus) -> {
                if (hadFocus && !hasFocus) saveBreakTime();
            });

        UnaryOperator<Change> filterDigits = change -> {
                String text = change.getText();
                return (text.matches("[0-9]*")) ? change : null;
            };
        cfgBreakLength.setTextFormatter(new TextFormatter<String>(filterDigits));
        cfgBreakLength.focusedProperty().addListener((obs, hadFocus, hasFocus) -> {
                if (hadFocus && !hasFocus) saveBreakLength();
            });

        
        // Current Session
        Session session = Reg.getSession();
        textSessionDay.textProperty().addListener((obs, oldText, newText) -> updateWeekDayStyles());
        textSessionDay.textProperty().bind(session.DayProperty());
        textSessionDate.textProperty().bind((session.DateProperty()));
        textSessionStart.textProperty().bind(session.StartProperty());
        textSessionEnd.textProperty().bind(session.EndProperty());
        textSessionDur.textProperty().bind(dataModel.elapsedTimeProperty());

        // Table Week
        colDay.setCellValueFactory(new PropertyValueFactory<>("DayName"));
        colDay.setCellFactory(formatWeekDay);
        colDay.getStyleClass().add("column-align-left");

        colDate.setCellValueFactory(new PropertyValueFactory<>("StartDate"));
        colDate.setCellFactory(formatDate);
        colDate.getStyleClass().add("column-align-left");

        colStart.setCellValueFactory(new PropertyValueFactory<>("StartTime"));
        colStart.setCellFactory(formatTime);
        colStart.getStyleClass().add("column-align-left");

        colEnd.setCellValueFactory(new PropertyValueFactory<>("EndTime"));
        colEnd.setCellFactory(formatTime);
        colEnd.getStyleClass().add("column-align-left");

        colDuration.setCellValueFactory(new PropertyValueFactory<>("Duration"));
        colDuration.setCellFactory(formatDuration);
        colDuration.getStyleClass().add("column-align-center");

        colWorkTime.setCellValueFactory(new PropertyValueFactory<>("WorkTime"));
        colWorkTime.setCellFactory(formatDuration);
        colWorkTime.getStyleClass().add("column-align-center");

        colWorkTime.prefWidthProperty().bind(tableWeek.widthProperty()
                                    .subtract(colStart.widthProperty())
                                    .subtract(colDate.widthProperty())
                                    .subtract(colEnd.widthProperty())
                                    .subtract(colDay.widthProperty())
                                    .subtract(colDuration.widthProperty())
                                    .subtract(weekTableBarWidthProperty)
                                    .subtract(2));

        tableWeek.setItems(dataModel.getSessionListWeek());

        stage.setOnShown(ev -> {
            adjustTableWidth(getVerticalScrollbar(tableWeek), weekTableBarWidthProperty);
        });

        if (Reg.getSystemTray() != null) {
            MenuItem miTrayExit = new MenuItem(i18n("trayicon.exit"));
		    miTrayExit.setOnAction(e -> close());
            Reg.getSystemTray().addMenuItem(miTrayExit);
        }
    }
   
    private void adjustTableWidth(ScrollBar bar, DoubleProperty width)
    {
        if (bar == null) return;

        width.set(bar.visibleProperty().get() ? bar.getWidth() : 0);
        bar.visibleProperty().addListener((obs, oldVal, newVal) -> {
                width.set(newVal ? bar.getWidth() : 0);
            });
    }

    /**
     * The close() nmethod overwrites the close() method of WindowsFX and adds a
     * confirmation dialog, so the user has the chance to change his mind.
     */
    @Override
    protected void close()
    {
        int rc = Notification.getDecission(i18n("dlg.exit.header"),
                                           i18n("dlg.exit.message"),
                                           i18n("dlg.exit.opta"),
                                           i18n("dlg.exit.optb"));
        if (rc == 0) {
            storePosition();
            Platform.exit();
        }
    }

    /**
     * This method hides the application. Because JavaFX does not distinguish between close() and hide()
     * and will terminate the application when it's last window has been closed, hiding will only work,
     * if  Platform.setImplicitExit(false); is set.
     */
    private void hide()
    {
        storePosition();
        super.close();
    }

    // ---------------------------------------------------------------------------------------- 
    //                                      FXML GUI handler
    // ---------------------------------------------------------------------------------------- 
    @FXML
    protected void handleAction(ActionEvent ev)
    {
        if (learningHotkey) return;

        if (ev.getSource() == btnHide) hide();
        if (ev.getSource() == btnLearnHotkey) learnHotkey();
        if (ev.getSource() == cfgBreakTime) cfgBreakLength.requestFocus();  //saveBreakTime();
        if (ev.getSource() == cfgBreakLength) cfgBreakTime.requestFocus();  //saveBreakLength();
        if (ev.getSource() == checkHideAtStart) saveOption(0);
        if (ev.getSource() == checkWDSaturday) saveOption(1);
        if (ev.getSource() == checkWDSunday) saveOption( 2);
        if (ev.getSource() == checkHasBreak) saveOption( 3);
    }

    @FXML
    protected void handleKeys(KeyEvent ev)
    {
        if (ev.getEventType() == KeyEvent.KEY_PRESSED) {
            if (ev.getCode() == KeyCode.ENTER) {
                if (ev.getSource() == btnHide) hide();
                if (ev.getSource() == btnLearnHotkey) learnHotkey();
            }
        } else if (ev.getEventType() == KeyEvent.KEY_TYPED) {
            String str = ev.getCharacter();
            for (int n = 0; n < str.length(); n++) {
                char c = str.charAt(n);
                if (Character.isLetterOrDigit(c) || " -_()".indexOf(c) >= 0) {
                    clearMessage();
                }
            }
        }
    }

    @FXML
    protected void handleMenus(ActionEvent event) throws IOException
    {
        if (learningHotkey) return;

        // File Menu
        if (event.getSource() == miExit) {
            close();

        } else if (event.getSource() == miExport) {
            try {
                dataModel.exportSessions();
                showSuccess(i18n("success.export"));

            } catch (SQLException | IOException e) {
                showError(i18n("error.export", e.getLocalizedMessage()));
            }

        // Help Menu
        } else if (event.getSource() == miAbout) {
            AboutController ctrl = new AboutController();
            ctrl.show();
        }
    }   

    /**
     * Handles the event of an pressed Hotkey.
     */
    public void handleHotkey()
    {
        if (stage.isShowing())
            hide();
        else
            show();
    }

    // ---------------------------------------------------------------------------------------- 
    //                                      Main Window Function
    // ---------------------------------------------------------------------------------------- 

    private void saveBreakTime()
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();
        Language i18n = Reg.getI18N();

        NumberFormat nf = NumberFormat.getInstance(i18n.locale());
        try {
            float hours = nf.parse(cfgBreakTime.getText().trim()).floatValue();
            if (hours <= 1 || hours >= 24)
                 throw(new ParseException(i18n("error.breaktime", String.format("%.1f",hours)),0));

            Config.setBreakTime(hours);
            clearMessage();

        } catch (ParseException e) {
            showError(e.getLocalizedMessage());
            cfgBreakTime.setText(String.format("%.1f",Config.getBreakTime()));
        }
    }
    
    /**
     * Validates and persists the current content of the break time field.
     * Non-numeric or negative input is rejected: the field is restored
     * to the stored value and the user is informed.
     */
    private void saveBreakLength()
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        int minutes = Integer.parseInt(cfgBreakLength.getText().trim());
        if (minutes > 180) {
            cfgBreakLength.setText(String.format("%d", Config.getBreakLength()));
            showError(i18n("error.breaklength"));
        } else {
            Config.setBreakLength(minutes);
            clearMessage();
        }
    }

    /**
     * Save the current state of the checkbox options to the Configuration.
     * After one minute the Configuration will be written to the database.
     */
    private void saveOption(int opt)
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        switch (opt) {
            case 0:         // HideAtStart
                Config.setHideAtStart(checkHideAtStart.isSelected());
                break;
            case 1:         // WDSaturday
                Config.setWDSaturday(checkWDSaturday.isSelected());
                break;
            case 2:         // WDSunday
                Config.setWDSunday(checkWDSunday.isSelected());
                break;
            case 3:
                Config.setHasBreak(checkHasBreak.isSelected());
                break;
            default:
                break;
        }
        tableWeek.refresh();
        updateWeekDayStyles();
    }

    /**
     * Handles clicks on the learn toggle button. Toggling it on starts the
     * recording of a new hotkey; toggling it off again aborts and restores the
     * currently stored combination.
     */
    private void learnHotkey()
    {
        if (btnLearnHotkey.isSelected()) {
            learningHotkey = true;
            cfgHotkey.setText(i18n("hotkey.recording"));
            showMessage(i18n("hotkey.howto"));
            learner = new GlobalHotkey.Learner(this::onLearnKey);
            learner.start();
        } else {
            stopLearning();
        }
    }

    /**
     * Leaves learning mode without storing anything and puts the currently
     * configured combination back into the text field.
     */
    private void stopLearning()
    {
        learningHotkey = false;
        btnLearnHotkey.setSelected(false);

        if (learner != null) {
            learner.stop();
            learner = null;
        }

        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        cfgHotkey.setText(GlobalHotkey.format(Config.getHotkey()));
        clearMessage();
    }

    /**
     * Processes a key press reported by the {@link GlobalHotkey.Learner} while
     * learning mode is active. Runs on the JavaFX thread. Modifier-only presses
     * update the preview; ESC cancels; the first non-modifier key completes the
     * combination (a bare key is accepted only for function keys), which is then
     * stored, re-targeted on the live hook and printed into {@link #cfgHotkey}.
     *
     * @param packedCombo  the pressed combination in packed form
     * @param modifierOnly true while only modifier keys are held
     */
    private void onLearnKey(int packedCombo, boolean modifierOnly)
    {
        if (!learningHotkey)
            return;

        // Wait for the actual key: show the modifiers gathered so far.
        if (modifierOnly) {
            cfgHotkey.setText(GlobalHotkey.format(packedCombo) + "…");
            return;
        }

        int keyCode   = packedCombo & 0xFFFF;
        int modifiers = packedCombo >>> 16;

        if (keyCode == NativeKeyEvent.VC_ESCAPE) {
            stopLearning();
            return;
        }

        if (modifiers == 0 && !GlobalHotkey.isFunctionKey(keyCode)) {
            showError(i18n("error.hotkey"));
            return;   // keep listening
        }

        // Full combination captured: leave learning mode, then apply it (updates
        // the config, re-targets the live hook and persists it to the database).
        learningHotkey = false;
        btnLearnHotkey.setSelected(false);
        learner.stop();
        learner = null;

        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();
        Config.setHotkey(packedCombo);
        cfgHotkey.setText(GlobalHotkey.format(packedCombo));
        showSuccess(i18n("success.hotkey", GlobalHotkey.format(packedCombo)));
    }

    // ---------------------------------------------------------------------------------------- 
    //                                      JavaFX Callbacks
    // ---------------------------------------------------------------------------------------- 

    private Callback<TableColumn<Session, String>, TableCell<Session, String>> formatWeekDay = (tableColumn) -> {
        TableCell<Session, String> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                this.getStyleClass().remove("bankholiday");

                Session session = this.getTableRow().getItem();
                if (empty || item == null || session == null) return;

                this.setText(item);
                if (session.isBankHoliday())
                    this.getStyleClass().add("bankholiday");
            }
        };
        return tableCell;
    };

    private void updateWeekDayStyles()
    {
        Registry Reg = Registry.get();
        Session session = Reg.getSession();

        textSessionDay.getStyleClass().remove("bankholiday");
        if (session.isBankHoliday())
            textSessionDay.getStyleClass().add("bankholiday");
    }

    private Callback<TableColumn<Session, LocalDate>, TableCell<Session, LocalDate>> formatDate = (tableColumn) -> {
        TableCell<Session, LocalDate> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if (empty || item == null) return;

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                this.setText(item.format(fmt));
            }
        };
        return tableCell;
    };

    private Callback<TableColumn<Session, LocalTime>, TableCell<Session, LocalTime>> formatTime = (tableColumn) -> {
        TableCell<Session, LocalTime> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if (empty || item == null) return;

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                this.setText(item.format(fmt));
            }
        };
        return tableCell;
    };

    private Callback<TableColumn<Session, Duration>, TableCell<Session, Duration>> formatDuration = (tableColumn) -> {
        TableCell<Session, Duration> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(Duration item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if (empty || item == null) return;

                this.setText(String.format("%02d:%02d", item.toHours(), item.toMinutesPart()));
            }
        };
        return tableCell;
    };

}
