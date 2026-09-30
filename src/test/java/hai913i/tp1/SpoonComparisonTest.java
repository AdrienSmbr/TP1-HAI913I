package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.callgraph.CallGraphBuilder;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.spoon.SpoonExtractor;
import hai913i.tp1.spoon.SpoonRawCounts;

class SpoonComparisonTest {

    private static final Path VALIDATION = TestProjects.validation();

    private static ProjectFacts jdt;
    private static ProjectFacts spoon;
    private static SpoonRawCounts raw;

    @BeforeAll
    static void analyse() throws Exception {
        ProjectSources sources = ProjectSources.of(VALIDATION);
        jdt = FactExtractor.extract(JdtParser.parse(sources, List.of()));
        spoon = SpoonExtractor.extract(sources.sourceRoot(), sources.javaFiles());
        raw = SpoonRawCounts.of(sources.sourceRoot());
    }

    @Test
    @DisplayName("Spoon et JDT donnent la meme structure apres conventions du sujet")
    void sameStructure() {
        assertEquals(jdt.types().size(), spoon.types().size());
        assertEquals(jdt.methodCount(), spoon.methodCount());
        assertEquals(jdt.constructorCount(), spoon.constructorCount());
        assertEquals(jdt.fieldCount(), spoon.fieldCount());
        assertEquals(jdt.packages(), spoon.packages());
    }

    @Test
    @DisplayName("Spoon et JDT donnent les memes appels apres conventions du sujet")
    void sameCalls() {
        assertEquals(jdt.callCount(), spoon.callCount());
        assertEquals(jdt.internalCallCount(), spoon.internalCallCount());
        assertEquals(jdt.externalCallCount(), spoon.externalCallCount());
        assertEquals(jdt.unresolvedCallCount(), spoon.unresolvedCallCount());
    }

    @Test
    @DisplayName("le graphe d'appel est identique quel que soit l'extracteur")
    void sameGraph() {
        CallGraph fromJdt = CallGraphBuilder.build(jdt);
        CallGraph fromSpoon = CallGraphBuilder.build(spoon);
        assertEquals(fromJdt.nodeCount(), fromSpoon.nodeCount());
        assertEquals(fromJdt.arcCount(), fromSpoon.arcCount());
        assertEquals(fromJdt.siteCount(), fromSpoon.siteCount());
    }

    @Test
    @DisplayName("les comptages bruts de Spoon different, et on sait pourquoi")
    void rawCountsDiffer() {
        assertEquals(14, raw.topLevelTypes(), "getAllTypes() ignore les types membres");
        assertEquals(50, raw.methods(), "getMethods() exclut les constructeurs");
        assertEquals(13, raw.constructors());
        assertEquals(4, raw.implicitConstructors(), "constructeurs par defaut ajoutes par Spoon");
        assertEquals(24, raw.fields(), "getFields() inclut les constantes d'enumeration");
        assertEquals(3, raw.enumValues());
        assertEquals(6, raw.packages(), "paquetage racine et library comptes en plus");
        assertEquals(107, raw.invocations());
        assertEquals(13, raw.constructorInvocations(), "super(...) et this(...) sont des CtInvocation");
    }
}
