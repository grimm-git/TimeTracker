/*
 * Copyright (C) 2026 Matthias Grimm <matthiasgrimm@users.sourceforge.net>
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Master data container class of the application
 * 
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
public class Registry
{
    private static volatile Registry instance = null;   // Singleton object instance 

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
    }

    public ExecutorService getExecutor() { return threadExecutor; }
}
