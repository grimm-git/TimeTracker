/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
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
import java.time.LocalTime;

import com.github.kwhat.jnativehook.NativeHookException;

import TimeTracker.Defaults;
import TimeTracker.util.GlobalHotkey;

public class Configuration
{   
    private boolean mDirty;
    private Path mDBPath;
    private LocalTime mBreakTime;
    private int mBreakLength;
    private int mHotkey;
    private boolean mHideAtStart;
    private boolean mWDSaturday;
    private boolean mWDSunday;
    private boolean mHasBreak;

    private GlobalHotkey hotkey;                 // live, registered global hook

    public Configuration()
    {
        mDirty       = false;
        mDBPath      = Paths.get(System.getProperty("user.dir"), Defaults.DB_FILE_NAME);
        mBreakTime   = LocalTime.of(Defaults.DEFAULT_BREAK_TIME_H, Defaults.DEFAULT_BREAK_TIME_M);
        mBreakLength = Defaults.DEFAULT_BREAK_LENGTH;
        mHotkey      = GlobalHotkey.DEFAULT_HOTKEY;
        mHideAtStart = Defaults.DEFAULT_HIDE_AT_START;
        mHasBreak    = false;
        mWDSaturday  = false;
        mWDSunday    = false;
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


    /** @return the time where after the break should be inserted */
    public LocalTime getBreakTime()
    {
        return mBreakTime;
    }

    public void setBreakTime(LocalTime time)
    {
        mBreakTime = time;
        mDirty = true;
    }

    /** @return the break duration in minutes */
    public int getBreakLength()
    {
        return mBreakLength;
    }

    public void setBreakLength(int arg)
    {
        mBreakLength = arg;
        mDirty = true;
    }

    public void setHasBreak(boolean arg)
    {
        mHasBreak = arg;
        mDirty = true;
    }

    public boolean hasBreak()
    {
        return mHasBreak;
    }

    /** @return the global hotkey combination in packed form */
    public int getHotkey()
    {
        return mHotkey;
    }

    public void setHotkey(GlobalHotkey objHotkey) throws NativeHookException 
    {
        this.hotkey = objHotkey;
        hotkey.setHotkey(mHotkey);
        hotkey.register();
    }

    public void setHotkey(int arg)
    {
        mHotkey = arg;
        mDirty = true;

        if (hotkey != null)
            hotkey.setHotkey(mHotkey);
    }

    public void clrHotkey()
    {
        hotkey.unregister();
        hotkey = null;
    }

    /** @return the "hide at start" flag (false = disabled, true = enabled) */
    public boolean getHideAtStart()
    {
        return mHideAtStart;
    }

    public void setHideAtStart(boolean arg)
    {
        mHideAtStart = arg;
        mDirty = true;
    }

    /** @return the "saturday is workday" flag (false = disabled, true = enabled) */
    public boolean getWDSaturday()
    {
        return mWDSaturday;
    }

    public void setWDSaturday(boolean arg)
    {
        mWDSaturday = arg;
        mDirty = true;
    }

    /** @return the "sunday is workday" flag (false = disabled, true = enabled) */
    public boolean getWDSunday()
    {
        return mWDSunday;
    }

    public void setWDSunday(boolean arg)
    {
        mWDSunday = arg;
        mDirty = true;
    }
}
