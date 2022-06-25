/*
 * File: PrettyPrinter.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 10
 * Date: May 6, 2022
 */

package proj10PengXuYu.bantam.visitor;

import proj10PengXuYu.bantam.ast.*;
import proj10PengXuYu.bantam.parser.Parser;
import proj10PengXuYu.bantam.semant.SemanticAnalyzer;
import proj10PengXuYu.bantam.util.*;
import proj10PengXuYu.bantam.util.Error;

/**
 * TranslatorVisitor class creates a visitor that traverses the AST to
 * translate a legal Bantam Java program to an equivalent Java program.
 * When visiting each node in the tree, it adds the pretty formatted
 * Java code to the outString field.
 *
 * Note: Comments are ignored because they are thrown away by the scanner.
 */
public class TranslatorVisitor extends PrettyPrinterVisitor {

    /* Add the protected keyword in front of all fields */
    @Override
    public Object visit(Field node) {
        addIndentation();
        addCode( "protected " + node.getType() + " " + node.getName());
        if (node.getInit() != null) {
            addCode(" = ");
            node.getInit().accept(this);
        }
        addCode("; \n");
        return null;
    }

    /* Add the public keyword in front of all methods */
    @Override
    public Object visit(Method node) {
        addCode("\n");
        addIndentation();
        addCode("public " + node.getReturnType() + " " + node.getName() + "(");
        node.getFormalList().accept(this);
        // remove the last two chars, ie. "," and " ", if formal list is not empty
        int strLength = getOutString().length();
        if (getOutString().charAt(strLength-1) != '(') {
            addCode(getOutString().substring(0, strLength - 2));
        }
        addCode(") ");
        startBlock();
        node.getStmtList().accept(this);
        endBlock();
        return null;
    }

    /* Add the static main method to the Main class */
    @Override
    public String visit(Class_ node) {
        addCode("\n");
        addCode( "class " + node.getName() + " ");
        if (! node.getParent().equals("Object")) {
            addCode("extends " + node.getParent() + " ");
        }
        startBlock();
        node.getMemberList().accept(this);
        if (node.getName().equals("Main")) {
            addCode("\n");
            addIndentation();
            addCode("public static void main(String[] args) {\n");
            addIndentation();
            addCode( "\t(new Main()).main();\n");
            addIndentation();
            addCode("} \n");
        }
        endBlock();
        return null;
    }

    /*  Create classes TextIO and Sys in Java */
    @Override
    public Object visit(Program node) {

        addCode("import java.util.Random;\n");
        addCode("import java.io.FileNotFoundException;\n");
        addCode("import java.io.PrintStream;\n");
        addCode("import java.io.InputStream;\n\n");

        // Sys class
        addCode("final class Sys {\n");
        addCode("\tpublic void exit(int status) " +
                "{\n\t\tSystem.exit(status);\n\t}\n\n");
        addCode("\tpublic int random() {\n\t\tRandom random = new Random();" +
                "\n\t\treturn random.nextInt();\n\t}");
        addCode("\tpublic int time() {\n\t\tlong ms = System.currentTimeMillis();" +
                "\n\t\treturn (int) (ms / 1000);\n\t}\n");
        addCode("}\n\n");

        // Text IO class
        addCode("final class TextIO {\n");
        addCode("\tprivate final InputStream DEFAULT_IN = System.in;\n");
        addCode("\tprivate final PrintStream DEFAULT_OUT = System.out;\n");
        addCode("\tprivate final PrintStream DEFAULT_ERR = System.err;\n\n");
        addCode("\tpublic void writeFile(String filename) " +
                "{\n\t\ttry {\n\t\t\tSystem.setOut(new PrintStream(filename));\n\t\t} " +
                "catch (FileNotFoundException e) " +
                "{\n\t\t\tSystem.out.println(\"Error: " +
                "file \" + filename + \" is not found.\");\n\t\t}\n\t}\n\n");
        addCode("\tpublic void readStdin() " +
                "{\n\t\tSystem.setIn(DEFAULT_IN);\n\t}\n\n");
        addCode("\tpublic void writeStdout() " +
                "{\n\t\tSystem.setOut(DEFAULT_OUT);\n\t}\n\n");
        addCode("\tpublic void writeStderr() " +
                "{\n\t\tSystem.setErr(DEFAULT_ERR);\n\t}\n\n");
        addCode("\tpublic void putString(String s) " +
                "{\n\t\tSystem.out.println(s);\n\t}\n\n");
        addCode("\tpublic void putInt(int i) " +
                "{\n\t\tSystem.out.println(i);\n\t}\n");
        addCode("}\n\n");

        node.getClassList().accept(this);
        return null;
    }

    private void addCode(String newCode) {
        setOutString(getOutString() + newCode);
    }


    // * for testing purpose
    public static void main(String[] args) {
        TranslatorVisitor translator = new TranslatorVisitor();
        for (String filename: args) {
            ErrorHandler errorHandler = new ErrorHandler();
            Parser parser = new Parser(errorHandler);
            SemanticAnalyzer analyzer = new SemanticAnalyzer(errorHandler);
            try {
                Program root = parser.parse(filename);
                analyzer.analyze(root);
                // assumes that the given parse tree is error free
                // else, compilation error will be caught
                String result = translator.generateOutputString(root);
                System.out.println(result);
            } catch (CompilationException ex) {
                System.out.println("Illegal Bantam Java program: " + filename);
                for (Error error : errorHandler.getErrorList()) {
                    System.out.println(error.toString());
                    ex.printStackTrace();
                }
            }
        }
    }

}
