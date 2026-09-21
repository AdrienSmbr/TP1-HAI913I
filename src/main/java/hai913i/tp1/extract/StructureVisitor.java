package hai913i.tp1.extract;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;

public class StructureVisitor extends ASTVisitor {

    private static final String OBJECT = "java.lang.Object";

    private final CompilationUnit unit;
    private final List<TypeFact> types = new ArrayList<>();

    public StructureVisitor(CompilationUnit unit) {
        this.unit = unit;
    }

    public List<TypeFact> types() {
        return types;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.isLocalTypeDeclaration()) {
            return false;
        }
        record(node, node.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS);
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        record(node, TypeKind.ENUM);
        return true;
    }

    @Override
    public boolean visit(RecordDeclaration node) {
        record(node, TypeKind.RECORD);
        return true;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        return false;
    }

    private void record(AbstractTypeDeclaration node, TypeKind kind) {
        ITypeBinding binding = node.resolveBinding();
        String qualifiedName = binding != null
                ? binding.getQualifiedName()
                : node.getName().getFullyQualifiedName();

        types.add(new TypeFact(qualifiedName,
                packageOf(binding),
                kind,
                superclassesOf(binding),
                interfacesOf(binding),
                fieldsOf(node),
                methodsOf(node, qualifiedName, kind)));
    }

    private String packageOf(ITypeBinding binding) {
        if (binding != null && binding.getPackage() != null) {
            return binding.getPackage().getName();
        }
        return unit.getPackage() != null
                ? unit.getPackage().getName().getFullyQualifiedName()
                : "(defaut)";
    }

    private List<String> superclassesOf(ITypeBinding binding) {
        List<String> chain = new ArrayList<>();
        if (binding == null) {
            return chain;
        }
        for (ITypeBinding current = binding.getSuperclass();
                current != null && !OBJECT.equals(current.getErasure().getQualifiedName());
                current = current.getSuperclass()) {
            chain.add(current.getErasure().getQualifiedName());
        }
        return chain;
    }

    private List<String> interfacesOf(ITypeBinding binding) {
        List<String> names = new ArrayList<>();
        if (binding == null) {
            return names;
        }
        for (ITypeBinding itf : binding.getInterfaces()) {
            names.add(itf.getErasure().getQualifiedName());
        }
        return names;
    }

    private List<FieldFact> fieldsOf(AbstractTypeDeclaration node) {
        List<FieldFact> fields = new ArrayList<>();
        for (Object member : node.bodyDeclarations()) {
            if (member instanceof FieldDeclaration field) {
                Visibility visibility = visibilityOf(field, node);
                String declaredType = field.getType().resolveBinding() != null
                        ? field.getType().resolveBinding().getErasure().getQualifiedName()
                        : field.getType().toString();
                for (Object fragment : field.fragments()) {
                    VariableDeclarationFragment variable = (VariableDeclarationFragment) fragment;
                    fields.add(new FieldFact(variable.getName().getIdentifier(),
                            declaredType, visibility));
                }
            }
        }
        return fields;
    }

    private List<MethodFact> methodsOf(AbstractTypeDeclaration node, String owner, TypeKind kind) {
        List<MethodFact> methods = new ArrayList<>();
        for (Object member : node.bodyDeclarations()) {
            if (member instanceof MethodDeclaration method) {
                List<String> parameterTypes = parameterTypesOf(method);
                methods.add(new MethodFact(
                        owner + "#" + method.getName().getIdentifier()
                                + "(" + String.join(",", parameterTypes) + ")",
                        method.getName().getIdentifier(),
                        parameterTypes,
                        method.isConstructor(),
                        visibilityOf(method, node),
                        bodyLoc(method)));
            }
        }
        return methods;
    }

    private List<String> parameterTypesOf(MethodDeclaration method) {
        List<String> names = new ArrayList<>();
        IMethodBinding binding = method.resolveBinding();
        if (binding != null) {
            for (ITypeBinding parameter : binding.getParameterTypes()) {
                names.add(parameter.getErasure().getQualifiedName());
            }
            return names;
        }
        for (Object parameter : method.parameters()) {
            names.add(((SingleVariableDeclaration) parameter).getType().toString());
        }
        return names;
    }

    private int bodyLoc(MethodDeclaration method) {
        if (method.getBody() == null) {
            return 0;
        }
        int start = unit.getLineNumber(method.getBody().getStartPosition());
        int end = unit.getLineNumber(
                method.getBody().getStartPosition() + method.getBody().getLength() - 1);
        return end - start + 1;
    }

    private Visibility visibilityOf(BodyDeclaration member, AbstractTypeDeclaration owner) {
        int flags = member.getModifiers();
        if (Modifier.isPublic(flags)) {
            return Visibility.PUBLIC;
        }
        if (Modifier.isProtected(flags)) {
            return Visibility.PROTECTED;
        }
        if (Modifier.isPrivate(flags)) {
            return Visibility.PRIVATE;
        }
        boolean inInterface = owner instanceof TypeDeclaration type && type.isInterface();
        return inInterface ? Visibility.PUBLIC : Visibility.PACKAGE;
    }
}
