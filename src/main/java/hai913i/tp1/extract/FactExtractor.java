package hai913i.tp1.extract;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.parse.JdtParser.ParsedFile;

public final class FactExtractor {

    private FactExtractor() {
    }

    public static ProjectFacts extract(List<ParsedFile> files) {
        List<TypeFact> types = new ArrayList<>();
        List<Path> sourceFiles = new ArrayList<>();

        for (ParsedFile file : files) {
            sourceFiles.add(file.path());
            StructureVisitor visitor = new StructureVisitor(file.unit());
            file.unit().accept(visitor);
            types.addAll(visitor.types());
        }

        types.sort(Comparator.comparing(TypeFact::qualifiedName));
        sourceFiles.sort(Comparator.naturalOrder());
        return new ProjectFacts(List.copyOf(types), List.copyOf(sourceFiles));
    }
}
