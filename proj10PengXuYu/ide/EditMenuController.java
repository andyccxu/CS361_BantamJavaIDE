/*
 * File: EditMenuController.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj10PengXuYu.ide;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import proj10PengXuYu.bantam.ast.Program;
import proj10PengXuYu.bantam.parser.Parser;
import proj10PengXuYu.bantam.util.CompilationException;
import proj10PengXuYu.bantam.util.Error;
import proj10PengXuYu.bantam.util.ErrorHandler;
import proj10PengXuYu.bantam.visitor.PrettyPrinterVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class that contains implementation for handlers of some
 * menu items in the Edit menu.
 *
 * Note: Not handlers of all menu items in Edit menu are implemented
 *       here. Because the handlers for cut, copy, paste, undo, redo,
 *       and select all are all implemented in one line of code, we don't
 *       think it is necessary to put it here. Instead, we have that
 *       implementation in the Controller class. For the rest of the
 *       Edit menu items that are selected by ourselves as an enhancement,
 *       we have their handlers implemented here.
 */
public class EditMenuController {

    // keep track of whether search bar is open
    private Boolean barOpen;
    // text field for find menu item
    private TextField searchField;
    // stores search results from find menu item
    private ArrayList<Integer> searchResults;
    // keep track of search index for finding next and previous results
    final private Integer[] searchIndex;
    // string property to update the search result label
    private StringProperty resultUpdate;
    // text field for replace menu item
    private TextField replaceField;
    // a new dialog helper
    private DialogHelper dialogHelper;
    // TabPane object of the IDE
    private TabPane tabPane;
    // AnchorPane of the IDE
    private AnchorPane anchorPane;

    /**
     * Constructor for EditMenuController
     *
     * @param dialogHelper  the DialogHelper object passed in by the Controller
     * @param tabPane  the TabPane object passed in by the IDE
     * @param anchorPane  the AnchorPane object passed in by the IDE
     */
    public EditMenuController(DialogHelper dialogHelper,
                              TabPane tabPane, AnchorPane anchorPane) {
        this.barOpen = false;
        this.searchField = null;
        this.searchResults = new ArrayList<>();
        this.searchIndex = new Integer[] {0};
        this.resultUpdate = new SimpleStringProperty();
        this .replaceField = null;
        this.dialogHelper = dialogHelper;
        this.tabPane = tabPane;
        this.anchorPane = anchorPane;
    }

    /**
     * Handler method for matching brackets.
     *
     * Select all text from the user-selected bracket to its matching bracket, it
     * can select closing bracket if user selects the opening bracket, or the other
     * way round. If something other than valid bracket is selected, alert users. If
     * there is no matching bracket, alert users.
     *
     * @author Ricky Peng
     */
    public void handleMatching() {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        String selectedText = codeArea.getSelectedText();
        // Get the left bound of the selected text
        int leftBound = codeArea.getSelection().getStart();
        // Verify user selects a valid bracket-like symbol
        if (isBracket(selectedText)) {
            // Find the other bound of the content at the position of the matching bracket
            int rightBound = findMatchingSymbolIndex(selectedText, leftBound);
            if (rightBound == -2) {
                // If no matching bracket could be found
                return;
            } else if (leftBound < rightBound) {
                // If user selects a opening bracket
                codeArea.selectRange(leftBound, rightBound);
            } else if (leftBound > rightBound) {
                // If user selects a closing bracket
                codeArea.selectRange(rightBound + 1, leftBound + 1);
            }
        } else {
            // If user selects an invalid symbol
            dialogHelper.getAlert("Error",
                    "Please select a valid bracket-like symbol.").show();
        }
    }

    /**
     * Handler method for menu item Find.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     *
     * @author Alex Yu
     */
    public void handleFindReplace(ActionEvent event) {
        // create a search bar if necessary
        createSearchBar(event);

        // if search bar is already open and text field is not empty,
        // search for the text
        if (searchField !=null && !(searchField.getText().isEmpty())){
            // use regular expression for search
            Matcher matcher = Pattern.compile(searchField.getText(),
                    Pattern.CASE_INSENSITIVE).matcher(
                            Util.getSelectedTextBox(tabPane).getText());
            while (matcher.find()){
                searchResults.add(matcher.start());
            }

            // if search result found, highlight the first found result
            if (searchResults.size() >= 1){
                Util.getSelectedTextBox(tabPane).selectRange(searchResults.get(0),
                        searchResults.get(0)+ searchField.getLength());
                searchIndex[0] = 0;
            }
            resultUpdate.set(searchResults.size() + " results"); // update the label
        }
    }

