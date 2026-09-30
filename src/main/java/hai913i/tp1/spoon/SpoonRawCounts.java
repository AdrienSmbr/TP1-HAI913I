package hai913i.tp1.spoon;

import java.nio.file.Path;

import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public record SpoonRawCounts(int allTypes,
                             int topLevelTypes,
                             int methods,
                             int constructors,
                             int implicitConstructors,
                             int fields,
                             int enumValues,
                             int packages,
                             int invocations,
                             int constructorInvocations) {

    public static SpoonRawCounts of(Path sourceRoot) {
        CtModel model = SpoonExtractor.buildModel(sourceRoot);

        int allTypes = model.getElements(new TypeFilter<>(CtType.class)).size();
        int topLevel = model.getAllTypes().size();
        int methods = model.getElements(new TypeFilter<>(CtMethod.class)).size();

        var constructors = model.getElements(new TypeFilter<>(CtConstructor.class));
        int implicitConstructors = (int) constructors.stream()
                .filter(CtConstructor::isImplicit)
                .count();

        var fields = model.getElements(new TypeFilter<>(CtField.class));
        int enumValues = (int) fields.stream()
                .filter(field -> field instanceof spoon.reflect.declaration.CtEnumValue)
                .count();

        var invocations = model.getElements(new TypeFilter<>(CtInvocation.class));
        int constructorInvocations = (int) invocations.stream()
                .filter(invocation -> invocation.getExecutable() != null
                        && invocation.getExecutable().isConstructor())
                .count();

        int anonymous = (int) model.getElements(new TypeFilter<>(CtClass.class)).stream()
                .filter(CtType::isAnonymous)
                .count();

        return new SpoonRawCounts(allTypes - anonymous, topLevel,
                methods, constructors.size(), implicitConstructors,
                fields.size(), enumValues,
                model.getAllPackages().size(),
                invocations.size(), constructorInvocations);
    }
}
