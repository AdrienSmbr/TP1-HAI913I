package hai913i.tp2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Régressions de l'audit : chaque cas reproduit un défaut du premier rendu. */
class AuditTest {
    @TempDir Path tmp;
    @Test void exportGraphiqueRemplaceAussiLeResumeEtConserveLaRelation() throws Exception {
        var f=new CsvFactsSource(Path.of("resources"),null).load();
        var g=new CouplingCalculator().compute(f).graph();
        var m=new ExplorerModel(g,.025,AgglomerativeClusterer.Linkage.AVERAGE,"calls","A0 | faits canoniques\n");
        m.export(tmp);assertTrue(Files.readString(tmp.resolve("resultats.txt")).contains("COUPE ACCEPTÉE"));
        m.setCp(.045);m.export(tmp);
        String summary=Files.readString(tmp.resolve("resultats.txt"));
        assertTrue(summary.contains("CP=0.045"));assertTrue(summary.contains("COUPE REJETÉE"));
        assertFalse(summary.contains("COUPE ACCEPTÉE"));
        assertTrue(Files.readString(tmp.resolve("parametres.txt")).contains("relation=calls\n"));
        m.setCp(.025);m.setLinkage(AgglomerativeClusterer.Linkage.SINGLE);m.export(tmp);
        assertTrue(Files.readString(tmp.resolve("resultats.txt")).contains("C1 | Variante"));
        assertTrue(Files.readString(tmp.resolve("parametres.txt")).contains("linkage=single"));
    }
    @Test void heritageSansAppelNeDoitPasAnnoncerUnGrapheNul() throws Exception {
        Files.writeString(tmp.resolve("x-classes.csv"),"class;simpleName;package\ndemo.A;A;demo\ndemo.B;B;demo\n");
        Files.writeString(tmp.resolve("x-methods.csv"),"method;owner\n");
        Files.writeString(tmp.resolve("x-calls.csv"),"caller;callerClass;callee;calleeClass;sites\n");
        Path relations=tmp.resolve("heritage.csv");Files.writeString(relations,"type;target;kind\ndemo.A;demo.B;extends\n");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        int code=Main.run(new String[]{tmp.toString(),"--cp",".5","--inheritance",relations.toString(),"--out",tmp.resolve("sorties").toString()},new PrintStream(out),System.err);
        assertEquals(0,code);assertFalse(out.toString().contains("tous nuls"));
        assertTrue(out.toString().contains("dénominateur = 1"));assertTrue(out.toString().contains("Arêtes : 1"));
    }
    @Test void grapheNonNormaliseEstRefuseEtGrapheSansAreteAccepte() throws Exception {
        Path p=tmp.resolve("g.csv");
        Files.writeString(p,"kind;classA;classB;weight\nnode;A;;\nnode;B;;\nedge;A;B;0.2\n");
        IOException e=assertThrows(IOException.class,()->WeightedGraph.load(p));assertTrue(e.getMessage().contains("non normalisé"));
        Files.writeString(p,"kind;classA;classB;weight\nnode;A;;\nnode;B;;\n");
        assertTrue(WeightedGraph.load(p).edges().isEmpty());
    }
    @Test void groupesInvalidesEtClassesInconnuesSontRejetes() {
        var g=new WeightedGraph(List.of("A","B"),Map.of(new WeightedGraph.Pair("A","B"),1.0));
        assertThrows(IllegalArgumentException.class,()->g.weight("A","inconnu"));
        assertThrows(IllegalArgumentException.class,()->g.internalMean(List.of("A","A")));
        assertThrows(IllegalArgumentException.class,()->g.internalMean(List.of("inconnu")));
        assertThrows(IllegalArgumentException.class,()->new ModuleCutter().cut(ClusterNode.leaf(0,"A"),g,.1));
        assertThrows(IllegalArgumentException.class,()->new ClusterNode(-1,null,null,0,List.of("A")));
        assertThrows(IllegalArgumentException.class,()->new ClusterNode(0,null,null,Double.NaN,List.of("A")));
    }
    @Test void nomsIncompatiblesAvecLesExportsSontRefuses() {
        for(String name:List.of("A|B","A\"B","A;B","A\nB"))
            assertThrows(IllegalArgumentException.class,()->new Facts(List.of(name),Map.of(),List.of()));
    }
    @Test void ordreDesFichiersSansEffetSurLesResultats() throws Exception {
        for(String kind:List.of("classes","methods","calls")) {
            var lines=Files.readAllLines(Path.of("resources/facts-validation-"+kind+".csv"));
            var data=new ArrayList<>(lines.subList(1,lines.size()));Collections.shuffle(data,new Random(41));
            Files.writeString(tmp.resolve("melange-"+kind+".csv"),lines.get(0)+"\n"+String.join("\n",data)+"\n");
        }
        var calc=new CouplingCalculator();var a=calc.compute(new CsvFactsSource(Path.of("resources"),null).load()).graph();
        var b=calc.compute(new CsvFactsSource(tmp,null).load()).graph();
        assertEquals(a.classes(),b.classes());assertEquals(a.edges(),b.edges());
        assertEquals(new AgglomerativeClusterer().cluster(a),new AgglomerativeClusterer().cluster(b));
    }
}
