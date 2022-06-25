/*
 * File: Controller.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 10
 * Date: May 6, 2022
 */

package proj10PengXuYu.ide;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;

/**
 * Controller class contains handler methods for buttons and menu items.
 * These handlers methods are set on action by the Main.fxml file for each
 * corresponding node in the GUI.
 */
public class Controller {

    // Helper class that contains all handler implementations for File menu items
    private FileMenuController fileMenuController;
    // TerminalHelper handling all terminal features
    private TerminalHelper terminalHelper;
    // helper class that contains implementations for FindReplace MenuItem
    private EditMenuController editMenuController;
    // Helper class contains implementation for button handlers
    private ToolBarHelper toolBarHelper;

    // the GUI nodes given by the FXML file
    @FXML private Button checkButton, compileRunButton, stopButton;
    @FXML private TabPane tabPane;
    @FXML private MenuItem undoMI, redoMI;
    @FXML private MenuItem selectAllMI, cutMI, copyMI, pasteMI;
    @FXML private MenuItem findReplaceMI, matchingMI, toggleCommentMI, prettyPrintMI;
    @FXML private MenuItem saveMI, saveAsMI, closeMI;
    @FXML private AnchorPane anchorPane;
    @FXML private StyleClassedTextArea ioConsole;


    /**
     * Exposes the exit handler's functionality to outside classes.
     */
    public void handleWindowExit(){
        handleExit(new ActionEvent());
    }


    /**
     * Creates the initial tab, binds disable properties, and sets up the terminal.
     * This method is called when the application launches.
     */
    @FXML
    private void initialize() {
        // construct a DialogHelper and pass it to the constructors of 4 helper classes
        DialogHelper dialogHelper = new DialogHelper();
        // initialize the helper classes/controllers
        this.fileMenuController = new FileMenuController(tabPane, dialogHelper);
        this.terminalHelper = new TerminalHelper(dialogHelper, ioConsole);
        this.editMenuController = new EditMenuController(dialogHelper, tabPane, anchorPane);
        this.toolBarHelper = new ToolBarHelper(fileMenuController,
                terminalHelper, dialogHelper, tabPane, ioConsole);

        // creates the initial tab on stage
        this.handleNew();
        // displays an initial prompt on the console
        terminalHelper.displayTerminalPrompt();

        // disable appropriate menu items when no tabs are open
        MenuItem[] menuItems = {closeMI, saveMI, saveAsMI, undoMI, redoMI,
                                selectAllMI, cutMI, copyMI, pasteMI, findReplaceMI,
                                matchingMI, toggleCommentMI, prettyPrintMI};
        for (MenuItem mi : menuItems) {
            mi.disableProperty().bind(Util.noTabs(tabPane));
        }

        // Bind compile buttons so that they are disabled when a process is running
        Button[] buttons = {checkButton, compileRunButton, stopButton};
        for (Button button: buttons) {
            button.disableProperty().bind(
                    Bindings.or(toolBarHelper.getThreadActive(), Util.noTabs(tabPane)));
        }

        // sets the default handler for the console
        terminalHelper.setTerminalHandler();
    }


    /**
     * Handles menu item About. Shows a dialog that contains program information.
     */
    @FXML
    private void handleAbout() {
        fileMenuController.handleAbout();
    }


    /**
     * Handles menu item New. Creates a new tab and adds it to the tabPane.
     */
    @FXML
    private void handleNew() {
        fileMenuController.handleNew();
    }


    /**
     * Handles menu item Open. Shows a dialog and lets the user select a file to be
     * loaded into the text box.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleOpen(ActionEvent event) {
        fileMenuController.handleOpen(event);
    }


    /**
     * Handles menu item Close. Creates a dialog if the selected tab is unsaved and
     * closes the tab.
     */
    @FXML
    public void handleClose() {
        fileMenuController.closeSelectedTab(SaveReason.CLOSING);
    }


    /**
     * Handler method for menu item Exit. When exit item of the menu
     * bar is clicked, the application quits if all tabs in the tabPane are
     * closed properly.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleExit(ActionEvent event) {
        fileMenuController.handleExit(event);
    }


    /**
     * Handler method for menu item Save. Behaves like Save as... if the text
     * has never been saved before. Otherwise, save the text to its corresponding
     * text file.
     */
    @FXML
    private void handleSave() {
        fileMenuController.handleSave();
    }

    /**
     * Handles menu item Save as....  a dialog appears in which the user is asked for
     * to save a file with two permitted extensions: .btm and .txt.
     */
    @FXML
    private void handleSaveAs() {
        fileMenuController.handleSaveAs();
    }

    /**
     * Handler method for menu item Undo.
     */
    @FXML
    private void handleUndo() {
        Util.getSelectedTextBox(tabPane).undo();
    }

    /**
     * Handler method for menu item Redo.
     */
    @FXML
    private void handleRedo() {
        Util.getSelectedTextBox(tabPane).redo();
    }

    /**
     * Handler method for menu item Cut.
     */
    @FXML
    private void handleCut() {
        Util.getSelectedTextBox(tabPane).cut();
    }

    /**
     * Handler method for menu item Copy.
     */
    @FXML
    private void handleCopy() {
        Util.getSelectedTextBox(tabPane).copy();
    }

    /**
     * Handler method for menu item Paste.
     */
    @FXML
    private void handlePaste() {
        Util.getSelectedTextBox(tabPane).paste();
    }

    /**
     * Handler method for menu item Select all.
     */
    @FXML
    private void handleSelectAll() {
        Util.getSelectedTextBox(tabPane).selectAll();
    }


    /**
     * Handler method for menu item Matching brackets.
     * 
     * Select all text from the user-selected bracket to its matching bracket, it
     * can select closing bracket if user selects the opening bracket, or the other
     * way round. If something other than valid bracket is selected, alert users. If
     * there is no matching bracket, alert users.
     *
     * @author Ricky Peng
     */
    @FXML
    private void handleMatching() {
        editMenuController.handleMatching();

    }

    /**
     * Handler method for menu item Toggle comments.
     */
     @FXML
    private void handleToggleComment() {
        editMenuController.handleToggleComment();
    }


    /**
     * Handler method for menu item FindReplace.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleFindReplace(ActionEvent event){
        editMenuController.handleFindReplace(event);
    }


    /**
     * Handler method for menu item Pretty print.
     */
    @FXML void handlePrettyPrint() {
        editMenuController.handlePrettyPrint();
    }


    /**
     * Handler method for Check button.
     */
    @FXML
    private void handleCheck() {
        toolBarHelper.handleCheck();
    }

    /**
     * Handler method for Compile & Run button.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    @FXML
    private void handleCompileRun(ActionEvent event) {
        toolBarHelper.handleCompileRun(event);
    }

    /**
     * Handles the stop button.
     * @deprecated
     */
    @FXML
    private void handleStop() throws IOException {
        toolBarHelper.handleStop();
    }

}
