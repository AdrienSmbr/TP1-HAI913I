package hai913i.tp1.extract;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class AstPrinter extends ASTVisitor {

    private int depth;

    @Override
    public boolean preVisit2(ASTNode node) {
        System.out.println("  ".repeat(depth) + node.getClass().getSimpleName());
        depth++;
        return true;
    }

    @Override
    public void postVisit(ASTNode node) {
        depth--;
    }
}
