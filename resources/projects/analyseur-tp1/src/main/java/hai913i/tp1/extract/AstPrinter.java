package hai913i.tp1.extract;

import java.io.PrintStream;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class AstPrinter extends ASTVisitor {

    private final PrintStream out;
    private int depth;

    public AstPrinter(PrintStream out) {
        this.out = out;
    }

    @Override
    public boolean preVisit2(ASTNode node) {
        out.println("  ".repeat(depth) + node.getClass().getSimpleName());
        depth++;
        return true;
    }

    @Override
    public void postVisit(ASTNode node) {
        depth--;
    }
}
