/*
 * Copyright (C) 2018 Matthias Grimm <matthiasgrimm@users.sourceforge.net>
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
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import TimeTracker.Defaults;

/**
 * FXML Controller class
 *
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
public class SplashController
extends WindowFX
{
    public static final int       SPLASH_TIMEOUT = 3;

    @FXML  private Label lbProgramVersion;

    double yOffset; double xOffset;
    
    @SuppressWarnings("LeakingThisInConstructor")
    public SplashController() throws IOException
    {
        super("Splashscreen.fxml", "timetracker.css");
        
        stage.setTitle("TimeTracker");
        stage.getIcons().add(getImageResource("timetracker_16x16.png"));
        stage.getScene().setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
 
        stage.getScene().setOnMousePressed(event -> {
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });
        stage.getScene().setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
        
        lbProgramVersion.setText(String.format("TimeTracker V%s\n", Defaults.getVersionString()));

        PauseTransition delay = new PauseTransition(Duration.seconds(SPLASH_TIMEOUT));
        delay.setOnFinished(e -> stage.close());
        delay.play();
    }
    
    /**
     *
     */
    public void showAndWait()
    {
        stage.showAndWait();   // show login dialog and wait for completion
        close();
    }
}
