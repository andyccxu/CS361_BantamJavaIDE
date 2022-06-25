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
import proj10PengXuYu.bantam.util.CompilationException;
import proj10PengXuYu.bantam.util.Error;
import proj10PengXuYu.bantam.util.ErrorHandler;

import java.util.Iterator;

/**
 * PrettyPrinterVisitor class creates a visitor that traverses the AST to
 * pretty print the input Bantam Java program. It assumes that the current
 * program is well-formed so that it would be meaningful to pretty print.
 * When visiting each node in the tree, it adds the pretty formatted
 * Bantam Java code to the outString field.
 *
 * Note: Comments are ignored because they are thrown away by the scanner.
 */
public class PrettyPrinterVisitor extends Visitor {

    // stores the pretty printed content in a StringBuilder
    private StringBuilder outString;
    // records the current level of indentation
    private int indentationLevel;

    /**
     * Method for usage of PrettyPrinterVisitor.
     *
     * @param rootNode the root node of the AST to traverse
     * @return String the outString field that stores the output
     */
    public String generateOutputString(ASTNode rootNode) {
        outString = new StringBuilder("");
        indentationLevel = 0;
        rootNode.accept(this);
        return outString.toString();
    }

    /** Get method for outString field */
    public String getOutString() {
        return outString.toString();
    }

    /** Set method for outString field */
    public void setOutString(String newCode) {
        int strLength = outString.length();
        this.outString.replace(0, strLength, newCode);
    }

    @Override
    public String visit(Class_ node) {
        outString.append("class " + node.getName() + " ");
        if (! node.getParent().equals("Object")) {
            outString.append("extends " + node.getParent() + " ");
        }
        startBlock();
        node.getMemberList().accept(this);
        endBlock();
        outString.append("\n");
        return null;
    }

    @Override
    public Object visit(Field node) {
        addIndentation();
        outString.append(node.getType() + " " + node.getName());
        if (node.getInit() != null) {
            outString.append(" = ");
            node.getInit().accept(this);
        }
        outString.append("; \n");
        return null;
    }

    @Override
    public Object visit(Method node) {
        outString.append("\n");
        addIndentation();
        outString.append(node.getReturnType() + " " + node.getName() + "(");
        node.getFormalList().accept(this);
        // remove the last two chars, ie. "," and " ", if formal list is not empty
        int strLength = outString.length();
        if (outString.charAt(strLength-1) != '(') {
            outString.delete(strLength-2, strLength);
        }
        outString.append(") ");
        startBlock();
        node.getStmtList().accept(this);
        endBlock();
        return null;
    }

    @Override
    public Object visit(Formal node) {
        outString.append(node.getType() + " " + node.getName() + ", ");
        return null;
    }

    @Override
    public Object visit(DeclStmt node) {
        addIndentation();
        outString.append("var " + node.getName() + " = ");
        node.getInit().accept(this);
        outString.append("; \n");
        return null;
    }

    @Override
    public Object visit(ExprStmt node) {
        addIndentation();
        node.getExpr().accept(this);
        outString.append("; \n");
        return null;
    }

    @Override
    public Object visit(IfStmt node) {
        addIndentation();
        outString.append("if (");
        node.getPredExpr().accept(this);
        outString.append(") ");
        startBlock();
        node.getThenStmt().accept(this);
        endBlock();
        if (node.getElseStmt() != null) {
            addIndentation();
            outString.append("else ");
            startBlock();
            node.getElseStmt().accept(this);
            endBlock();
        }
        return null;
    }

    @Override
    public Object visit(WhileStmt node) {
        addIndentation();
        outString.append("while (");
        node.getPredExpr().accept(this);
        outString.append(") ");
        startBlock();
        node.getBodyStmt().accept(this);
        endBlock();
        return null;
    }

    @Override
    public Object visit(ForStmt node) {
        addIndentation();
        outString.append("for (");
        if (node.getInitExpr() != null) {
            node.getInitExpr().accept(this);
        }
        outString.append("; ");
        if (node.getPredExpr() != null) {
            node.getPredExpr().accept(this);
        }
        outString.append("; ");
        if (node.getUpdateExpr() != null) {
            node.getUpdateExpr().accept(this);
        }
        outString.append(") ");
        startBlock();
        node.getBodyStmt().accept(this);
        endBlock();
        return null;
    }

    @Override
    public Object visit(BreakStmt node) {
        addIndentation();
        outString.append("break; \n");
        return null;
    }

