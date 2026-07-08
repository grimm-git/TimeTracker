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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

import TimeTracker.Defaults;
import TimeTracker.Registry;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Session
{
    private int           SessionID;
    private LocalDateTime SessionStart;
    private LocalDateTime SessionEnd;

    private final StringProperty SessionDay = new SimpleStringProperty();
    private final StringProperty SessionDate = new SimpleStringProperty();
    private final StringProperty SessionTime = new SimpleStringProperty();

    public Session()
    {
        SessionID    = 0;
        SessionStart = LocalDateTime.now();
        SessionEnd   = LocalDateTime.now();

        setProperties();
    }

    /**
     * Reconstructs a Session from stored values, e.g. when read back from the
     * database. A not yet finished session is expected to use LocalDateTime.MIN
     * as its end time.
     *
     * @param start the start time of the session
     * @param end   the end time of the session, or LocalDateTime.MIN if the
     *              session has not been finished yet
     */
    public Session(int id, LocalDateTime start, LocalDateTime end)
    {
        SessionID    = id;
        SessionStart = start;
        SessionEnd   = end;

        setProperties();
    }

    public int getSessionID()
    {
        return SessionID;
    }

    public void setSessionID(int id)
    {
        SessionID = id;
    }

    public LocalDateTime getSessionStart()
    {
        return SessionStart;
    }

    public LocalDateTime getSessionEnd()
    {
        return SessionEnd;
    }

    public String getDayName()
    {
        return SessionDay.get();
    }

    /**
     * Returns the elapsed time between session start and session end as a
     * Duration object.
     *
     * @return the session duration
     */
    public Duration getDuration()
    {
        return Duration.between(SessionStart, SessionEnd);
    }

    /**
     * Returns the net working time of the session, i.e. the raw session
     * duration reduced by the configured break. Sessions shorter than
     * {@link Defaults#DEFAULT_FIRST_BREAK} minutes are returned unchanged; once
     * the session reaches that length the configured break time (in minutes) is
     * subtracted.
     *
     * @return the session duration minus the break once it is due
     */
    public Duration getWorkTime()
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        Duration duration = getDuration();

        if (duration.toMinutes() < Defaults.DEFAULT_FIRST_BREAK)
            return duration;

        return duration.minusMinutes(Config.getBreakTime());
    }

    public boolean isBankHoliday()
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        int day = SessionStart.getDayOfWeek().getValue();
        if (day == 6 && !Config.getWDSaturday()) return true;  // Saturday is workday?
        if (day == 7 && !Config.getWDSunday())   return true;  // Sunday is workday ?

        return false;
    }

    /**
     * Returns the elapsed time between the session start and the current time.
     * Unlike {@link #getWorkTime()} this ignores the stored end time and is
     * meant for an open, still running session whose live duration should be
     * displayed.
     *
     * @return the time elapsed since the session started
     */
    public Duration getRunningTime()
    {
        return Duration.between(SessionStart, LocalDateTime.now());
    }

    private void setProperties()
    {
        // Sets the day of week of the session start date as a short name,
        //  e.g. "Mon", "Tue", "Wed".
        SessionDay.set(SessionStart.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.getDefault()));

        DateTimeFormatter fmt1 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        SessionDate.set(SessionStart.format(fmt1));

        DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("HH:mm");
        SessionTime.set(SessionStart.format(fmt2));
    }

    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty SessionDayProperty()    { return SessionDay; }
    public StringProperty SessionDateProperty()   { return SessionDate; }
    public StringProperty SessionTimeProperty()   { return SessionTime; }
}
