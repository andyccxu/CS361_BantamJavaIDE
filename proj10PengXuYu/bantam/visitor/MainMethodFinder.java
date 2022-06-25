/**
 * File: MainMethodFinder
 * Name: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS361
 * Project 9
 * Date: April 24, 2022
 *
 * Code is implemented from homework and posted on Moodle by Dale
 */

package proj10PengXuYu.bantam.visitor;

import proj10PengXuYu.bantam.ast.ASTNode;
import proj10PengXuYu.bantam.ast.Class_;
import proj10PengXuYu.bantam.ast.Field;
import proj10PengXuYu.bantam.ast.Method;

public class MainMethodFinder extends Visitor
{
    private boolean hasMainMethod;


    public boolean hasMain(ASTNode rootNode) {
        hasMainMethod = false;
        rootNode.accept(this);
        return hasMainMethod;
    }

    @Override
    public Object visit(Class_ node) {
        if ("Main".equals(node.getName()))
            node.getMemberList().accept(this);
        return null;
    }

    @Override
    public Object visit(Method node) {
        if ("main".equals(node.getName()) && "void".equals(node.getReturnType())
                && node.getFormalList().getSize() == 0)
            hasMainMethod = true;
        return null;
    }

    @Override
    public Object visit(Field node) {
        return null;
    }
}
