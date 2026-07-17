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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private boolean       hadBreak;

    private final StringProperty Day = new SimpleStringProperty();
    private final StringProperty Date = new SimpleStringProperty();
    private final StringProperty Start = new SimpleStringProperty();
    private final StringProperty End = new SimpleStringProperty();

    public Session()
    {
        SessionID    = 0;
        SessionStart = LocalDateTime.now();
        SessionEnd   = LocalDateTime.now();
        hadBreak     = false;

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

    public int getID()
    {
        return SessionID;
    }

    public void setID(int id)
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

    public boolean hadBreak()
    {
        return hadBreak;
    }

    public void setBreak(boolean arg)
    {
        hadBreak = arg;
    }

    public void setEndToNow()
    {
        SessionEnd = LocalDateTime.now();
        setProperties();
    }

    public String getDayName()
    {
        return Day.get();
    }

    public LocalDate getStartDate()
    {
        return SessionStart.toLocalDate();
    }

    public LocalTime getStartTime()
    {
        return SessionStart.toLocalTime();
    }

    public LocalTime getEndTime()
    {
        return SessionEnd.toLocalTime();
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

        if (hadBreak())
            duration = duration.minusMinutes(Config.getBreakLength());

        return duration;
    }

    /**
     * Returns the elapsed time between the session start and the current time.
     * Unlike {@link #getWorkTime()} this ignores breaks.
     *
     * @return the time elapsed since the session started without breaks
     */
    public Duration getRunningTime()
    {
        return Duration.between(SessionStart, SessionEnd);
    }

    public boolean isBankHoliday()
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        int day = SessionStart.getDayOfWeek().getValue();
        if (day == 6 && !Config.getWDSaturday()) return true;  // Saturday is workday?
        if (day == 7 && !Config.getWDSunday())   return true;  // Sunday is workday ?
        if (Reg.isPublicHoliday(SessionStart.toLocalDate())) return true;

        return false;
    }

    private void setProperties()
    {
        // Sets the day of week of the session start date as a short name,
        //  e.g. "Mon", "Tue", "Wed".
        Day.set(SessionStart.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.getDefault()));

        DateTimeFormatter fmt1 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        Date.set(SessionStart.format(fmt1));

        DateTimeFormatter fmt2 = DateTimeFormatter.ofPattern("HH:mm");
        Start.set(SessionStart.format(fmt2));
        End.set(SessionEnd.format(fmt2));
    }

    // -------------------------------------------------------------------------------- 
    //                                   Property Objects
    // -------------------------------------------------------------------------------- 
    public StringProperty DayProperty()    { return Day; }
    public StringProperty DateProperty()   { return Date; }
    public StringProperty StartProperty()  { return Start; }
    public StringProperty EndProperty()    { return End; }
}