    /**
     * Handler for toggle comment.
     * If one or more lines are selected, toggle comment will append "//" to convert
     * the selected lines into comments. (i.e. comment out the selected lines.)
     * If one "//" is already appended in the beginning of each line selected, then
     * the toggle comment item will uncomment the selected lines by removing the "//"
     * at the beginning of each line.
     * To toggle comment for a single line, we can simply put the caret to the targeted
     * line. The line does not necessarily need to be selected.
     */
    public void handleToggleComment() {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        // If the user does not select anything
        if (codeArea.getSelectedText().equals("")) {
            toggleCommentSingleLine();
        } else {
            // If user makes a selection
            int startPosition = codeArea.getSelection().getStart();
            int endPosition = codeArea.getSelection().getEnd();
            codeArea.moveTo(startPosition);
            // Toggle the first line
            boolean isComment = toggleCommentSingleLine();
            // Compensate the 3 characters "// " added by toggleComments
            if (isComment) {
                endPosition -= 3;
            } else {
                endPosition += 3;
            }
            for (int caretPosition = startPosition;
                 caretPosition < endPosition; caretPosition++) {
                // Toggle every other line
                if (codeArea.getText(caretPosition, caretPosition+1).equals("\n")) {
                    codeArea.moveTo(caretPosition+1);
                    toggleCommentSingleLine();
                    if (isComment) {
                        endPosition -= 3;
                    } else {
                        endPosition += 3;
                    }
                }
            }
        }
    }


    /**
     * Handler for pretty print.
     * Replaces the current content in the code area with the pretty
     * printed version, if the code is well-formed.
     * Otherwise, an alert will pop out to indicate that pretty printing
     * failed because it requires well-formed code as input.
     *
     * @author Andy Xu
     */
    public void handlePrettyPrint() {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        // put the content in code area to a local copy
        String localFilePath = "./temp/prettyPrint.txt";
        File newFile = new File(localFilePath);
        try {
            Files.createDirectories(Paths.get("./temp"));
            newFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
            writer.write(codeArea.getText());
            writer.close();
        } catch (IOException e) {
            dialogHelper.getAlert("Pretty Print Failed",
                    "Unable to copy the content to a local copy.").show();
        }
        // use prettyPrinterVisitor to generate the pretty printed content
        PrettyPrinterVisitor prettyPrinterVisitor = new PrettyPrinterVisitor();
        ErrorHandler errorHandler = new ErrorHandler();
        Parser parser = new Parser(errorHandler);
        try {
            Program root = parser.parse(localFilePath);
            String result = prettyPrinterVisitor.generateOutputString(root);
            codeArea.clear();
            codeArea.append(result, "utf-8");
        } catch (CompilationException ex) {
            String errorMessages = "";
            for (Error error : errorHandler.getErrorList()) {
                errorMessages += error + "\n";
            }
            dialogHelper.getAlert("Code is not Well-formed",
                    "Pretty printing failed because code is not well-formed:\n" +
                            errorMessages).show();
        }
    }


    /**
     * Find matching bracket-like symbol's index.
     *
     * @param symbol the bracket-like symbol user selects
     * @param startIndex the left index of the user-selected symbol in the codeArea
     * @return index of the matching symbol, -2 if not found
     * @author Ricky Peng
     */
    private int findMatchingSymbolIndex(String symbol, int startIndex) {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        if (symbol.equals("(") || symbol.equals("[") || symbol.equals("{")) {
            // Use closing symbol index finder
            return findClosingSymbolIndex(symbol, startIndex + 1);
        } else {
            // Use opening symbol index finder
            return findOpeningSymbolIndex(symbol, startIndex - 1);
        }
    }

    /**
     * Find index of the closing bracket-like symbol.
     *
     * @param symbol the bracket-like symbol user selects
     * @param startIndex the left index of the user-selected symbol in the codeArea
     * @return index of the matching symbol, -2 if not found
     * @author Ricky Peng
     */
    private int findClosingSymbolIndex(String symbol, int startIndex) {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        // Initialize a hashmap for looking up corresponding symbol
        HashMap<String, String> matchingSymbols = new HashMap<String, String>() {{
            put("(", ")");
            put("[", "]");
            put("{", "}");
        }};

        // Initialize counter of found symbol to 1
        int counter = 1;
        String currentSelected;

        try {
            while (counter != 0) { // Until the matching symbol is found
                // Check the next character
                currentSelected = codeArea.getText(startIndex, startIndex + 1);
                // If found the same symbol, increment the counter
                if (currentSelected.equals(symbol))
                    counter += 1;
                // If found the matching symbol, decrement the counter
                else if (currentSelected.equals(matchingSymbols.get(symbol)))
                    counter -= 1;

                startIndex += 1;
            }
        } catch (IndexOutOfBoundsException ex) {
            // If no matching symbol was found
            dialogHelper.getAlert("Error",
                    "No matching bracket could be found.").show();
            startIndex = -2;
        }

        return startIndex;
    }

