package hai913i.tp1.spoon;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;
import spoon.Launcher;
import spoon.OutputType;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Bonus C1 : seconde extraction, avec Spoon 11.5.0, vers le <b>meme</b> modele
 * de faits que l'extraction JDT.
 *
 * <p>C'est l'interet du decouplage de l'etape B1 : les metriques et le graphe
 * d'appel fonctionnent a l'identique sur l'une ou l'autre extraction, sans
 * qu'une ligne de traitement ne change. Les conventions de comptage du sujet
 * sont reappliquees ici, car celles de Spoon different (voir
 * {@link SpoonRawCounts}).</p>
 */
public final class SpoonExtractor {

    private SpoonExtractor() {
    }

    public static CtModel buildModel(Path sourceRoot) {
        Launcher launcher = new Launcher();
        launcher.addInputResource(sourceRoot.toString());
        // Classpath complet : on veut les memes exigences de resolution que JDT.
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(17);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setShouldCompile(false);
        launcher.getEnvironment().setOutputType(OutputType.NO_OUTPUT);
        return launcher.buildModel();
    }

    public static ProjectFacts extract(Path sourceRoot, List<Path> sourceFiles) {
        CtModel model = buildModel(sourceRoot);

        List<TypeFact> types = new ArrayList<>();
        for (CtType<?> type : model.getElements(new TypeFilter<>(CtType.class))) {
            if (!isCounted(type)) {
                continue;
            }
            types.add(new TypeFact(
                    qualifiedName(type),
                    type.getPackage() == null ? "(defaut)" : type.getPackage().getQualifiedName(),
                    kindOf(type),
                    superclassesOf(type),
                    interfacesOf(type),
                    fieldsOf(type),
                    methodsOf(type)));
        }
        types.sort(Comparator.comparing(TypeFact::qualifiedName));
        return new ProjectFacts(List.copyOf(types), List.copyOf(sourceFiles));
    }

    /** Types nommes, de premier niveau ou membres. Ni anonymes, ni locaux. */
    private static boolean isCounted(CtType<?> type) {
        if (type.isAnonymous() || type.isImplicit()) {
            return false;
        }
        if (type instanceof CtClass<?> clazz && clazz.isLocalType()) {
            return false;
        }
        return !type.isShadow();
    }

    /** Spoon separe les types membres par $ ; le sujet demande des points. */
    private static String qualifiedName(CtType<?> type) {
        return type.getQualifiedName().replace('$', '.');
    }

    private static TypeKind kindOf(CtType<?> type) {
        if (type instanceof CtEnum) {
            return TypeKind.ENUM;
        }
        if (type instanceof CtRecord) {
            return TypeKind.RECORD;
        }
        return type.isInterface() ? TypeKind.INTERFACE : TypeKind.CLASS;
    }

    private static List<String> superclassesOf(CtType<?> type) {
        List<String> chain = new ArrayList<>();
        for (CtTypeReference<?> current = type.getSuperclass();
                current != null && !"java.lang.Object".equals(current.getQualifiedName());
                current = current.getSuperclass()) {
            chain.add(current.getQualifiedName().replace('$', '.'));
        }
        return chain;
    }

    private static List<String> interfacesOf(CtType<?> type) {
        return type.getSuperInterfaces().stream()
                .map(reference -> reference.getQualifiedName().replace('$', '.'))
                .sorted()
                .toList();
    }

    /** getFields() inclut les constantes d'enumeration : le sujet les exclut. */
    private static List<FieldFact> fieldsOf(CtType<?> type) {
        List<FieldFact> fields = new ArrayList<>();
        for (CtField<?> field : type.getFields()) {
            if (field instanceof CtEnumValue || field.isImplicit()) {
                continue;
            }
            fields.add(new FieldFact(field.getSimpleName(),
                    field.getType().getQualifiedName().replace('$', '.'),
                    visibilityOf(field, type)));
        }
        return fields;
    }

