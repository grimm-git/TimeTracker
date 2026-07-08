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

package TimeTracker;

import java.sql.SQLException;

import TimeTracker.data.Configuration;
import TimeTracker.data.Database;
import TimeTracker.data.Session;
import TimeTracker.gui.Notification;
import TimeTracker.util.GlobalHotkey;

/**
 * Master data container class of the application
 * 
 * @author Matthias Grimm
 */
public class Registry
{
    private static volatile Registry instance = null;   // Singleton object instance 

    private Database DBase;
    private GlobalHotkey hotkey;                 // live, registered global hook
    private Session activeSession;
    private Configuration Config;

    /**
     * This method is the global access to the Registry object, which is a Singleton
     * 
     * @return  Singleton instance of Registry object
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static Registry get()
    {
        if (instance == null)
        {
            synchronized(Registry.class)
            {
                if (instance == null)
                    instance = new Registry();
            }
        }
        return instance;
    }

    /**
     * Singletons require a private constructor, so no further objects of this class could
     * be instanciated.
     */
    private Registry()
    {
        activeSession = new Session();
        Config        = new Configuration();  // create configuration object with default values
        DBase         = null;
    }

    /**
     * Cleanup stuff before close the program
     */
    public void close()
    {
        if (hotkey != null)
            hotkey.unregister();

        try {
            DBase.updateDatabase(); // save Session and Configuration

        } catch (SQLException e) { /* IGNORE */ }
    }

    /**
     * 
     * @return   Configuration object, containing the current program configuration
     */
    public Configuration getConfig()
    {
        return Config;
    }

    /**
     * 
     * @return   Database object, link to SQLlite database
     */
    public Database getDBase()
    {
        return DBase;
    }

    /**
     * Opens the SQLite database at the path stored in the Registry and, on
     * success, stores the database handle in the Registry. If the database can
     * not be opened an error dialog with a single close button is shown; after
     * it is dismissed the application terminates gracefully.
     *
     * @return TRUE if the database was opened, FALSE if startup should abort
     */
    public boolean initDatabase()
    {
        try {
            DBase = new Database(Config.getDBPath());
            return true;

        } catch (SQLException e) {
            Notification.showError("Global hotkey could not be registered.\n" + e.getLocalizedMessage());
            return false;
        }
    }
    
    public Session getSession()
    {
        return activeSession;
    }

    public void setSession(Session active)
    {
        activeSession = active;
    }
}