    /**
     * Find index of the opening bracket-like symbol.
     *
     * @param symbol the bracket-like symbol user selects
     * @param startIndex the left index of the user-selected symbol in the codeArea
     * @return index of the matching symbol, -2 if not found
     * @author Ricky Peng
     */
    private int findOpeningSymbolIndex(String symbol, int startIndex) {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        // Initialize a hashmap for looking up corresponding symbol
        HashMap<String, String> matchingSymbols = new HashMap<String, String>() {{
            put(")", "(");
            put("]", "[");
            put("}", "{");
        }};

        int counter = 1;
        String currentSelected;

        try {
            while (counter != 0) {
                // If we have checked everything to the start of the codeArea,
                // then no found
                if (startIndex < 0) {
                    startIndex = -2;
                    throw new IndexOutOfBoundsException();
                }
                // Check the next character
                currentSelected = codeArea.getText(startIndex, startIndex + 1);
                // If found the same symbol, increment the counter
                if (currentSelected.equals(symbol))
                    counter += 1;
                // If found the matching symbol, decrement the counter
                else if (currentSelected.equals(matchingSymbols.get(symbol)))
                    counter -= 1;

                startIndex -= 1;
            }
        } catch (IndexOutOfBoundsException ex) {
            // If no matching symbol was found
            dialogHelper.getAlert("Error",
                    "No matching bracket could be found.").show();
            startIndex = -2;
        }

        return startIndex;
    }

    /**
     * Check if a given symbol is a bracket-like symbol.
     * @param selected User selected character
     * @return true if it is a bracket-like symbol, false otherwise
     */
    private boolean isBracket(String selected) {
        return ("(".equals(selected)) || (")".equals(selected)) ||
                ("{".equals(selected)) || ("}".equals(selected)) ||
                ("[".equals(selected)) || ("]".equals(selected));
    }


