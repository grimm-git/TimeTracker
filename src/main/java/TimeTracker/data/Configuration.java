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
package TimeTracker.data;

import java.nio.file.Path;
import java.nio.file.Paths;

import TimeTracker.Defaults;
import TimeTracker.util.GlobalHotkey;

public class Configuration
{   
    private boolean mDirty;
    private Path mDBPath;
    private int mBreakTime;
    private int mHotkey;
    private boolean mHideAtStart;

    public Configuration()
    {
        mDirty       = false;
        mDBPath      = Paths.get(System.getProperty("user.dir"), Defaults.DB_FILE_NAME);
        mBreakTime   = Defaults.DEFAULT_BREAK_TIME;
        mHotkey      = GlobalHotkey.DEFAULT_HOTKEY;
        mHideAtStart = Defaults.DEFAULT_HIDE_AT_START;
    }
    
    public boolean isDirty()
    {
        return mDirty;
    }

    /** @return the path to the TimeTracker database file */
    public Path getDBPath()
    {
        return mDBPath;
    }

    public void setDBPath(Path arg)
    {
        mDBPath = arg;
    }


    /** @return the break duration in minutes */
    public int getBreakTime()
    {
        return mBreakTime;
    }

    public void setBreakTime(int arg)
    {
        mBreakTime = arg;
        mDirty = true;
    }

    /** @return the global hotkey combination in packed form */
    public int getHotkey()
    {
        return mHotkey;
    }

    public void setHotkey(int arg)
    {
        mHotkey = arg;
        mDirty = true;
    }

    /** @return the "hide at start" flag (0 = disabled, 1 = enabled) */
    public boolean getHideAtStart()
    {
        return mHideAtStart;
    }

    public void setHideAtStart(boolean arg)
    {
        mHideAtStart = arg;
        mDirty = true;
    }
}
