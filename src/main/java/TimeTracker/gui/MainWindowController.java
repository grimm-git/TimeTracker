/*
 * Copyright (C) 2018 Matthias Grimm 
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import TimeTracker.Defaults;
import TimeTracker.GlobalHotkey;
import TimeTracker.Registry;
import TimeTracker.data.Session;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
    @FXML  private Button  btnClose;
    @FXML  private Label  errorMsg;
    @FXML  private MenuItem miOpen;
    @FXML  private MenuItem miExit;
    @FXML  private MenuItem miAbout;

    @FXML  private TextField textSessionStart;
    @FXML  private TextField textSessionEnd;

    // Tab View: Week
    @FXML  private TableView<Session> tableWeek;
    @FXML  private TableColumn<Session, String> colDay;
    @FXML  private TableColumn<Session, LocalDateTime> colStart;
    @FXML  private TableColumn<Session, LocalDateTime> colEnd;
    @FXML  private TableColumn<Session, Duration> colWorkTime;

    // Tab View: Config
    @FXML  private TextField cfgDBPath;
    @FXML  private TextField cfgHotkey;
    @FXML  private TextField cfgBreaktime;
    @FXML  private ToggleButton btnLearnHotkey;

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
        cfgDBPath.setText(Reg.getDatabasePath().toString());
        cfgHotkey.setText(GlobalHotkey.format(Reg.getHotkey()));
        cfgBreaktime.setText(String.format("%d",Reg.getBreakTime()));
        
        // Current Session
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm");
        Session current = dataModel.getCurrentSession();
        textSessionStart.setText(current.getSessionStart().format(fmt));
        if (current.isSessonFinished())
            textSessionEnd.setText(current.getSessionEnd().format(fmt));

        // Table Week
        colDay.setCellValueFactory(new PropertyValueFactory<>("DayName"));
        colDay.getStyleClass().add("column-align-left");

        colStart.setCellValueFactory(new PropertyValueFactory<>("SessionStart"));
        colStart.setCellFactory(formatDateTime);
        colStart.getStyleClass().add("column-align-right");

        colEnd.setCellValueFactory(new PropertyValueFactory<>("SessionEnd"));
        colEnd.setCellFactory(formatDateTime);
        colEnd.getStyleClass().add("column-align-right");

        colWorkTime.setCellValueFactory(new PropertyValueFactory<>("WorkTime"));
        colWorkTime.setCellFactory(formatDuration);
        colWorkTime.getStyleClass().add("column-align-center");

        colWorkTime.prefWidthProperty().bind(tableWeek.widthProperty()
                                    .subtract(colStart.widthProperty())
                                    .subtract(colEnd.widthProperty())
                                    .subtract(colDay.widthProperty())
                                    .subtract(weekTableBarWidthProperty)
                                    .subtract(2));

        tableWeek.setItems(dataModel.getSessionListWeek());

        stage.setOnShown(ev -> {
            adjustTableWidth(getVerticalScrollbar(tableWeek), weekTableBarWidthProperty);
        });
    }

    private void adjustTableWidth(ScrollBar bar, DoubleProperty width)
    {
        if (bar == null) return;

        width.set(bar.visibleProperty().get() ? bar.getWidth() : 0);
        bar.visibleProperty().addListener((obs, oldVal, newVal) -> {
                width.set(newVal ? bar.getWidth() : 0);
            });
    }

    // ---------------------------------------------------------------------------------------- 
    //                                      FXML GUI handler
    // ---------------------------------------------------------------------------------------- 
    @FXML
    protected void handleAction(ActionEvent ev)
    {
        if (learningHotkey) return;

        if (ev.getSource() == btnClose) close();
        if (ev.getSource() == btnLearnHotkey) learnHotkey();
    }

    @FXML
    protected void handleKeys(KeyEvent ev)
    {
        if (ev.getEventType() == KeyEvent.KEY_PRESSED) {
            if (ev.getCode() == KeyCode.ENTER) {
                if (ev.getSource() == btnClose) close();
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

    /**
     * Handles clicks on the learn toggle button. Toggling it on starts the
     * recording of a new hotkey; toggling it off again aborts and restores the
     * currently stored combination.
     */
    private void learnHotkey()
    {
        if (btnLearnHotkey.isSelected()) {
            learningHotkey = true;
            cfgHotkey.setText("Recording new hotkey…");
            showMessage("Press a modifier plus a key (ESC to cancel).");
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

        cfgHotkey.setText(GlobalHotkey.format(Registry.get().getHotkey()));
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
            showError("A hotkey needs at least one modifier");
            return;   // keep listening
        }

        // Full combination captured: leave learning mode, then apply it (updates
        // the config, re-targets the live hook and persists it to the database).
        learningHotkey = false;
        btnLearnHotkey.setSelected(false);
        learner.stop();
        learner = null;

        try {
            Registry.get().updateHotkey(packedCombo);
            cfgHotkey.setText(GlobalHotkey.format(Registry.get().getHotkey()));
            showSuccess("New hotkey: " + GlobalHotkey.format(Registry.get().getHotkey()));

        } catch (SQLException e) {
            // The combination is already active; only persisting it failed.
            cfgHotkey.setText(GlobalHotkey.format(Registry.get().getHotkey()));
            showError("Hotkey is active but could not be saved: " + e.getLocalizedMessage());
        }
    }

    @FXML
    protected void handleMenus(ActionEvent event) throws IOException
    {
        if (learningHotkey) return;

        // File Menu
        if (event.getSource() == miExit) {
            Platform.exit();

        // Help Menu
        } else if (event.getSource() == miAbout) {
            AboutController ctrl = new AboutController();
            ctrl.show();
        }
    }   

    private Callback<TableColumn<Session, LocalDateTime>, TableCell<Session, LocalDateTime>> formatDateTime = (tableColumn) -> {
        TableCell<Session, LocalDateTime> tableCell = new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);

                if (empty || item == null) return;

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy  HH:mm");
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
