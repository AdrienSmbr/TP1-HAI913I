package hai913i.tp2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BonusTest {
    @TempDir Path tmp;
    private Facts canonical() throws IOException { return new CsvFactsSource(Path.of("resources"),null).load(); }
    @Test void lienSimpleSurMicroJeuEtPremiereDivergenceCanonique() throws Exception {
        var mini=new CouplingCalculator().compute(new CsvFactsSource(Path.of("examples"),null).load()).graph();
        var single=new AgglomerativeClusterer(AgglomerativeClusterer.Linkage.SINGLE);
        assertEquals(1.0/3,single.cluster(mini).root().similarity(),1e-12);
        assertEquals(1.0/6,new AgglomerativeClusterer().cluster(mini).root().similarity(),1e-12);
        var g=new CouplingCalculator().compute(canonical()).graph();
        var a=new AgglomerativeClusterer().cluster(g);var s=single.cluster(g);
        assertEquals(a.merges().get(0),s.merges().get(0));
        assertEquals(11,s.merges().get(1).left().id());assertEquals(15,s.merges().get(1).right().id());
        assertEquals(4.0/37,s.merges().get(1).similarity(),1e-12);
        assertEquals(0,a.merges().get(1).left().id());assertEquals(7,a.merges().get(1).right().id());
        assertEquals(14,s.merges().size());assertEquals(s,single.cluster(g));
        double last=1;
        for(var n:s.merges()){assertTrue(n.similarity()<=last+1e-9);last=n.similarity();}
    }
    @Test void enrichissementNormaliseEtIsoles() throws Exception {
        var f=canonical();var e=new DependencyEnricher();
        var result=e.enrich(f,e.load(Path.of("resources/facts-validation-inheritance.csv"),f.classes()));
        assertEquals(37,result.callArcs());assertEquals(4,result.structuralRelations());assertEquals(3,result.externalRelations());
        assertEquals(41,result.denominator());var g=result.graph();
        assertEquals(22,g.edges().size());assertEquals(1,g.edges().values().stream().mapToDouble(Double::doubleValue).sum(),1e-12);
        assertEquals(2.0/41,g.weight("library.model.Book","library.model.Item"),1e-12);
        assertEquals(1.0/41,g.weight("library.model.Dvd","library.model.Item"),1e-12);
        assertEquals(List.of("library.service.LoanException"),g.isolated());
        var t=new AgglomerativeClusterer().cluster(g);var c=new ModuleCutter();
        assertEquals(8,c.cut(t.root(),g,.025).modules().size());assertFalse(c.cut(t.root(),g,.025).accepted());
        assertEquals(7,c.cut(t.root(),g,.020).modules().size());assertTrue(c.cut(t.root(),g,.020).accepted());
    }
    @Test void relationsInvalidesEtCasSansAppels() throws Exception {
        var e=new DependencyEnricher();var f=canonical();Path csv=tmp.resolve("relations.csv");
        for(String row:List.of("absent;library.model.Item;extends","library.model.Item;library.model.Item;extends",
                "library.model.Item;library.model.Book;unknown","library.model.Item;;extends")) {
            Files.writeString(csv,"type;target;kind\n"+row+"\n");assertThrows(IOException.class,()->e.load(csv,f.classes()));
        }
        Files.writeString(csv,"type;target;kind\nlibrary.model.Dvd;library.model.Item;extends\nlibrary.model.Dvd;library.model.Item;extends\n");
        assertThrows(IOException.class,()->e.load(csv,f.classes()));
        var noCalls=new Facts(List.of("A","B"),Map.of(),List.of());
        assertEquals(1,e.enrich(noCalls,List.of(new DependencyEnricher.Relation("A","B","extends"))).denominator());
        assertTrue(e.enrich(noCalls,List.of()).graph().edges().isEmpty());
    }
    @Test void interactionsDuModeleGraphiqueReutilisentLeCalcul() throws Exception {
        var graph=new CouplingCalculator().compute(canonical()).graph();
        var m=new ExplorerModel(graph,.025,AgglomerativeClusterer.Linkage.AVERAGE);
        assertEquals(7,m.cut().modules().size());assertTrue(m.cut().accepted());
        assertEquals(List.of("library.model.Dvd"),m.search("DVD"));
        assertEquals(15,m.search("").size());assertTrue(m.search("absent").isEmpty());
        m.setCp(.045);assertEquals(10,m.cut().modules().size());assertFalse(m.cut().accepted());
        m.setCp(.025);m.setLinkage(AgglomerativeClusterer.Linkage.SINGLE);
        assertEquals(new AgglomerativeClusterer(AgglomerativeClusterer.Linkage.SINGLE).cluster(graph),m.tree());
        assertEquals(graph.classes(),m.assignment().keySet().stream().toList());
        assertThrows(IllegalArgumentException.class,()->m.setCp(Double.NaN));assertEquals(.025,m.cp());
    }
    @Test void secondProjetExtraitEtGenericite() throws Exception {
        var f=new CsvFactsSource(Path.of("datasets/analyseur-tp1"),null).load();
        assertEquals(30,f.classes().size());assertEquals(199,f.methodOwners().size());assertEquals(267,f.calls().size());
        var result=new CouplingCalculator().compute(f);assertEquals(77,result.intra());assertEquals(190,result.inter());
        var g=result.graph();assertEquals(52,g.edges().size());var t=new AgglomerativeClusterer().cluster(g);
        assertEquals(29,t.merges().size());assertEquals(30,t.root().members().size());
        assertTrue(f.methodOwners().containsKey("hai913i.tp1.model.TypeFact#qualifiedName()"));
        assertTrue(f.calls().stream().anyMatch(a -> a.callee().equals("hai913i.tp1.model.TypeFact#qualifiedName()")));
        assertEquals(1,g.edges().values().stream().mapToDouble(Double::doubleValue).sum(),1e-12);
        var c=new ModuleCutter();assertEquals(15,c.cut(t.root(),g,.01).modules().size());assertTrue(c.cut(t.root(),g,.01).accepted());
        assertEquals(23,c.cut(t.root(),g,.025).modules().size());assertFalse(c.cut(t.root(),g,.025).accepted());
    }
    @Test void optionsBonusEtIncompatibilites() throws Exception {
        PrintStream sink=new PrintStream(OutputStream.nullOutputStream());
        assertEquals(0,Main.run(new String[]{"resources","--cp",".025","--linkage","single","--out",tmp.resolve("single").toString()},sink,sink));
        assertEquals(3,Main.run(new String[]{"resources","--cp",".025","--inheritance","resources/facts-validation-inheritance.csv","--out",tmp.resolve("enriched").toString()},sink,sink));
        assertEquals(2,Main.run(new String[]{"resources","--cp",".025","--linkage","unknown"},sink,sink));
        assertEquals(2,Main.run(new String[]{"--graph","x.csv","--cp",".025","--inheritance","x.csv"},sink,sink));
    }
}
