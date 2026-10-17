package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import hai913i.tp1.model.CallFact;

class CallCollector extends ASTVisitor {

    private static final String UNKNOWN = "(inconnu)";

    private final CompilationUnit unit;
    private final String callerId;
    private final List<CallFact> calls = new ArrayList<>();

    CallCollector(CompilationUnit unit, String callerId) {
        this.unit = unit;
        this.callerId = callerId;
    }

    List<CallFact> calls() {
        return calls;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        IMethodBinding target = node.resolveMethodBinding();
        calls.add(new CallFact(callerId,
                node.getName().getIdentifier(),
                targetIdOf(target),
                receiverOf(node, target),
                lineOf(node),
                target != null,
                isInternal(target)));
        return true;
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        IMethodBinding target = node.resolveMethodBinding();
        calls.add(new CallFact(callerId,
                node.getName().getIdentifier(),
                targetIdOf(target),
                superReceiverOf(node),
                lineOf(node),
                target != null,
                isInternal(target)));
        return true;
    }

    private String receiverOf(MethodInvocation node, IMethodBinding target) {
        Expression receiver = node.getExpression();
        if (receiver != null) {
            return nameOf(receiver.resolveTypeBinding());
        }
        if (target != null && Modifier.isStatic(target.getModifiers())) {
            return nameOf(target.getDeclaringClass());
        }
        return nameOf(enclosingTypeOf(node));
    }

    private String superReceiverOf(SuperMethodInvocation node) {
        ITypeBinding enclosing = enclosingTypeOf(node);
        return enclosing != null ? nameOf(enclosing.getSuperclass()) : UNKNOWN;
    }

    private Optional<String> targetIdOf(IMethodBinding target) {
        if (target == null) {
            return Optional.empty();
        }
        IMethodBinding declaration = target.getMethodDeclaration();
        StringBuilder parameters = new StringBuilder();
        for (ITypeBinding parameter : declaration.getParameterTypes()) {
            if (parameters.length() > 0) {
                parameters.append(",");
            }
            parameters.append(nameOf(parameter));
        }
        return Optional.of(nameOf(declaration.getDeclaringClass())
                + "#" + declaration.getName() + "(" + parameters + ")");
    }

    private boolean isInternal(IMethodBinding target) {
        if (target == null) {
            return false;
        }
        ITypeBinding owner = target.getMethodDeclaration().getDeclaringClass();
        return owner != null && owner.isFromSource();
    }

    private ITypeBinding enclosingTypeOf(ASTNode node) {
        for (ASTNode current = node; current != null; current = current.getParent()) {
            if (current instanceof AnonymousClassDeclaration anonymous) {
                return anonymous.resolveBinding();
            }
            if (current instanceof AbstractTypeDeclaration type) {
                return type.resolveBinding();
            }
        }
        return null;
    }

    private String nameOf(ITypeBinding binding) {
        if (binding == null) {
            return UNKNOWN;
        }
        ITypeBinding erasure = binding.getErasure();
        String qualified = erasure.getQualifiedName();
        return qualified.isEmpty() ? erasure.getBinaryName() : qualified;
    }

    private int lineOf(ASTNode node) {
        return unit.getLineNumber(node.getStartPosition());
    }
}
