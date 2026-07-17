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
 */
package TimeTracker.gui;

import java.io.IOException;

import TimeTracker.Defaults;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * FXML Controller class
 * Full support of I18N
 *
 * @author Matthias Grimm
 */
public class AboutController
extends WindowFX
{
    @FXML  private TextFlow textArea;
    @FXML  private Button btnClose;
    
    /**
     * Programmer and License notice for ProjectMaster V2.0
     * 
     * @throws IOException
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public AboutController() throws IOException
    {
        super("About.fxml", "timetracker.css");
        stage.setTitle(i18n("about.title"));
        stage.setResizable(false);

        Text text;
        text = new Text(String.format("Version: %s\n", Defaults.getVersionString()));
        text.setStyle("-fx-font-weight: bold; -fx-font-size:1.2em");
        textArea.getChildren().add(text);
        
        text = new Text(i18n("about.author","Matthias Grimm, \u00A92026\n"));
        textArea.getChildren().add(text);

        text = new Text("\n");
        text.setStyle("-fx-font-size: 0.4em");
        textArea.getChildren().add(text);

        text = new Text(i18n("about.brief") + "\n");
        textArea.getChildren().add(text);
        
        text = new Text("\n");
        text.setStyle("-fx-font-size: 0.4em");
        textArea.getChildren().add(text);

        text = new Text(i18n("about.detailed") + "\n");
        textArea.getChildren().add(text);

        text = new Text("\n");
        text.setStyle("-fx-font-size: 0.4em");
        textArea.getChildren().add(text);

        text = new Text(i18n("about.license")); 
        textArea.getChildren().add(text);
    }

    @FXML
    protected void handleAction(ActionEvent ev)
    {
        if (ev.getSource() == btnClose) close();
    }
}




