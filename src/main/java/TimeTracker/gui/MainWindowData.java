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
import java.util.ArrayList;

import TimeTracker.Registry;
import TimeTracker.data.Database;
import TimeTracker.data.Session;
import static TimeTracker.gui.Notification.getDecission;
import static TimeTracker.gui.Notification.showError;
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
    
    private final ObservableList<Session> sessionListWeek =  FXCollections.observableArrayList();


    public MainWindowData()
    {
        Registry Reg = Registry.get();
        Database dbHandle = Reg.getDbHandle();
        int ok;

        try {
            Session active = Reg.getActiveSession();
            if (active == null || active.isSessonFinished()) {
                active = new Session();
                dbHandle.writeSession(active);  // write unfinished session

            } else {  // session is still open
                ok = getDecission("Open Session", "What shall we do with the session?", "Continue", "Start new one");
                if (ok == 1) {
                    active.finishSession();
                    dbHandle.writeSession(active);  // close open session
                    active = new Session();
                    dbHandle.writeSession(active);  // write unfinished session
                }
            }
            Reg.setActiveSession(active);
    
            ArrayList<Session> list = dbHandle.getSessionLog();
            sessionListWeek.setAll(list);
    
        } catch (SQLException e) {
            showError("Can't find Session.\n" + e.getLocalizedMessage());
        }        

    }

    public Session getCurrentSession()
    {
        Registry Reg = Registry.get();
        return Reg.getActiveSession();
    }

    public ObservableList<Session> getSessionListWeek()
    {
        return sessionListWeek;
    }


    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty errorMsgProperty()    { return errorMsg; }
    public StringProperty successMsgProperty()  { return successMsg; }
    
}
