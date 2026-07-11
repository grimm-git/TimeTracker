/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */package TimeTracker.gui;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

public class SystemTrayIcon
{
    private FXTrayIcon trayIcon;

    public SystemTrayIcon(Stage stage, String icon)
    {
		trayIcon = new FXTrayIcon(stage, getClass().getResource("/images/"+icon));
		trayIcon.show();
		trayIcon.setTrayIconTooltip("TimeTracker - track your working time");
    }

    public void addMenuItem(Menu item)
    {
        trayIcon.addMenuItem(item);
    }

    public void addMenuItem(MenuItem item)
    {
        trayIcon.addMenuItem(item);
    }

    public void remove()
    {
		// Removing the FXTrayIcon, this will also cause the JVM to terminate
		// after the last JavaFX Stage is hidden
		trayIcon.hide();
    }
}
