/*
 * Copyright (C) 2026 Matthias Grimm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package TimeTracker.gui;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;

import TimeTracker.Registry;
import TimeTracker.data.Database;
import TimeTracker.data.Session;
import static TimeTracker.gui.Notification.getDecission;
import static TimeTracker.gui.Notification.showError;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Matthias Grimm
 */
public class MainWindowData
{
    private final StringProperty errorMsg = new SimpleStringProperty();
    private final StringProperty successMsg = new SimpleStringProperty();
    
    private final StringProperty elapsedTime = new SimpleStringProperty();
    private final ObservableList<Session> sessionListWeek =  FXCollections.observableArrayList();

    /** Ticks once per minute to save data to the database. */
    private Timeline systemClock;


    public MainWindowData()
    {
        Registry Reg = Registry.get();
        Database DBase = Reg.getDBase();
        int ok;

        try {
            Session session = Reg.getSession();
            LocalDate today = LocalDate.now();

            if (session == null || today.isAfter(session.getSessionStart().toLocalDate())) {
                Reg.setSession(new Session());   // new day, new session

            } else {    // still the same day, so...
                ok = getDecission("Open Session", "What shall we do with the session?", "Continue", "Start new one");
                if (ok == 1) Reg.setSession(new Session());   // start new one
            }

            ArrayList<Session> list = DBase.getSessionLog();
            sessionListWeek.setAll(list);
    
        } catch (SQLException e) {
            showError("Can't find Session.\n" + e.getLocalizedMessage());
        }        

        elapsedTime.set("00:00");

        systemClock = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(1), ev -> handleClockEvents()));
        systemClock.setCycleCount(Animation.INDEFINITE);
        systemClock.play();
    }

    public ObservableList<Session> getSessionListWeek()
    {
        return sessionListWeek;
    }

  /**
     * Recomputes the elapsed time of the currently active session and writes it
     * into {@link #textSessionDur} as HH:MM. Always reads the current session
     * from the data model so it follows a session switch automatically. Runs on
     * the JavaFX application thread (invoked by the {@link #sessionClock}).
     */
    private void handleClockEvents()
    {
        Registry Reg = Registry.get();

        Duration elapsed = Reg.getSession().getRunningTime();
        elapsedTime.set(String.format("%02d:%02d",
                    elapsed.toHours(), elapsed.toMinutesPart()));

        try {
            Reg.getDBase().updateDatabase();

        } catch (SQLException e) {
            showError("Data could not be saved!");
        }
    }

    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty errorMsgProperty()    { return errorMsg; }
    public StringProperty successMsgProperty()  { return successMsg; }
    public StringProperty elapsedTimeProperty() { return elapsedTime; }
    
}
