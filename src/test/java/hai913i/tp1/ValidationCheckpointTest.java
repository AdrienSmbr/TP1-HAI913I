package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.callgraph.CallGraphBuilder;
import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.metrics.MetricsService;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;

/**
 * Points de controle A0 a B3 sur le projet de validation fourni.
 * Un echec ici signale une regression sur des chiffres connus.
 */
class ValidationCheckpointTest {

    private static final Path VALIDATION = TestProjects.validation();

    private static ProjectFacts facts;
    private static int compilationUnits;

    @BeforeAll
    static void analyse() throws Exception {
        ProjectSources sources = ProjectSources.of(VALIDATION);
        var files = JdtParser.parse(sources, List.of());
        compilationUnits = files.size();
        facts = FactExtractor.extract(files);
    }

    @Test
    @DisplayName("A0 : 14 unites de compilation")
    void a0() {
        assertEquals(14, compilationUnits);
    }

    @Test
    @DisplayName("A2 : 15 types, 59 methodes dont 9 constructeurs, 21 attributs, 4 paquetages")
    void a2Totals() {
        assertEquals(15, facts.types().size());
        assertEquals(59, facts.methodCount());
        assertEquals(9, facts.constructorCount());
        assertEquals(21, facts.fieldCount());
        assertEquals(4, facts.packages().size());
    }

    @Test
    @DisplayName("A2 : la classe membre Loan n'est pas comptee dans LoanService")
    void a2NestedType() {
        TypeFact service = type("library.service.LoanService");
        TypeFact loan = type("library.service.LoanService.Loan");
        assertEquals(6, service.methods().size());
        assertEquals(5, service.fields().size());
        assertEquals(4, loan.methods().size());
        assertEquals(3, loan.fields().size());
    }

    @Test
    @DisplayName("A2 : la chaine des superclasses vient des bindings, sans java.lang.Object")
    void a2Superclasses() {
        assertEquals(List.of("java.lang.Exception", "java.lang.Throwable"),
                type("library.service.LoanException").superclasses());
    }

    @Test
    @DisplayName("A3 : 94 appels, 46 internes, 48 externes, 0 non resolu")
    void a3Totals() {
        assertEquals(94, facts.callCount());
        assertEquals(46, facts.internalCallCount());
        assertEquals(48, facts.externalCallCount());
        assertEquals(0, facts.unresolvedCallCount());
    }

    @Test
    @DisplayName("A3 : le receveur est le type statique, Loanable et non Item")
    void a3StaticReceiver() {
        var call = facts.calls().stream()
                .filter(c -> c.calledName().equals("checkOut"))
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Loanable", call.staticReceiverType());
        assertEquals("library.model.Loanable#checkOut(library.model.Member)",
                call.targetId().orElseThrow());
    }

    @Test
    @DisplayName("A3 : super.describe() vise la superclasse directe")
    void a3SuperCall() {
        var call = facts.calls().stream()
                .filter(c -> c.callerId().equals("library.model.Book#describe()"))
                .filter(c -> c.calledName().equals("describe"))
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Item", call.staticReceiverType());
    }

    @Test
    @DisplayName("A3 : un appel dans une lambda est rattache a la methode englobante")
    void a3LambdaCall() {
        assertTrue(facts.calls().stream()
                .filter(c -> c.callerId().startsWith("library.service.Catalog#available"))
                .anyMatch(c -> c.calledName().equals("isAvailable")));
    }

    @Test
    @DisplayName("B2 : questions 1 a 7 et 13")
    void b2Basics() throws Exception {
        MetricsReport report = MetricsService.compute(facts, 5);
        assertEquals(15, report.classCount());
        assertEquals(493, report.applicationLines());
        assertEquals(59, report.methodCount());
        assertEquals(4, report.packageCount());
        assertEquals(3.93, report.methodsPerClass(), 0.005);
        assertEquals(4.64, report.linesPerMethod(), 0.005);
        assertEquals(1.40, report.fieldsPerClass(), 0.005);
        assertEquals(53, report.methodsWithBody());
        assertEquals(246, report.bodyLinesTotal());
        assertEquals(5, report.maxParameters());
        assertEquals(List.of("library.util.Statistics#report(java.lang.String,int,int,double,int)"),
                report.methodsWithMaxParameters().stream().map(MethodFact::id).toList());
    }

