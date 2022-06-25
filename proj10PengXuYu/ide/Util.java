/*
 * File: Controller.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj10PengXuYu.ide;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;

/**
 * Utility class that contains handy static methods that is available
 * to use in any other class.
 */
public class Util {

    /**
     * Returns a true BooleanBinding if there are no more tabs and a false one if there
     * is at least one tab.
     *
     * @return a BooleanBinding demonstrating if there are no more tabs
     */
    public static BooleanBinding noTabs(TabPane tabPane) {
        return Bindings.isEmpty(tabPane.getTabs());
    }

    /**
     * Gets the currently selected tab in tabPane
     * @param tabPane TabPane object to perform file handling.
     *
     * @return the selected tab
     */
    public static Tab getSelectedTab(TabPane tabPane) {
        return tabPane.getSelectionModel().getSelectedItem();
    }

    /**
     * helper function to get the text box in the selected tab.
     * @param tabPane TabPane object to perform file handling.
     *
     * @return TextArea  the text box in the selected tab
     */
    public static CodeArea getSelectedTextBox(TabPane tabPane) {
        Tab currentTab = getSelectedTab(tabPane);
        VirtualizedScrollPane scrollPane;
        scrollPane = (VirtualizedScrollPane) currentTab.getContent();
        return (CodeArea) scrollPane.getContent();
    }

    /**
     * Handler method for display a given String on console.
     *
     * @param toDisplay the string to be displayed on console
     */
    public static void putOnConsole(String toDisplay,
                                    StyleClassedTextArea console) throws IOException {
        Platform.runLater(() -> {
            console.appendText(toDisplay);
        });
    }

}
