/*
 * File: Scanner.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 9
 * Date: April 24, 2022
 */


package proj10PengXuYu.bantam.lexer;

import proj10PengXuYu.bantam.util.CompilationException;
import proj10PengXuYu.bantam.util.Error;
import proj10PengXuYu.bantam.util.ErrorHandler;

import java.io.IOException;
import java.io.Reader;

/**
 * This class reads characters from a file or a Reader
 * and breaks it into Tokens.
 */
public class Scanner
{
    /** the source of the characters to be broken into tokens */
    private SourceFile sourceFile;
    /** collector of all errors that occur */
    private ErrorHandler errorHandler;
    /** previous character that was not returned as token */
    private char prevChar = ' ';

    /**
     * creates a new scanner for the given file
     * @param filename the name of the file to be scanned
     * @param handler the ErrorHandler that collects all the errors found
     */
    public Scanner(String filename, ErrorHandler handler) {
        errorHandler = handler;
        sourceFile = new SourceFile(filename);
    }

    /**
     * creates a new scanner for the given file
     * @param reader the Reader object for the file to be scanned
     * @param handler the ErrorHandler that collects all the errors found
     */
    public Scanner(Reader reader, ErrorHandler handler) {
        errorHandler = handler;
        sourceFile = new SourceFile(reader);
    }

    /**
     * read characters and collect them into a Token.
     * It ignores white space unless it is inside a string or a comment.
     * It returns an EOF Token if all characters from the sourceFile have
     * already been read.
     * @return the Token containing the characters read
     */
    public Token scan() {
        try {
            // get the first character
            char firstChar;
            if (prevChar != ' ') {
                firstChar = prevChar;
                prevChar = ' '; // reset the prevChar
            } else {
                firstChar = sourceFile.getNextChar();
            }
            // throw away whitespaces and continue
            while (Character.isWhitespace(firstChar)) {
                firstChar = sourceFile.getNextChar();
            }
            // EOF
            if (firstChar == SourceFile.EOF) {
                return new Token(Token.Kind.EOF,
                        "", sourceFile.getCurrentLineNumber());
            }
            // int constant
            else if (Character.isDigit(firstChar)) {
                String spelling = "" + firstChar;
                // repetitively get the next int
                char nextChar = sourceFile.getNextChar();
                while (Character.isDigit(nextChar)) {
                    spelling += nextChar;
                    nextChar = sourceFile.getNextChar();
                }
                // check error: integer const that are too long
                try {
                    Integer.parseInt(spelling);
                } catch (NumberFormatException ex) {
                    errorHandler.register(Error.Kind.LEX_ERROR,
                            sourceFile.getFilename(),
                            sourceFile.getCurrentLineNumber(),
                            "Integer constant is too long!");
                    return new Token(Token.Kind.ERROR,
                            spelling, sourceFile.getCurrentLineNumber());
                }
                prevChar = nextChar;
                return new Token(Token.Kind.INTCONST,
                        spelling, sourceFile.getCurrentLineNumber());
            }
            // brackets
            else if (firstChar == '{') {
                return new Token(Token.Kind.LCURLY,
                        "{", sourceFile.getCurrentLineNumber());
            } else if (firstChar == '}') {
                return new Token(Token.Kind.RCURLY,
                        "}", sourceFile.getCurrentLineNumber());
            } else if (firstChar == '(') {
                return new Token(Token.Kind.LPAREN,
                        "(", sourceFile.getCurrentLineNumber());
            } else if (firstChar == ')') {
                return new Token(Token.Kind.RPAREN,
                        ")", sourceFile.getCurrentLineNumber());
            }
            // punctuation
            else if (firstChar == '.') {
                return new Token(Token.Kind.DOT,
                        ".", sourceFile.getCurrentLineNumber());
            } else if (firstChar == ':'){
                return new Token(Token.Kind.COLON,
                        ":", sourceFile.getCurrentLineNumber());
            } else if (firstChar == ';') {
                return new Token(Token.Kind.SEMICOLON,
                        ";", sourceFile.getCurrentLineNumber());
            } else if (firstChar == ',') {
                return new Token(Token.Kind.COMMA,
                        ",", sourceFile.getCurrentLineNumber());
            }
            // identifier
            else if (Character.isLetter(firstChar)) {
                String spelling = Character.toString(firstChar);
                char nextChar = sourceFile.getNextChar();
                while (Character.isLetter(nextChar)
                        || Character.isDigit(nextChar) || nextChar == '_') {
                    spelling += nextChar;
                    nextChar = sourceFile.getNextChar();
                }
                prevChar = nextChar;
                int lineNum = sourceFile.getCurrentLineNumber();
                if (nextChar == '\n' || nextChar == '\r') {
                    lineNum--;
                }
                return new Token(Token.Kind.IDENTIFIER, spelling, lineNum);
            }
            // string constant
            else if (firstChar == '"'){
                return scanString(firstChar);
            }
            // binary logic operator: and
            else if (firstChar == '&'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '&'){
                    prevChar = ' ';
                    return new Token(Token.Kind.BINARYLOGIC,
                            "&&", sourceFile.getCurrentLineNumber());
                }
                else {
                    firstChar = prevChar;
                    prevChar = ' ';
                    errorHandler.register(Error.Kind.LEX_ERROR,
                            sourceFile.getFilename(),
                            sourceFile.getCurrentLineNumber(),
                            "Unsupported character!");
                    return new Token(Token.Kind.ERROR,
                            "&", sourceFile.getCurrentLineNumber()-1);
                }
            }
            // binary logic operator: or
            else if (firstChar == '|'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '|'){
                    prevChar = ' ';
                    return new Token(Token.Kind.BINARYLOGIC,
                            "||", sourceFile.getCurrentLineNumber());
                }
                else {
                    errorHandler.register(Error.Kind.LEX_ERROR,
                            sourceFile.getFilename(),
                            sourceFile.getCurrentLineNumber(), "Unsupported character!");
                    return new Token(Token.Kind.ERROR,
                            "|", sourceFile.getCurrentLineNumber());
                }
            }

