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

import TimeTracker.Registry;
import TimeTracker.data.Database;
import TimeTracker.data.Session;
import static TimeTracker.gui.Notification.getDecission;
import static TimeTracker.gui.Notification.showError;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author grimm
 */
public class MainWindowData
{
    private final StringProperty errorMsg = new SimpleStringProperty();
    private final StringProperty successMsg = new SimpleStringProperty();
    private final ObjectProperty<Session> curSession = new SimpleObjectProperty<>();
    
    private final ObservableList<Session> sessionListWeek =  FXCollections.observableArrayList();


    public MainWindowData()
    {
        Registry Reg = Registry.get();
        Database DBHandle = Reg.getDatabase();
        int ok;

        try {
            Session current = DBHandle.getLastSession();
            if (current == null) {
                current = new Session();

            } else if (!current.isSessonFinished()) {
                ok = getDecission("Open Session", "What shall we do with the open session?", "Continue", "Start new");
                if (ok == 1) {
                    current.finishSession();
                    DBHandle.writeSession(current);
                    current = new Session();
                }
            }
            curSession.set(current);
            
        } catch (SQLException e) {
            showError("Can't find Session.\n" + e.getLocalizedMessage());
        }        

    }

    public Session getCurrentSession()
    {
        return curSession.get();
    }

    public ObservableList<Session> getSessionListWeek() { return sessionListWeek; }


    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty errorMsgProperty()    { return errorMsg; }
    public StringProperty successMsgProperty()  { return successMsg; }
    public ObjectProperty<Session> curSessionProperty()  { return curSession; }

}
