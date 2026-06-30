/*
 * Copyright (C) 2018 Matthias Grimm 
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
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 *
 * @author Matthias Grimm
 */
public abstract class WindowFX
{
    protected Stage stage;
    protected Label msgLabel;
    protected double posX;
    protected double posY;
     
    @SuppressWarnings("LeakingThisInConstructor")
    protected WindowFX(String fxmlFile, String cssFile) throws IOException
    {
        this(new Stage(), fxmlFile, cssFile);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    protected WindowFX(Stage stage, String fxmlFile, String cssFile) throws IOException
    {
        this.stage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getFXMLResource(fxmlFile));
        fxmlLoader.setController(this);
        Parent root = fxmlLoader.load();
      
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getCSSResource(cssFile));
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent ev) -> {
            clearMessage();
        });
        
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setOnCloseRequest(ev -> this.close());
    }
    
    /**
     * Show the dialog and return.
     */
    public void show()
    {
        if (stage.isShowing())
            return;
        
        if (posX > 0) stage.setX(posX);
        if (posY > 0) stage.setY(posY);
        stage.show();
        stage.toFront();        // raise above other windows when re-shown
        stage.requestFocus();
    }

    /**
     * Show the dialog and wait until it is closed again. The input to other windows is
     * blocked while this dialog is open (WINDOW.MODAL).
     * 
     * @param owner  top level window this dialog belongs to
     */
    public void showAndWait(Window owner)
    {
        if (posX > 0) stage.setX(posX);
        if (posY > 0) stage.setY(posY);

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.showAndWait();   // show dialog and wait for completion
        close();
    }

    /**
     * Close the dialog
     */
    protected void close()
    {
        posX = stage.getX();
        posY = stage.getY();
        stage.close();
    }

    // ----------------------------------------------------------------------------------- 
    //                               Dialogue Helper Methods
    // ----------------------------------------------------------------------------------- 
    protected boolean isTextfieldEmpty(TextField field, String errortxt)
    {
        if (field.getText() == null || field.getText().isEmpty()) {
            showError(errortxt);
            field.requestFocus();
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------------------- 
    //                               Display Messages 
    // ----------------------------------------------------------------------------------- 
    protected final void setMsgLabel(Label msg)
    {
        msgLabel = msg;
        msg.setText("");   // clear message
    }

    protected void showError(String msg)
    {
        if (msgLabel != null) {
            msgLabel.setText(msg);
            msgLabel.setStyle("-fx-text-fill:-fx-negative-color;-fx-font-size:0.8em;");
            syncTooltip(msg);
        }
    }

    protected void showMessage(String msg)
    {
        if (msgLabel != null) {
            msgLabel.setText(msg);
            msgLabel.setStyle("-fx-text-fill:-fx-text-base-color;-fx-font-size:1.2em;");
            syncTooltip(msg);
        }
    }

    protected void showSuccess(String msg)
    {
        if (msgLabel != null) {
            msgLabel.setText(msg);
            msgLabel.setStyle("-fx-text-fill:-fx-positive-color;-fx-font-size:1.2em;");
            syncTooltip(msg);
        }
    }

    protected void clearMessage()
    {
        showMessage("");
    }

    private void syncTooltip(String msg)
    {
        Tooltip tt = msgLabel.getTooltip();
        if (tt != null)
            tt.setText(msg);
    }
    
    // ----------------------------------------------------------------------------------- 
    //                               TableView Helper
    // ----------------------------------------------------------------------------------- 
    protected ScrollBar getVerticalScrollbar(TableView<?> table)
    {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n;
                if (bar.getOrientation().equals(Orientation.VERTICAL))
                    return bar;
            }
        }       
        return null;
    }
    protected ScrollBar getVerticalScrollbar(TreeTableView<?> table)
    {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n;
                if (bar.getOrientation().equals(Orientation.VERTICAL))
                    return bar;
            }
        }       
        return null;
    }

    // ----------------------------------------------------------------------------------- 
    //                               Ressource Helper
    // ----------------------------------------------------------------------------------- 
    protected final URL getFXMLResource(String name) {
        return getClass().getResource("/fxml/" + name);
    }
    
    protected final String getCSSResource(String name) {
        return getClass().getResource("/css/" + name).toExternalForm();
    }
    
    public final Image getImageResource(String name) {
        return new Image(getClass().getResource("/images/" + name).toExternalForm());

    }
    public final Image getImageResource(String name, int w, int h, boolean ratio, boolean smooth) {
        return new Image(getClass().getResource("/images/" + name).toExternalForm(), w, h, ratio, smooth);
    }
    
    protected void addStyleClass(Node node, String style)
    {
        if (!node.getStyleClass().contains(style))
            node.getStyleClass().add(style);
    }

    protected void removeStyleClass(Node node, String style)
    {
         node.getStyleClass().remove(style);
    }
}
