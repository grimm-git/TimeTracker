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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import TimeTracker.data.Database;

/**
 * Master data container class of the application
 * 
 * @author Matthias Grimm
 */
public class Registry
{
    private static volatile Registry instance = null;   // Singleton object instance 

    private Path dbPath;
    private Database database;
    private ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
    
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
        dbPath = Paths.get(System.getProperty("user.dir"), Defaults.DB_FILE_NAME);
    }

    /**
     * Cleanup stuff before close the program
     */
    public void close()
    {
        threadExecutor.shutdown();
        try {
            if (!threadExecutor.awaitTermination(200, TimeUnit.MILLISECONDS))
                threadExecutor.shutdownNow();

        } catch (InterruptedException e) {
            threadExecutor.shutdownNow();
        }

        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                System.err.println("Database could not be closed: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the location of the SQLite database file. This is either the path
     * given on the command line via the "db" option or the default location.
     *
     * @return the database file path
     */
    public Path getDatabasePath()
    {
        return dbPath;
    }

    /**
     * Sets the location of the SQLite database file, e.g. from the "db" command
     * line option.
     *
     * @param dbPath the database file path
     */
    public void setDatabasePath(Path dbPath)
    {
        this.dbPath = dbPath;
    }

    /**
     * Returns the open database handle, or null if the database has not been
     * opened yet.
     *
     * @return the database handle
     */
    public Database getDatabase()
    {
        return database;
    }

    /**
     * Stores the open database handle so it can be accessed application wide.
     *
     * @param database the open database handle
     */
    public void setDatabase(Database database)
    {
        this.database = database;
    }

    public ExecutorService getExecutor() { return threadExecutor; }
}
