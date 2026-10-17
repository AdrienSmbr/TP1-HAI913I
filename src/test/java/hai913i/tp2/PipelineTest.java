package hai913i.tp2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PipelineTest {
    @TempDir Path tmp;
    private Facts mini() throws IOException { return new CsvFactsSource(Path.of("examples"), "facts-mini").load(); }
    private Facts canonical() throws IOException { return new CsvFactsSource(Path.of("resources"), null).load(); }
    private WeightedGraph graph(Facts facts) { return new CouplingCalculator().compute(facts).graph(); }
    private int cli(String... args) {
        return Main.run(args, new PrintStream(OutputStream.nullOutputStream()), new PrintStream(OutputStream.nullOutputStream()));
    }
    @Test void microJeuCalculableALaMain() throws Exception {
        Facts f = mini(); var c = new CouplingCalculator().compute(f); var g = c.graph();
        assertEquals(3, f.classes().size()); assertEquals(5, f.methodOwners().size()); assertEquals(4, f.calls().size());
        assertEquals(1, c.intra()); assertEquals(3, c.inter());
        assertEquals(2.0/3, g.weight("demo.A", "demo.B"), 1e-12);
        assertEquals(1.0/3, g.weight("demo.B", "demo.C"), 1e-12);
        assertEquals(0, g.weight("demo.A", "demo.C"));
        var tree = new AgglomerativeClusterer().cluster(g);
        assertEquals(List.of("demo.A", "demo.B"), tree.merges().get(0).members());
        assertEquals(2.0/3, tree.merges().get(0).similarity(), 1e-12);
        assertEquals(1.0/6, tree.root().similarity(), 1e-12);
        var cutter = new ModuleCutter();
        assertTrue(cutter.cut(tree.root(), g, .2).accepted());
        assertEquals(1, cutter.cut(tree.root(), g, .2).modules().size());
        assertFalse(cutter.cut(tree.root(), g, .5).accepted());
        assertEquals(2, cutter.cut(tree.root(), g, .5).modules().size());
    }
    @Test void controlesCanoniquesA0A2() throws Exception {
        Facts f = canonical(); var c = new CouplingCalculator().compute(f); var g = c.graph();
        assertEquals(15, f.classes().size()); assertEquals(59, f.methodOwners().size()); assertEquals(44, f.calls().size());
        assertEquals(7, c.intra()); assertEquals(37, c.inter()); assertEquals(19, g.edges().size());
        assertEquals(1, g.edges().values().stream().mapToDouble(Double::doubleValue).sum(), 1e-12);
        for (String a : g.classes()) for (String b : g.classes()) {
            assertEquals(g.weight(a,b), g.weight(b,a)); assertTrue(g.weight(a,b) >= 0 && g.weight(a,b) <= 1);
        }
        for (String a : g.classes()) assertEquals(0, g.weight(a,a));
        assertEquals(4.0/37, g.weight("library.model.Item", "library.service.LoanService"), 1e-12);
        assertEquals(List.of("library.model.Dvd", "library.service.LoanException"), g.isolated());
        assertEquals(List.of(4.0/37, 4.0/37, 3.0/37, 3.0/37, 3.0/37),
                g.rankedEdges().subList(0,5).stream().map(Map.Entry::getValue).toList());
    }
    @Test void arbreCanoniqueEtPartitionsActives() throws Exception {
        var g = graph(canonical()); var clusterer = new AgglomerativeClusterer(); var tree = clusterer.cluster(g);
        assertEquals(14, tree.merges().size());
        assertEquals(4.0/37, tree.merges().get(0).similarity(), 1e-12);
        assertEquals(3.0/37, tree.merges().get(1).similarity(), 1e-12);
        assertEquals(9.0/148, tree.merges().get(2).similarity(), 1e-12);
        assertEquals(List.of("library.model.Item", "library.service.LoanService"), tree.merges().get(0).members());
        assertEquals(List.of("library.app.LibraryApp", "library.service.Catalog"), tree.merges().get(1).members());
        assertEquals(0, tree.merges().get(12).similarity()); assertEquals(0, tree.merges().get(13).similarity());
        double last = Double.POSITIVE_INFINITY;
        Map<Integer, List<String>> active = new TreeMap<>();
        for (int i=0;i<g.classes().size();i++) active.put(i,List.of(g.classes().get(i)));
        for (ClusterNode n : tree.merges()) {
            assertTrue(n.similarity() <= last + 1e-9); last=n.similarity();
            assertNotNull(active.remove(n.left().id())); assertNotNull(active.remove(n.right().id()));
            active.put(n.id(),n.members());
            assertEquals(g.classes(),active.values().stream().flatMap(List::stream).sorted().toList());
            assertEquals(clusterer.similarity(g,n.left(),n.right()),n.similarity(),1e-12);
        }
        assertEquals(1,active.size()); assertEquals(29, countNodes(tree.root()));
        assertEquals(tree,clusterer.cluster(g));
    }
    private int countNodes(ClusterNode n) { return n.isLeaf()?1:1+countNodes(n.left())+countNodes(n.right()); }
    @Test void coupeCanoniqueEtBranches() throws Exception {
        var g=graph(canonical()); var tree=new AgglomerativeClusterer().cluster(g); var cutter=new ModuleCutter();
        double[] cps={.015,.025,.045}; int[] counts={4,7,10};
        Set<Integer> branches=new HashSet<>(); collect(tree.root(),branches);
        for(int i=0;i<cps.length;i++) {
            var cut=cutter.cut(tree.root(),g,cps[i]); assertEquals(counts[i],cut.modules().size());
            assertEquals(i<2,cut.accepted()); assertEquals(7,cut.maximum());
            assertEquals(g.classes(),cut.modules().stream().flatMap(m->m.node().members().stream()).sorted().toList());
            for(var m:cut.modules()) { assertTrue(branches.contains(m.node().id())); assertTrue(m.node().isLeaf()||m.internalMean()>cps[i]); }
        }
        assertEquals(11,cutter.cut(tree.root(),g,.015).modules().stream().mapToInt(m->m.node().members().size()).max().orElseThrow());
    }
    private void collect(ClusterNode n,Set<Integer> ids) { ids.add(n.id()); if(!n.isLeaf()){collect(n.left(),ids);collect(n.right(),ids);} }
    @Test void seuilStrictEtMoyenneInterneDistincte() throws Exception {
        var g=graph(mini()); var tree=new AgglomerativeClusterer().cluster(g); var cutter=new ModuleCutter();
        assertEquals(1.0/3,g.internalMean(tree.root().members()),1e-12);
        assertNotEquals(tree.root().similarity(),g.internalMean(tree.root().members()));
        assertEquals(2,cutter.cut(tree.root(),g,1.0/3).modules().size());
        assertEquals(3,cutter.cut(tree.root(),g,2.0/3).modules().size());
    }
    @Test void departageAvecToleranceEtIdentifiants() {
        var g=new WeightedGraph(List.of("D","B","A","C"),Map.of(
            new WeightedGraph.Pair("A","C"),.25,
            new WeightedGraph.Pair("B","D"),.25+1e-11,
            new WeightedGraph.Pair("C","D"),.25));
        var first=new AgglomerativeClusterer().cluster(g).merges().get(0);
        assertEquals(0,first.left().id()); assertEquals(2,first.right().id());
        var sameFirst=new WeightedGraph(List.of("A","B","C","D"),Map.of(
            new WeightedGraph.Pair("A","C"),.25,new WeightedGraph.Pair("A","D"),.25));
        assertEquals(2,new AgglomerativeClusterer().cluster(sameFirst).merges().get(0).right().id());
    }
    @Test void moyenneDirecteEtFormulePonderee() throws Exception {
        var g=graph(canonical()); var c=new AgglomerativeClusterer();
        var a=ClusterNode.merge(50,ClusterNode.leaf(0,g.classes().get(0)),ClusterNode.leaf(1,g.classes().get(1)),0);
        var b=ClusterNode.leaf(2,g.classes().get(4)); var d=ClusterNode.leaf(3,g.classes().get(10));
        var merged=ClusterNode.merge(51,a,b,0);
        assertEquals((2*c.similarity(g,a,d)+c.similarity(g,b,d))/3,c.similarity(g,merged,d),1e-12);
    }
    @Test void exportRechargeEtDeterminismeOctetParOctet() throws Exception {
        var g=graph(canonical()); Path csv=tmp.resolve("graph.csv"); g.save(csv); var loaded=WeightedGraph.load(csv);
        assertEquals(g.classes(),loaded.classes()); assertEquals(g.edges(),loaded.edges());
        assertEquals(g.isolated(),loaded.isolated());
        var tree=new AgglomerativeClusterer().cluster(g); var cut=new ModuleCutter().cut(tree.root(),g,.025);
        Path a=tmp.resolve("a"), b=tmp.resolve("b"); Exports.save(a,g,tree,cut,.025); Exports.save(b,loaded,tree,cut,.025);
        try(var paths=Files.list(a)) { for(Path file:paths.toList()) assertEquals(-1,Files.mismatch(file,b.resolve(file.getFileName()))); }
    }
    @Test void aucunAppelEtPetitsSystemes() {
        var f=new Facts(List.of("A","B"),Map.of(),List.of()); var c=new CouplingCalculator().compute(f);
        assertEquals(0,c.inter()); assertTrue(c.graph().edges().isEmpty());
        var t=new AgglomerativeClusterer().cluster(c.graph()); assertEquals(0,t.root().similarity());
        assertEquals(2,new ModuleCutter().cut(t.root(),c.graph(),0).modules().size());
        var one=new WeightedGraph(List.of("A"),Map.of()); var single=new AgglomerativeClusterer().cluster(one);
        assertTrue(single.merges().isEmpty()); assertFalse(new ModuleCutter().cut(single.root(),one,0).accepted());
    }
    @Test void cliCodesEtEntreesInvalides() throws Exception {
        assertEquals(0,cli("--help")); assertEquals(2,cli());
        assertEquals(0,cli("resources","--cp",".025","--out",tmp.resolve("ok").toString()));
        assertEquals(3,cli("resources","--cp",".045","--out",tmp.resolve("rejet").toString()));
        assertTrue(Files.isRegularFile(tmp.resolve("rejet/modules.csv")));
        for(String cp:List.of("abc","NaN","Infinity","-.1","1.1","0,025")) assertEquals(2,cli("resources","--cp",cp));
        assertEquals(2,cli("absent","--cp",".025"));
        assertEquals(2,cli("resources","--cp",".025","--inconnu","x"));
        assertEquals(2,cli("resources","--cp",".025","--cp",".1"));
        assertEquals(2,cli("resources","--cp"));
        assertEquals(2,cli("resources","--graph","x","--cp",".025"));
    }
    @Test void fichiersIncoherentsEtDoublonsRejetes() throws Exception {
        Path dir=tmp.resolve("facts"); Files.createDirectories(dir);
        for(String kind:List.of("classes","methods","calls")) Files.copy(Path.of("examples/facts-mini-"+kind+".csv"),dir.resolve("x-"+kind+".csv"));
        var source=new CsvFactsSource(dir,null); assertEquals(3,source.load().classes().size());
        Path calls=dir.resolve("x-calls.csv"); String original=Files.readString(calls);
        String first=original.lines().skip(1).findFirst().orElseThrow();
        Files.writeString(calls,original+"\n"+first+"\n"); assertThrows(IOException.class,source::load);
        Files.writeString(calls,original.replace("demo.B;", "inconnu;")); assertThrows(IOException.class,source::load);
        Files.writeString(calls,"mauvais\n"); assertThrows(IOException.class,source::load);
        Files.delete(calls); assertThrows(IOException.class,source::load);
        Files.copy(dir.resolve("x-classes.csv"),dir.resolve("y-classes.csv")); assertThrows(IOException.class,source::load);
        assertThrows(IllegalArgumentException.class,()->new Facts(List.of("A","A"),Map.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Facts(List.of("A"),Map.of("m","B"),List.of()));
    }
    @Test void grapheMalformeRejete() throws Exception {
        Path file=tmp.resolve("g.csv");
        for(String data:List.of("node;A;;\nedge;A;B;0.5\n","node;A;;\nnode;B;;\nedge;A;B;NaN\n",
            "node;A;;\nnode;A;;\n","node;A;;\nnode;B;;\nedge;A;B;0.5\nedge;B;A;0.5\n")) {
            Files.writeString(file,"kind;classA;classB;weight\n"+data); assertThrows(IOException.class,()->WeightedGraph.load(file));
        }
    }
}