            // plus and unary increment operator
            else if (firstChar == '+'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '+'){
                    prevChar = ' ';
                    return new Token(Token.Kind.UNARYINCR,
                            "++", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.PLUSMINUS,
                            "+", sourceFile.getCurrentLineNumber());
                }
            }

            // minus and unary decrement operator
            else if (firstChar == '-'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '-'){
                    prevChar = ' ';
                    return new Token(Token.Kind.UNARYDECR,
                            "--", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.PLUSMINUS,
                            "-", sourceFile.getCurrentLineNumber());
                }
            }

            // multiply operator
            else if (firstChar == '*'){
                return new Token(Token.Kind.MULDIV,
                        "*", sourceFile.getCurrentLineNumber());
            }

            // mod operator
            else if (firstChar == '%'){
                return new Token(Token.Kind.MULDIV,
                        "%", sourceFile.getCurrentLineNumber());
            }

            // compare operators
            else if (firstChar == '<'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '='){
                    prevChar = ' ';
                    return new Token(Token.Kind.COMPARE,
                            "<=", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.COMPARE,
                            "<", sourceFile.getCurrentLineNumber());
                }
            }
            else if (firstChar == '>'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '='){
                    prevChar = ' ';
                    return new Token(Token.Kind.COMPARE,
                            ">=", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.COMPARE,
                            ">", sourceFile.getCurrentLineNumber());
                }
            }
            // compare and assignment operator
            else if (firstChar == '='){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '='){
                    prevChar = ' ';
                    return new Token(Token.Kind.COMPARE,
                            "==", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.ASSIGN,
                            "=", sourceFile.getCurrentLineNumber());
                }
            }
            // compare and unary not operator
            else if (firstChar == '!'){
                prevChar = sourceFile.getNextChar();
                if (prevChar == '='){
                    prevChar = ' ';
                    return new Token(Token.Kind.COMPARE,
                            "!=", sourceFile.getCurrentLineNumber());
                }
                else {
                    return new Token(Token.Kind.UNARYNOT,
                            "!", sourceFile.getCurrentLineNumber());
                }
            }
            // line/block comment or division operator
            else if (firstChar == '/'){
                return scanCommentDiv();
            }
            else {
                // catch all the edge cases
                errorHandler.register(Error.Kind.LEX_ERROR, sourceFile.getFilename(),
                        sourceFile.getCurrentLineNumber(), "Unsupported character!");
                return new Token(Token.Kind.ERROR, Character.toString(firstChar),
                        sourceFile.getCurrentLineNumber());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // in case of IOException and to satisfy the java compiler
    }


    /**
     * Helper method for scan(); scans for the string constant
     * @return the Token containing the characters read
     */
    public Token scanString(char firstChar){
        try {
            String string = Character.toString(firstChar);
            // record the starting point
            int start = sourceFile.getCurrentLineNumber();
            char nextChar = sourceFile.getNextChar();
            string += nextChar;
            char prev = '"'; // store the prev character for looping
            // read in until closing double quotation is read in
            while (prev == '\\' || nextChar != '"'){
                // handles unterminated string constants
                if (nextChar == SourceFile.EOF) {
                    errorHandler.register(Error.Kind.LEX_ERROR,
                            sourceFile.getFilename(),
                            sourceFile.getCurrentLineNumber(),
                            "Unterminated string constants!");
                    return new Token(Token.Kind.ERROR,
                            string.substring(0, string.length()-1),
                            sourceFile.getCurrentLineNumber());
                }
                prev = nextChar;
                nextChar = sourceFile.getNextChar();
                string += nextChar;
            }
            // check for escape character
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == '\\') {
                    // get one more character
                    nextChar = string.charAt(i + 1);
                    if (nextChar != 'n' && nextChar != 't'
                            && nextChar != '"' && nextChar != '\\'
                            && nextChar != 'f') {
                        errorHandler.register(Error.Kind.LEX_ERROR,
                                sourceFile.getFilename(),
                                sourceFile.getCurrentLineNumber(),
                                "Unsupported escape characters within a string.");
                        return new Token(Token.Kind.ERROR,
                                string, sourceFile.getCurrentLineNumber());
                    }
                }
            }
            // check if string constant exceeds 5000 characters
            if (string.length() > 5000) {
                errorHandler.register(Error.Kind.LEX_ERROR,
                        sourceFile.getFilename(),
                        sourceFile.getCurrentLineNumber(),
                        "String constant cannot exceed 5000 characters!");
                return new Token(Token.Kind.ERROR,
                        string, sourceFile.getCurrentLineNumber());
            }
            // check if string spans multiple lines
            if (sourceFile.getCurrentLineNumber() != start){
                errorHandler.register(Error.Kind.LEX_ERROR,
                        sourceFile.getFilename(),
                        sourceFile.getCurrentLineNumber(),
                        "String cannot span multiple lines!");
                return new Token(Token.Kind.ERROR,
                        string, sourceFile.getCurrentLineNumber());
            }
            return new Token(Token.Kind.STRCONST,
                    string, sourceFile.getCurrentLineNumber());
        } catch(IOException e){
            e.printStackTrace();
        }
        return null; // in case of IOException and to satisfy the java compiler
    }

    /**
     * Helper method for scan(); scans for line & block comments and division operator
     * @return the Token containing the characters read
     */
    public Token scanCommentDiv(){
        try {
            char secondChar = sourceFile.getNextChar();
            // if it is a line comment, ignores it
            if (secondChar == '/'){
                // keep reading until end of line
                char nextChar = sourceFile.getNextChar();
                while (nextChar != sourceFile.EOL){
                    nextChar = sourceFile.getNextChar();
                }
                return scan();
            }
            // check if it's a block comment
            else if (secondChar == '*'){
                String comment = "/*";
                char nextChar = sourceFile.getNextChar(); // look for asterisk
                char nextNextChar = sourceFile.getNextChar(); //look for closing slash
                // check for the closing asterisk and slash
                while (nextChar != '*' || nextNextChar != '/'){
                    // handles unterminated block comments
                    if (nextChar ==sourceFile.EOF){
                        errorHandler.register(Error.Kind.LEX_ERROR,
                                sourceFile.getFilename(),
                                sourceFile.getCurrentLineNumber(),
                                "Unterminated block comment!");
                        return new Token(Token.Kind.ERROR,
                                comment, sourceFile.getCurrentLineNumber());
                    }
                    comment += nextChar;
                    nextChar = nextNextChar;
                    nextNextChar = sourceFile.getNextChar();
                }
                return scan();
            }
            else {
                return new Token(Token.Kind.MULDIV,
                        "/", sourceFile.getCurrentLineNumber());
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        return null; // in case of IOException and to satisfy the java compiler
    }

    /**
     * Main method created for testing purposes.
     */
    public static void main(String[] args) {
        // loop through files
        for (String filename: args) {
            // catch the CompilationException
            try{
                Scanner scanner = new Scanner(filename, new ErrorHandler());
                System.out.println("Scanning " + filename);
                // keep track of number of errors
                int numErrors = 0;
                Token token = scanner.scan();
                while (token.kind != Token.Kind.EOF) {
                    if (token.kind == Token.Kind.ERROR) {
                        numErrors++;
                    }
                    System.out.println(token.toString());
                    token = scanner.scan();
                }
                // print messages after scanning
                if (numErrors == 0) {
                    System.out.println("Scanning was successful.");
                } else {
                    System.out.println(String.format("%d errors were found.", numErrors));
                }
            } catch(CompilationException e){
                System.out.println(String.format("Unable to read and scan %s. Probably the "
                        + "file does not exist.", filename));
            }
        }
    }
}