    @Override
    public Object visit(ReturnStmt node) {
        addIndentation();
        outString.append("return");
        if (node.getExpr() != null) {
            outString.append(" ");
            node.getExpr().accept(this);
        }
        outString.append("; \n");
        return null;
    }

    @Override
    public Object visit(DispatchExpr node) {
        if (node.getRefExpr() != null) {
            node.getRefExpr().accept(this);
            outString.append(".");
        }
        outString.append(node.getMethodName() + "(");
        node.getActualList().accept(this);
        // remove the ending ", " if the actual list is not empty
        if (outString.charAt(outString.length()-1) != '(') {
            int strLength = outString.length();
            outString.delete(strLength-2, strLength);
        }
        outString.append(")");
        return null;
    }

    @Override
    public Object visit(ExprList node) {
        for (Iterator it = node.iterator(); it.hasNext(); ) {
            ((Expr) it.next()).accept(this);
            outString.append(", ");
        }
        return null;
    }

    @Override
    public Object visit(NewExpr node) {
        outString.append("new " + node.getType() + "()");
        return null;
    }

    @Override
    public Object visit(InstanceofExpr node) {
        node.getExpr().accept(this);
        outString.append(" instanceof " + node.getType());
        return null;
    }

    @Override
    public Object visit(CastExpr node) {
        outString.append("cast(" + node.getType() + ", ");
        node.getExpr().accept(this);
        outString.append(")");
        return null;
    }

    @Override
    public Object visit(AssignExpr node) {
        if (node.getRefName() != null) {
            outString.append(node.getRefName() + ".");
        }
        outString.append(node.getName() + " = ");
        node.getExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompEqExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompNeExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompLtExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompLeqExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompGtExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryCompGeqExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryArithPlusExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryArithMinusExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryArithTimesExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryArithDivideExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryArithModulusExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryLogicAndExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(BinaryLogicOrExpr node) {
        node.getLeftExpr().accept(this);
        outString.append(" " + node.getOpName() + " ");
        node.getRightExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(UnaryNegExpr node) {
        outString.append(node.getOpName());
        node.getExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(UnaryNotExpr node) {
        outString.append(node.getOpName());
        node.getExpr().accept(this);
        return null;
    }

    @Override
    public Object visit(UnaryIncrExpr node) {
        if (node.isPostfix()) {
            node.getExpr().accept(this);
            outString.append(node.getOpName());
        } else {
            outString.append(node.getOpName());
            node.getExpr().accept(this);
        }
        return null;
    }

    @Override
    public Object visit(UnaryDecrExpr node) {
        if (node.isPostfix()) {
            node.getExpr().accept(this);
            outString.append(node.getOpName());
        } else {
            outString.append(node.getOpName());
            node.getExpr().accept(this);
        }
        return null;
    }

    @Override
    public Object visit(VarExpr node) {
        if (node.getRef() != null) {
            node.getRef().accept(this);
            outString.append(".");
        }
        outString.append(node.getName());
        return null;
    }


    @Override
    public Object visit(ConstIntExpr node) {
        outString.append(node.getIntConstant());
        return null;
    }

    @Override
    public Object visit(ConstBooleanExpr node) {
        outString.append(node.getConstant());
        return null;
    }

    @Override
    public Object visit(ConstStringExpr node) {
        outString.append(node.getConstant());
        return null;
    }


    /** Add the right level of indentation at the start of a new line. */
    public void addIndentation() {
        outString.append(new String(new char[indentationLevel]).replace("\0", "\t"));
    }

    /** Start a new block by print out a left curly brace and switch to a new line. */
    public void startBlock() {
        outString.append("{ \n");
        indentationLevel++;
    }

    /** Add the right curly brace at the right level of indentation. */
    public void endBlock() {
        indentationLevel--;
        addIndentation();
        outString.append("} \n");
    }


    // * for testing purpose
    public static void main(String[] args) {
        PrettyPrinterVisitor prettyPrinterVisitor = new PrettyPrinterVisitor();
        for (String filename: args) {
            ErrorHandler errorHandler = new ErrorHandler();
            Parser parser = new Parser(errorHandler);
            try {
                Program root = parser.parse(filename);
                String result = prettyPrinterVisitor.generateOutputString(root);
                System.out.println(result);
            } catch (CompilationException ex) {
                for (Error error : errorHandler.getErrorList()) {
                    System.out.println(error.toString());
                    ex.printStackTrace();
                }
            }
        }
    }
}
