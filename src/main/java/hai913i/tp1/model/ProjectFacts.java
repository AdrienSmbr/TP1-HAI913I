package hai913i.tp1.model;

import java.nio.file.Path;
import java.util.List;

public record ProjectFacts(List<TypeFact> types, List<Path> sourceFiles) {

    public int methodCount() {
        return types.stream().mapToInt(t -> t.methods().size()).sum();
    }

    public int fieldCount() {
        return types.stream().mapToInt(t -> t.fields().size()).sum();
    }

    public int constructorCount() {
        return (int) types.stream()
                .flatMap(t -> t.methods().stream())
                .filter(MethodFact::constructor)
                .count();
    }

    public List<CallFact> calls() {
        return types.stream()
                .flatMap(t -> t.methods().stream())
                .flatMap(m -> m.calls().stream())
                .toList();
    }

    public int callCount() {
        return calls().size();
    }

    public int internalCallCount() {
        return (int) calls().stream().filter(CallFact::internal).count();
    }

    public int externalCallCount() {
        return (int) calls().stream()
                .filter(CallFact::resolved)
                .filter(c -> !c.internal())
                .count();
    }

    public int unresolvedCallCount() {
        return (int) calls().stream().filter(c -> !c.resolved()).count();
    }

    public List<String> packages() {
        return types.stream().map(TypeFact::packageName).distinct().sorted().toList();
    }
}