    /**
     * Creates a search bar.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    private void createSearchBar(ActionEvent event){
        // first check if there is a search bar open or not
        if (barOpen == false) {
            ToolBar searchBar = new ToolBar();
            searchBar.setPrefWidth(anchorPane.getWidth());
            TextField searchField = new TextField();
            this.searchField = searchField;

            // gives find functionality to 'enter' key and resets the search results
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER){
                    searchResults.clear();
                    searchIndex[0] = 0;
                    handleFindReplace(event);
                }
            });

            // gives close functionality to 'esc' key if search bar is open
            anchorPane.setOnKeyPressed(e -> {
                if (barOpen && e.getCode() == KeyCode.ESCAPE){
                    closeSearchBar(searchBar);
                }
            });

            // empty horizontal space for placeholder
            HBox hBox = new HBox();
            HBox.setHgrow(hBox, Priority.ALWAYS);

            // create a text field for replace menu item
            TextField replaceField = new TextField();
            this.replaceField = replaceField;

            // now add all the components to the search bar and to the anchor pane
            ArrayList<Button> buttons = createSearchBarButtons(searchBar);
            // display the number of found search results
            Label result = new Label("");
            // bind the label to the search result string property
            result.textProperty().bind(resultUpdate);
            Separator separator = new Separator(Orientation.VERTICAL);
            searchBar.getItems().addAll(searchField, result, buttons.get(0),
                    buttons.get(1), separator, replaceField,
                    buttons.get(2), buttons.get(3), hBox, buttons.get(4));
            searchBar.setLayoutY(tabPane.getLayoutY()+33);

            // loop through all open tabs and move their code areas down
            // to create space for search bar
            for (Tab tab: tabPane.getTabs()){
                VirtualizedScrollPane scrollPane =
                        (VirtualizedScrollPane) tab.getContent();
                scrollPane.getContent().setLayoutY(40);
            }
            anchorPane.getChildren().add(searchBar);
            barOpen = true;
        }
    }

    /**
     * Creates buttons for search bar.
     *
     * @param searchBar ToolBar which contains search/replace text fields
     *                  along with five related buttons
     *
     * @return five buttons in the search bar as an ArrayList
     */
    private ArrayList<Button> createSearchBarButtons(ToolBar searchBar) {
        // create a button that closes the search bar
        // and modifies associated fields accordingly
        Button closeBarButton = new Button("x");
        closeBarButton.setId("closeBar");
        closeBarButton.setOnAction(e ->{
            closeSearchBar(searchBar);
        });

        // create a button that moves the highlighting to the next found item
        Button findNextButton = new Button("next");
        findNextButton.setOnAction(e -> {
            if (searchResults.size() > 1 && searchIndex[0] < searchResults.size()-1){
                searchIndex[0] += 1;
                Util.getSelectedTextBox(tabPane).selectRange(
                        searchResults.get(searchIndex[0]),
                        searchResults.get(
                                searchIndex[0])+ searchField.getLength());
            }
        });

        // create a button that moves the highlighting to the previous found item
        Button findPrevButton = new Button("prev");
        findPrevButton.setOnAction(e -> {
            if (searchResults.size() > 1 && searchIndex[0] > 0){
                searchIndex[0] -= 1;
                Util.getSelectedTextBox(tabPane).selectRange(
                        searchResults.get(searchIndex[0]),
                        searchResults.get(
                                searchIndex[0])+ searchField.getLength());
            }
        });

        // create a button that replaces the found result with the text
        // in the replace field
        Button replaceButton = new Button("replace");
        replaceButton.setOnAction(e -> {
            if (barOpen){
                Util.getSelectedTextBox(tabPane).replaceSelection(replaceField.getText());
                // resets the search results after replacing the selected text
                searchResults.clear();
            }
        });

        // create a button that replaces all the found search results
        Button replaceAllButton = new Button("replace all");
        replaceAllButton.setOnAction(e -> {
            if (barOpen){
                // loop through the search results and replace them
                for (int i = 0; i < searchResults.size(); i++){
                    Util.getSelectedTextBox(tabPane).selectRange(searchResults.get(i),
                            searchResults.get(i)+searchField.getLength());
                    Util.getSelectedTextBox(tabPane).replaceSelection(
                            replaceField.getText());

                    // update the indices of the remaining search results
                    for (int j = i+1; j < searchResults.size(); j++){
                        Integer curValue = searchResults.get(j);
                        searchResults.set(j,
                                curValue - (searchField.getLength()
                                        - replaceField.getLength()));
                    }
                }
                searchResults.clear();
            }
        });

        // return the buttons as a list
        ArrayList<Button> buttons =
                new ArrayList<>(Arrays.asList(findNextButton, findPrevButton,
                        replaceButton, replaceAllButton, closeBarButton));
        return buttons;
    }

    /**
     * Closes the search bar and its associated fields.
     *
     * @param searchBar ToolBar which contains search/replace text fields
     *                  along with five related buttons
     */
    private void closeSearchBar(ToolBar searchBar){
        anchorPane.getChildren().remove(searchBar);
        // loop through all open tabs and reset their code area y positions to default
        for (Tab tab: tabPane.getTabs()){
            VirtualizedScrollPane scrollPane = (VirtualizedScrollPane) tab.getContent();
            scrollPane.getContent().setLayoutY(0);
        }
        barOpen = false;
        searchField = null;
        replaceField = null;
        searchResults.clear();
        searchIndex[0] = 0;
        resultUpdate.set("");
    }


    /**
     * Toggle comment status on a single line.
     *
     * If a line is already a comment line, then using this method will make it a
     * code line otherwise it will be changed to comment line.
     *
     * @return boolean that indicates if toggleComment is commenting out the current line.
     * @author Ricky Peng
     */
    private boolean toggleCommentSingleLine() {
        CodeArea codeArea = Util.getSelectedTextBox(tabPane);
        boolean isCommenting;
        codeArea.selectLine();
        String lineText = codeArea.getSelectedText();
        if (isComment(lineText)) {
            // The line is a comment, needs to be converted to code line
            lineText = lineText.replaceFirst("//\\s?", "");
            isCommenting = true;
        } else {
            // The line is a code line, needs to be comment out
            int sentenceIndex = 0;
            for (char c : lineText.toCharArray()) {
                if (c != '\t') break;
                else sentenceIndex += 1;
            }
            lineText = lineText.substring(0, sentenceIndex) + "// "
                    + lineText.substring(sentenceIndex, lineText.length());
            isCommenting = false;
        }
        codeArea.replaceSelection(lineText);
        codeArea.deselect();
        return isCommenting;
    }

    /**
     * Determine if one line is a code line or comment line.
     *
     * @param lineText the input line
     * @return true if it is a comment line, false otherwise
     */
    private boolean isComment(String lineText) {
        return lineText.contains("//");
    }


}