    /**
     * getMethods() rend les methodes <b>sans</b> les constructeurs, qui
     * s'obtiennent par CtClass.getConstructors(). Les membres implicites
     * (constructeur par defaut, values() d'une enumeration) sont ecartes.
     */
    private static List<MethodFact> methodsOf(CtType<?> type) {
        List<CtExecutable<?>> executables = new ArrayList<>();
        type.getMethods().stream()
                .filter(method -> !method.isImplicit())
                .sorted(Comparator.comparing(CtMethod::getSimpleName))
                .forEach(executables::add);
        if (type instanceof CtClass<?> clazz) {
            clazz.getConstructors().stream()
                    .filter(constructor -> !constructor.isImplicit())
                    .forEach(executables::add);
        }

        List<MethodFact> methods = new ArrayList<>();
        for (CtExecutable<?> executable : executables) {
            boolean constructor = executable instanceof CtConstructor;
            String name = constructor ? type.getSimpleName() : executable.getSimpleName();
            List<String> parameterTypes = executable.getParameters().stream()
                    .map(parameter -> parameter.getType().getQualifiedName().replace('$', '.'))
                    .toList();
            String id = qualifiedName(type) + "#" + name
                    + "(" + String.join(",", parameterTypes) + ")";
            methods.add(new MethodFact(id, name, parameterTypes, constructor,
                    visibilityOf((CtModifiable) executable, type),
                    bodyLines(executable),
                    callsOf(executable, id)));
        }
        methods.sort(Comparator.comparing(MethodFact::id));
        return methods;
    }

    private static int bodyLines(CtExecutable<?> executable) {
        if (executable.getBody() == null || !executable.getBody().getPosition().isValidPosition()) {
            return 0;
        }
        return executable.getBody().getPosition().getEndLine()
                - executable.getBody().getPosition().getLine() + 1;
    }

    /**
     * Appels contenus dans le corps. Les CtInvocation dont l'executable est un
     * constructeur (super(...), this(...)) sont ecartes : le sujet ne les compte
     * pas comme des appels de methode.
     */
    private static List<CallFact> callsOf(CtExecutable<?> executable, String callerId) {
        if (executable.getBody() == null) {
            return List.of();
        }
        List<CallFact> calls = new ArrayList<>();
        for (CtInvocation<?> invocation
                : executable.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
            CtExecutableReference<?> target = invocation.getExecutable();
            if (target == null || target.isConstructor()) {
                continue;
            }
            CtTypeReference<?> receiver = receiverOf(invocation, target);
            String declaring = target.getDeclaringType() == null
                    ? "(inconnu)"
                    : target.getDeclaringType().getQualifiedName().replace('$', '.');
            List<String> parameterTypes = target.getParameters().stream()
                    .map(parameter -> parameter.getQualifiedName().replace('$', '.'))
                    .toList();
            boolean resolved = target.getDeclaringType() != null;
            calls.add(new CallFact(callerId,
                    target.getSimpleName(),
                    resolved ? Optional.of(declaring + "#" + target.getSimpleName()
                            + "(" + String.join(",", parameterTypes) + ")") : Optional.empty(),
                    receiver == null ? "(inconnu)" : receiver.getQualifiedName().replace('$', '.'),
                    lineOf(invocation),
                    resolved,
                    resolved && isFromSource(target)));
        }
        return calls;
    }

    /** Type statique du receveur : le type de la cible d'appel, sinon le type declarant. */
    private static CtTypeReference<?> receiverOf(CtInvocation<?> invocation,
            CtExecutableReference<?> target) {
        if (invocation.getTarget() != null && invocation.getTarget().getType() != null) {
            return invocation.getTarget().getType();
        }
        return target.getDeclaringType();
    }

    private static boolean isFromSource(CtExecutableReference<?> target) {
        CtType<?> declaration = target.getDeclaringType().getTypeDeclaration();
        return declaration != null && !declaration.isShadow();
    }

    private static int lineOf(CtElement element) {
        return element.getPosition().isValidPosition() ? element.getPosition().getLine() : 0;
    }

    private static Visibility visibilityOf(CtModifiable member, CtType<?> owner) {
        if (member.hasModifier(ModifierKind.PUBLIC)) {
            return Visibility.PUBLIC;
        }
        if (member.hasModifier(ModifierKind.PROTECTED)) {
            return Visibility.PROTECTED;
        }
        if (member.hasModifier(ModifierKind.PRIVATE)) {
            return Visibility.PRIVATE;
        }
        return owner.isInterface() ? Visibility.PUBLIC : Visibility.PACKAGE;
    }
}
