package hai913i.tp1;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.extract.FactExtractor;
import org.eclipse.jdt.core.compiler.IProblem;

import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.JdtParser.ParsedFile;
import hai913i.tp1.parse.ProjectSources;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage : java -jar target/hai913i-tp1-analyzer.jar DOSSIER_DU_PROJET");
            System.exit(2);
        }

        Path projectDirectory = Path.of(args[0]);

        ProjectSources sources = null;
        try {
            sources = ProjectSources.of(projectDirectory);
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur : " + e.getMessage());
            System.exit(3);
        } catch (IOException e) {
            System.err.println("Erreur de lecture : " + e.getMessage());
            System.exit(3);
        }

        if (sources.isEmpty()) {
            System.err.println("Aucun fichier .java sous " + sources.sourceRoot());
            System.exit(4);
        }

        List<ParsedFile> files = JdtParser.parse(sources, List.of());

        int errors = 0;
        for (ParsedFile file : files) {
            for (IProblem problem : file.unit().getProblems()) {
                if (problem.isError()) {
                    errors++;
                    System.err.println(file.path().getFileName()
                            + ":" + problem.getSourceLineNumber()
                            + " " + problem.getMessage());
                }
            }
        }

        System.out.println("Racine des sources     : " + sources.sourceRoot());
        System.out.println("Unites de compilation  : " + files.size());
        System.out.println("Erreurs de compilation : " + errors);

        ProjectFacts facts = FactExtractor.extract(files);

        System.out.println();
        System.out.println("Types      : " + facts.types().size());
        System.out.println("Methodes   : " + facts.methodCount()
                + " (dont " + facts.constructorCount() + " constructeurs)");
        System.out.println("Attributs  : " + facts.fieldCount());
        System.out.println("Paquetages : " + facts.packages().size() + " " + facts.packages());
        System.out.println();

        for (TypeFact type : facts.types()) {
            System.out.println(type.kind() + " " + type.qualifiedName()
                    + "  [" + type.packageName() + "]");
            if (!type.superclasses().isEmpty()) {
                System.out.println("    superclasses : " + type.superclasses());
            }
            if (!type.interfaces().isEmpty()) {
                System.out.println("    interfaces   : " + type.interfaces());
            }
            for (FieldFact field : type.fields()) {
                System.out.println("    attribut " + field.visibility()
                        + " " + field.declaredType() + " " + field.name());
            }
            for (MethodFact method : type.methods()) {
                System.out.println("    methode  " + method.visibility()
                        + " " + method.name() + "/" + method.parameterCount()
                        + (method.constructor() ? " (constructeur)" : "")
                        + " corps=" + method.bodyLoc() + "l");
            }
        }
    }
}