    @Test
    @DisplayName("B2 : questions 8 a 12, classements et seuil X")
    void b2Rankings() throws Exception {
        MetricsReport report = MetricsService.compute(facts, 5);
        assertEquals(List.of("library.model.Item", "library.model.Member",
                "library.service.LoanService"), names(report.topByMethods()));
        assertEquals(List.of("library.model.Item", "library.service.LoanService"),
                names(report.topByFields()));
        assertEquals(List.of("library.model.Item", "library.service.LoanService"),
                names(report.topByBoth()));
        assertEquals(3, report.overThreshold().size());
        assertEquals(6, MetricsService.compute(facts, 4).overThreshold().size());

        Map<String, List<MethodFact>> perClass = report.topMethodsPerClass();
        assertEquals(List.of("checkOut"), methodNames(perClass.get("library.model.Item")));
        assertEquals(List.of("borrow"), methodNames(perClass.get("library.service.LoanService")));
        assertEquals(6, perClass.get("library.model.Member").size());
        assertTrue(perClass.get("library.model.Loanable").isEmpty(),
                "Loanable : toutes les methodes sont a 0 ligne, aucune retenue");
    }

    @Test
    @DisplayName("B3 : 59 noeuds, 44 arcs, 46 sites, 48 externes, 0 non resolu")
    void b3Totals() {
        CallGraph graph = CallGraphBuilder.build(facts);
        assertEquals(59, graph.nodeCount());
        assertEquals(44, graph.arcCount());
        assertEquals(46, graph.siteCount());
        assertEquals(48, graph.externalCalls());
        assertEquals(0, graph.unresolvedCalls());
    }

    @Test
    @DisplayName("B3 : poids, recursion, et interrogation dans les deux sens")
    void b3Queries() {
        CallGraph graph = CallGraphBuilder.build(facts);

        String addTwo = "library.service.Catalog#add(library.model.Item,library.model.Item)";
        String addOne = "library.service.Catalog#add(library.model.Item)";
        assertEquals(2, graph.callees(addTwo).get(addOne),
                "deux sites d'appel donnent un seul arc de poids 2");

        String repeat = "library.util.TextUtils#repeat(java.lang.String,int)";
        assertTrue(graph.callees(repeat).containsKey(repeat), "recursion : arc f -> f");

        assertEquals(List.of("library.service.LoanService#borrow(library.model.Member,java.lang.String,int)"),
                List.copyOf(graph.callers("library.model.Loanable#checkOut(library.model.Member)").keySet()));
        assertTrue(graph.callers("library.model.Item#checkOut(library.model.Member)").isEmpty(),
                "pas d'expansion des appels virtuels : la methode heritee n'a aucune appelante");
    }

    @Test
    @DisplayName("B1 : deux extractions successives donnent le meme modele")
    void b1Deterministic() throws Exception {
        ProjectSources sources = ProjectSources.of(VALIDATION);
        ProjectFacts again = FactExtractor.extract(JdtParser.parse(sources, List.of()));
        assertEquals(names(facts.types()), names(again.types()));
        assertEquals(facts.callCount(), again.callCount());
        assertEquals(CallGraphBuilder.build(facts).outgoing(),
                CallGraphBuilder.build(again).outgoing());
    }

    private static TypeFact type(String qualifiedName) {
        return facts.types().stream()
                .filter(t -> t.qualifiedName().equals(qualifiedName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("type absent : " + qualifiedName));
    }

    private static List<String> names(List<TypeFact> types) {
        return types.stream().map(TypeFact::qualifiedName).toList();
    }

    private static List<String> methodNames(List<MethodFact> methods) {
        return methods.stream().map(MethodFact::name).toList();
    }
}
